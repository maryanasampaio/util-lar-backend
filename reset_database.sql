-- Script para resetar o usuário admin
-- Execute este script no DBeaver para recriar o usuário admin

DELETE FROM tb_usuario WHERE username = 'admin';
