-- Migration: Rename token column to refresh_token
-- Author: Maryana Sampaio
-- Date: 2025-12-06
-- Description: Rename token to refresh_token for JWT implementation

ALTER TABLE tb_usuario CHANGE COLUMN token refresh_token VARCHAR(500);
