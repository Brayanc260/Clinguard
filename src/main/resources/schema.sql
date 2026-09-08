-- =============================================
-- Base de datos: Sistema Clínico de Unidades
-- =============================================

CREATE DATABASE IF NOT EXISTS clinica_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE clinica_db;

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre   VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol      ENUM('ADMIN','MEDICO','ENFERMERO') NOT NULL
);

-- Tabla de historial de conversiones
CREATE TABLE IF NOT EXISTS registro_conversiones (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_nombre  VARCHAR(50)  NOT NULL,
    tipo_conversion VARCHAR(100) NOT NULL,
    valor_entrada   DOUBLE       NOT NULL,
    valor_salida    DOUBLE       NOT NULL,
    fecha_hora      DATETIME     NOT NULL,
    CONSTRAINT fk_usuario FOREIGN KEY (usuario_nombre)
        REFERENCES usuarios(nombre) ON DELETE CASCADE
);

-- Usuarios por defecto (contraseñas encriptadas con BCrypt)
-- admin123 / medico123 / enfermero123
INSERT IGNORE INTO usuarios (nombre, password, rol) VALUES
  ('admin',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN'),
  ('medico',    '$2a$10$8K1p/a0dR1xqM5mMxmZ6meVJGrpGv0yJPbDRQHdWgXJF1ZdJ0VKXW', 'MEDICO'),
  ('enfermero', '$2a$10$oI2GCRqnNZoZ5P0u.UzHvOCEp0.gR6oa4oy0fCvDPNiCAaGZGHlv2', 'ENFERMERO');
