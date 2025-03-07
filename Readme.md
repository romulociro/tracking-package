# Tracking Application

Este projeto é uma aplicação de rastreamento de pacotes. A aplicação fornece APIs RESTful para criação, atualização, cancelamento e consulta de pacotes, além de processar eventos de rastreamento de forma assíncrona. Segue as principais decisões de design, estratégias de escalabilidade, otimizações e instruções para execução do ambiente utilizando Docker Compose.

---

## Índice

- [Introdução](#introdução)
- [Funcionalidades](#funcionalidades)
- [Decisões de Design](#decisões-de-design)
    - [Modelagem do Banco de Dados](#modelagem-do-banco-de-dados)
    - [Estratégias de Escalabilidade e Otimização](#estratégias-de-escalabilidade-e-otimização)
    - [Gestão de Threads e Chamadas Assíncronas](#gestão-de-threads-e-chamadas-assíncronas)
    - [Retry e Tolerância a Falhas](#retry-e-tolerância-a-falhas)
- [Configuração do Ambiente com Docker Compose](#configuração-do-ambiente-com-docker-compose)
- [Testes](#testes)
- [Melhorias Futuras](#melhorias-futuras)
- [Instruções de Uso](#instruções-de-uso)

---

## Introdução

A Tracking Application foi desenvolvida com foco em alta escalabilidade e performance. O sistema gerencia o rastreamento de pacotes em um ambiente de logística, permitindo o processamento assíncrono de eventos e integração com serviços externos para enriquecimento dos dados.

---

## Funcionalidades

- **Cadastro e Gerenciamento de Pacotes:**  
  Criação, atualização, cancelamento e consulta de pacotes.

- **Processamento Assíncrono de Eventos:**  
  O endpoint `/api/tracking-events` processa eventos de rastreamento de forma assíncrona, integrando dados com chamadas a APIs externas (ex.: Nager.date e Dog API).

- **Monitoramento e Métricas:**  
  Integração com o Spring Boot Actuator e Micrometer para monitoramento do estado da aplicação e do pool de conexões.

---

## Decisões de Design

### Modelagem do Banco de Dados

- **Estrutura das Tabelas:**
    - **Tabela `packages`:** Armazena informações do pacote, como descrição, remetente, destinatário, status, datas de criação, atualização e entrega, e campos para feriado e fun fact.
    - **Tabela `tracking_events`:** Registra os eventos de rastreamento, com informações de localização, descrição, data/hora e uma chave estrangeira que referencia a tabela de pacotes.

- **Índices e Integridade:**  
  Índices foram criados nas colunas de consulta (por exemplo, `estimated_delivery_date`) e as constraints garantem a integridade referencial (ex.: ON DELETE CASCADE).

- **Particionamento e Expurgo:**  
  Estratégia de particionamento por data (ex.: por ano) pode ser aplicada para gerenciar milhões de registros. Além disso, jobs agendados (usando `@Scheduled` ou Spring Batch) podem ser implementados para expurgar registros antigos.

### Estratégias de Escalabilidade e Otimização

- **Pool de Conexões:**  
  Utilizamos o HikariCP com configurações otimizadas para alta concorrência:
  ```properties
  spring.datasource.hikari.maximum-pool-size=50
  spring.datasource.hikari.connection-timeout=30000
  spring.datasource.hikari.idle-timeout=60000
  spring.datasource.hikari.max-lifetime=1800000
  ```

- **Monitoramento:**  
  O Spring Boot Actuator fornece métricas importantes, como número de conexões ativas, ociosas e throughput do pool.

### Gestão de Threads e Chamadas Assíncronas

- **Processamento Assíncrono:**  
  Métodos anotados com `@Async` são executados em threads separadas, melhorando a responsividade. A configuração do executor é definida na classe `AsyncConfig`:
  ```java
  @Configuration
  @EnableAsync
  public class AsyncConfig {
      @Bean(name = "taskExecutor")
      public Executor taskExecutor() {
          ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
          executor.setCorePoolSize(20);
          executor.setMaxPoolSize(40);
          executor.setQueueCapacity(100);
          executor.setThreadNamePrefix("AsyncThread-");
          executor.initialize();
          return executor;
      }
  }
  ```

### Retry e Tolerância a Falhas

- **Spring Retry:**  
  Utilizamos o Spring Retry para reexecutar operações que podem falhar temporariamente (por exemplo, em caso de deadlock). Caso todas as tentativas falhem, um método de recuperação (`@Recover`) é acionado para tratar a exceção.
  ```java
  @Retryable(
      value = { MySQLTransactionRollbackException.class, DeadlockLoserDataAccessException.class },
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)
  )
  @Async
  @Transactional
  public void processTrackingEvent(TrackingEventRequest request) {
      // Lógica de processamento...
  }

  @Recover
  public void recover(Exception ex, TrackingEventRequest request) {
      logger.error("Failed to process tracking event for package {} after retries: {}", request.packageId(), ex.getMessage());
      // Lógica de fallback ou log adicional
  }
  ```

---

## Configuração do Ambiente com Docker Compose

Para facilitar a avaliação e a execução local, o ambiente pode ser levantado utilizando Docker Compose. O arquivo `docker-compose.yml` inclui serviços para o MySQL e para a aplicação.

### Exemplo de `docker-compose.yml`

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: tracking-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: trackingdb
      MYSQL_USER: trackinguser
      MYSQL_PASSWORD: trackingpass
    ports:
      - "3306:3306"
    volumes:
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h localhost -p$MYSQL_ROOT_PASSWORD"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: .
    container_name: tracking-app
    restart: on-failure
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/trackingdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: trackinguser
      SPRING_DATASOURCE_PASSWORD: trackingpass
      SPRING_DATASOURCE_DRIVER-CLASS-NAME: com.mysql.cj.jdbc.Driver
      SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.MySQLDialect
      SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: 50
      SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT: 30000
      SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT: 60000
      SPRING_DATASOURCE_HIKARI_MAX_LIFETIME: 1800000
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy
```

### Instruções

1. **Subir os Containers:**
   ```bash
   docker-compose up -d
   ```
2. **Acessar a Aplicação:**
    - A aplicação ficará disponível em `http://localhost:8080`.
    - Verifique a saúde com `http://localhost:8080/actuator/health`.
3. **Popular o Banco:**
   O arquivo `init.sql` será executado automaticamente para criar e popular o banco de dados.

---

## Testes

### Testes Unitários e de Integração

- **Unitários:**  
  Execute com:
  ```bash
  ./gradlew clean test
  ```

### Teste de Carga

- **JMeter:**
![evidencia docker.JPG](assets%2Fevidencia%20docker.JPG)
![evidencia1.JPG](assets%2Fevidencia1.JPG)
![evidencia2.JPG](assets%2Fevidencia2.JPG)
---

## Melhorias Futuras

- **Sharding:** Distribuição horizontal dos dados para escalabilidade extrema.
- **Cache:** Integração com Redis para reduzir a carga do banco em operações de leitura frequentes.
- **Monitoramento Avançado:** Integração com Prometheus e Grafana para dashboards de métricas em tempo real.
- **Otimizações de Transação:** Revisão dos níveis de isolamento e melhoria na gestão de transações para reduzir deadlocks.

---

## Instruções de Uso

1. **Preparação:**  
   Certifique-se de ter Docker e Docker Compose instalados.
2. **Subir o Ambiente:**  
   Execute:
   ```bash
   docker-compose up -d
   ```
3. **Acessar a Aplicação:**  
   Acesse `http://localhost:8080` e os endpoints do Actuator, como `/actuator/health`.
4. **Testes:**  
   Utilize ferramentas como Postman, cURL e JMeter para testar os endpoints.