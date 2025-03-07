-- init.sql
CREATE DATABASE IF NOT EXISTS trackingdb
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE trackingdb;

CREATE TABLE IF NOT EXISTS packages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    sender VARCHAR(100) NOT NULL,
    recipient VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    is_holliday BOOLEAN,
    fun_fact TEXT,
    estimated_delivery_date DATE,
    INDEX idx_estimated_delivery_date (estimated_delivery_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tracking_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    location VARCHAR(255) NOT NULL,
    description TEXT,
    date_time TIMESTAMP NOT NULL,
    package_id BIGINT,
    CONSTRAINT fk_package FOREIGN KEY (package_id) REFERENCES packages(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_package_id (package_id),
    INDEX idx_date_time (date_time)
) ENGINE=InnoDB;

-- Inserindo dados iniciais na tabela packages
INSERT INTO packages (description, sender, recipient, status, created_at, updated_at, estimated_delivery_date)
VALUES ('Pacote de Teste', 'Loja ABC', 'Joao Silva', 'CREATED', NOW(), NOW(), '2025-10-24');