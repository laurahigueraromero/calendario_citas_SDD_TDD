-- Usuarios de la aplicación e identidades de proveedor OAuth asociadas.
-- PRD §2.1. Soporta login con GitHub y con Google sobre la misma cuenta.

CREATE TABLE usuario (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(320) NOT NULL,
    nombre     VARCHAR(200) NOT NULL,
    rol        VARCHAR(20)  NOT NULL,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE identidad_oauth (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id    BIGINT       NOT NULL,
    proveedor     VARCHAR(20)  NOT NULL,
    proveedor_id  VARCHAR(255) NOT NULL,
    handle        VARCHAR(320),
    creado_en     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_identidad_proveedor UNIQUE (proveedor, proveedor_id),
    CONSTRAINT fk_identidad_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE = InnoDB;

CREATE INDEX idx_identidad_usuario ON identidad_oauth (usuario_id);
