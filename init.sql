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
) ENGINE=InnoDB
PARTITION BY RANGE (TO_DAYS(estimated_delivery_date)) (
    PARTITION p2025_01 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p2025_02 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    PARTITION p2025_03 VALUES LESS THAN (TO_DAYS('2025-04-01')),
    PARTITION p2025_04 VALUES LESS THAN (TO_DAYS('2025-05-01')),
    PARTITION p2025_05 VALUES LESS THAN (TO_DAYS('2025-06-01')),
    PARTITION p2025_06 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION p2025_07 VALUES LESS THAN (TO_DAYS('2025-08-01')),
    PARTITION p2025_08 VALUES LESS THAN (TO_DAYS('2025-09-01')),
    PARTITION p2025_09 VALUES LESS THAN (TO_DAYS('2025-10-01')),
    PARTITION p2025_10 VALUES LESS THAN (TO_DAYS('2025-11-01')),
    PARTITION p2025_11 VALUES LESS THAN (TO_DAYS('2025-12-01')),
    PARTITION p2025_12 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

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