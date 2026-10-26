-- AutoSmart: esquema de base de datos (Sprint 1)

CREATE DATABASE IF NOT EXISTS autosmart_db;
USE autosmart_db;

-- ---------- Roles y usuarios ----------

CREATE TABLE IF NOT EXISTS roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);

INSERT IGNORE INTO roles (nombre) VALUES ('CLIENTE'), ('MECANICO'), ('ADMIN');

CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    id_rol INT,
    telefono VARCHAR(15),
    activo TINYINT(1) NOT NULL DEFAULT 1,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
);

-- ---------- Vehiculos (T06.1, T08.1, T09.1) ----------

CREATE TABLE IF NOT EXISTS vehiculos (
    id_vehiculo BIGINT AUTO_INCREMENT PRIMARY KEY,
    placa VARCHAR(15) UNIQUE NOT NULL,
    marca VARCHAR(50) NOT NULL,
    modelo VARCHAR(50) NOT NULL,
    anio INT NOT NULL,
    kilometraje INT DEFAULT 0,
    id_cliente INT,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    FOREIGN KEY (id_cliente) REFERENCES usuarios(id_usuario)
);


-- ---------- Perfil profesional del mecanico (T13.1) ----------

CREATE TABLE IF NOT EXISTS especialidades (
    id_especialidad INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(60) UNIQUE NOT NULL
);


-- ---------- Catalogo de servicios (T18.1, T19.1, T10.1) ----------

CREATE TABLE IF NOT EXISTS servicios (
    id_servicio BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) UNIQUE NOT NULL,
    descripcion VARCHAR(500),
    precio_base DECIMAL(10,2) NOT NULL,
    duracion_minutos INT NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    categoria VARCHAR(15) NOT NULL DEFAULT 'PREVENTIVO',
    id_especialidad INT NULL,   
    FOREIGN KEY (id_especialidad) REFERENCES especialidades(id_especialidad) 
);


CREATE TABLE IF NOT EXISTS perfil_mecanico (
    id_perfil INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT UNIQUE NOT NULL,
    anios_experiencia INT NOT NULL DEFAULT 0,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE TABLE IF NOT EXISTS mecanico_especialidad (
    id_perfil INT NOT NULL,
    id_especialidad INT NOT NULL,
    PRIMARY KEY (id_perfil, id_especialidad),
    FOREIGN KEY (id_perfil) REFERENCES perfil_mecanico(id_perfil),
    FOREIGN KEY (id_especialidad) REFERENCES especialidades(id_especialidad)
);

-- ---------- Recuperacion de contrasena (T04.1) ----------

CREATE TABLE IF NOT EXISTS codigos_recuperacion (
    id_codigo BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    codigo_hash VARCHAR(100) NOT NULL,
    expira_en DATETIME NOT NULL,
    usado TINYINT(1) NOT NULL DEFAULT 0,
    intentos INT NOT NULL DEFAULT 0,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- ---------- Horario semanal del mecanico (T21.1) ----------

CREATE TABLE IF NOT EXISTS horario_mecanico (
    id_horario INT AUTO_INCREMENT PRIMARY KEY,
    id_mecanico INT NOT NULL,
    dia_semana TINYINT NOT NULL,          
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    FOREIGN KEY (id_mecanico) REFERENCES usuarios(id_usuario)
);

-- ---------- Citas (T23.1; la logica de reserva llega en T26.1) ----------

USE autosmart_db;

CREATE TABLE IF NOT EXISTS citas (
    id_cita BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket VARCHAR(20) UNIQUE NOT NULL,
    id_cliente INT NOT NULL,
    id_mecanico INT NOT NULL,
    id_vehiculo BIGINT NOT NULL,
    id_servicio BIGINT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',   -- PENDIENTE, CONFIRMADA, RECHAZADA, CANCELADA
    precio DECIMAL(10,2) NOT NULL,
    motivo_rechazo VARCHAR(300) NULL,   
    FOREIGN KEY (id_cliente) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_mecanico) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_vehiculo) REFERENCES vehiculos(id_vehiculo),
    FOREIGN KEY (id_servicio) REFERENCES servicios(id_servicio)
);

-- ---------- Fechas bloqueadas del mecanico (T22.1) ----------

CREATE TABLE IF NOT EXISTS fecha_bloqueada (
    id_bloqueo INT AUTO_INCREMENT PRIMARY KEY,
    id_mecanico INT NOT NULL,
    fecha DATE NOT NULL,
    motivo VARCHAR(200),
    UNIQUE KEY uk_mecanico_fecha (id_mecanico, fecha),
    FOREIGN KEY (id_mecanico) REFERENCES usuarios(id_usuario)
);
-- ---------- Horario del taller y feriados (T40.1) ----------

CREATE TABLE IF NOT EXISTS configuracion_taller (
    dia_semana INT PRIMARY KEY,           -- 1 = lunes ... 7 = domingo
    abierto TINYINT(1) NOT NULL,
    hora_apertura TIME NOT NULL,
    hora_cierre TIME NOT NULL
);

CREATE TABLE IF NOT EXISTS feriados (
    id_feriado INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE UNIQUE NOT NULL,
    descripcion VARCHAR(100) NOT NULL
);

-- ---------- Bloqueo temporal de turnos (T33.1) ----------

CREATE TABLE IF NOT EXISTS bloqueos_turno (
    id_bloqueo BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NOT NULL,
    id_mecanico INT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    expira_en DATETIME NOT NULL,   -- vence 10 minutos despues de crearse
    FOREIGN KEY (id_cliente) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_mecanico) REFERENCES usuarios(id_usuario)
);
