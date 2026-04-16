-- Migration: Create User Table
-- Author: Maryana Sampaio
-- Date: 2025-12-06
-- Description: Create users table for authentication with token storage

CREATE TABLE tb_usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    token VARCHAR(500),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
