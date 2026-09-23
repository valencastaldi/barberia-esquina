-- =========================================================
-- Esquema de Barbería Esquina — basado en la Etapa 2 (DER).
-- Las diferencias con el documento están marcadas con [EXT]
-- (extensión multi-peluquero y pagos) o [AJUSTE].
-- =========================================================

CREATE TABLE barbero (
    id_barbero        BIGINT       NOT NULL AUTO_INCREMENT,
    nombre            VARCHAR(60)  NOT NULL,
    apellido          VARCHAR(60)  NOT NULL,
    dni               VARCHAR(15)  NULL,
    fecha_nacimiento  DATE         NULL,
    email             VARCHAR(120) NOT NULL,
    password_hash     VARCHAR(100) NOT NULL,
    telefono          VARCHAR(30)  NULL,
    fecha_alta        DATETIME     NOT NULL,
    rol               ENUM('dueno', 'barbero') NOT NULL DEFAULT 'barbero',   -- [EXT]
    comision_pct      TINYINT      NOT NULL DEFAULT 0,                        -- [EXT] % que cobra el peluquero
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,                     -- [EXT]
    CONSTRAINT pk_barbero PRIMARY KEY (id_barbero),
    CONSTRAINT uq_barbero_email UNIQUE (email),
    CONSTRAINT uq_barbero_dni UNIQUE (dni),
    CONSTRAINT ck_barbero_comision CHECK (comision_pct BETWEEN 0 AND 100)
);

-- [AJUSTE] En el documento Servicio tenía id_barbero. Con varios peluqueros el
-- catálogo es de la barbería y la relación "quién hace qué" va en barbero_servicio.
CREATE TABLE servicio (
    id_servicio       BIGINT        NOT NULL AUTO_INCREMENT,
    nombre            VARCHAR(60)   NOT NULL,
    descripcion       VARCHAR(160)  NULL,
    duracion_minutos  SMALLINT      NOT NULL,
    precio            DECIMAL(10,2) NOT NULL,
    activo            BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_servicio PRIMARY KEY (id_servicio),
    CONSTRAINT ck_servicio_duracion CHECK (duracion_minutos > 0),
    CONSTRAINT ck_servicio_precio CHECK (precio >= 0)
);

-- [EXT] Qué servicios hace cada peluquero.
CREATE TABLE barbero_servicio (
    id_barbero   BIGINT NOT NULL,
    id_servicio  BIGINT NOT NULL,
    CONSTRAINT pk_barbero_servicio PRIMARY KEY (id_barbero, id_servicio),
    CONSTRAINT fk_bs_barbero  FOREIGN KEY (id_barbero)  REFERENCES barbero (id_barbero),
    CONSTRAINT fk_bs_servicio FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio)
);

-- dia_semana: 0 = domingo ... 6 = sábado (igual que el documento).
CREATE TABLE horario_atencion (
    id_horario         BIGINT   NOT NULL AUTO_INCREMENT,
    id_barbero         BIGINT   NOT NULL,
    dia_semana         TINYINT  NOT NULL,
    hora_inicio        TIME     NOT NULL,
    hora_fin           TIME     NOT NULL,
    duracion_slot_min  SMALLINT NOT NULL DEFAULT 30,
    activo             BOOLEAN  NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_horario PRIMARY KEY (id_horario),
    CONSTRAINT fk_horario_barbero FOREIGN KEY (id_barbero) REFERENCES barbero (id_barbero),
    CONSTRAINT uq_horario_dia UNIQUE (id_barbero, dia_semana),
    CONSTRAINT ck_horario_dia CHECK (dia_semana BETWEEN 0 AND 6),
    CONSTRAINT ck_horario_rango CHECK (hora_inicio < hora_fin)
);

-- Se crea sola al reservar; el cliente no tiene cuenta.
-- [AJUSTE] email único: así un cliente que vuelve se reconoce por su email.
CREATE TABLE cliente (
    id_cliente  BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(60)  NOT NULL,
    apellido    VARCHAR(60)  NOT NULL,
    email       VARCHAR(120) NOT NULL,
    telefono    VARCHAR(30)  NOT NULL,
    fecha_alta  DATETIME     NOT NULL,
    CONSTRAINT pk_cliente PRIMARY KEY (id_cliente),
    CONSTRAINT uq_cliente_email UNIQUE (email)
);

CREATE TABLE turno (
    id_turno            BIGINT        NOT NULL AUTO_INCREMENT,
    id_cliente          BIGINT        NOT NULL,
    id_servicio         BIGINT        NOT NULL,
    id_barbero          BIGINT        NOT NULL,
    fecha               DATE          NOT NULL,
    hora_inicio         TIME          NOT NULL,
    hora_fin            TIME          NOT NULL,
    estado              ENUM('pendiente', 'completado', 'ausente', 'cancelado') NOT NULL DEFAULT 'pendiente',
    precio              DECIMAL(10,2) NOT NULL,   -- [AJUSTE] precio al momento de reservar
    token_cancelacion   VARCHAR(64)   NOT NULL,
    token_vencimiento   DATETIME      NOT NULL,   -- [AJUSTE] RNF: token de un solo uso, vence a las 48 h
    token_encuesta      VARCHAR(64)   NULL,       -- [AJUSTE] evita que cualquiera responda la encuesta de otro
    fecha_creacion      DATETIME      NOT NULL,
    CONSTRAINT pk_turno PRIMARY KEY (id_turno),
    CONSTRAINT fk_turno_cliente  FOREIGN KEY (id_cliente)  REFERENCES cliente (id_cliente),
    CONSTRAINT fk_turno_servicio FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio),
    CONSTRAINT fk_turno_barbero  FOREIGN KEY (id_barbero)  REFERENCES barbero (id_barbero),
    CONSTRAINT uq_turno_token UNIQUE (token_cancelacion),
    CONSTRAINT uq_turno_token_encuesta UNIQUE (token_encuesta),
    CONSTRAINT ck_turno_rango CHECK (hora_inicio < hora_fin)
);
CREATE INDEX ix_turno_barbero_fecha ON turno (id_barbero, fecha);
CREATE INDEX ix_turno_fecha ON turno (fecha);

CREATE TABLE encuesta (
    id_encuesta      BIGINT       NOT NULL AUTO_INCREMENT,
    id_turno         BIGINT       NOT NULL,
    calificacion     TINYINT      NOT NULL,
    comentario       VARCHAR(500) NULL,
    fecha_respuesta  DATETIME     NOT NULL,
    CONSTRAINT pk_encuesta PRIMARY KEY (id_encuesta),
    CONSTRAINT fk_encuesta_turno FOREIGN KEY (id_turno) REFERENCES turno (id_turno),
    CONSTRAINT uq_encuesta_turno UNIQUE (id_turno),
    CONSTRAINT ck_encuesta_calificacion CHECK (calificacion BETWEEN 1 AND 5)
);

-- [EXT] Cobro de un turno completado.
CREATE TABLE pago (
    id_pago  BIGINT        NOT NULL AUTO_INCREMENT,
    id_turno BIGINT        NOT NULL,
    monto    DECIMAL(10,2) NOT NULL,
    medio    ENUM('efectivo', 'transferencia', 'mercadopago') NOT NULL,
    fecha    DATETIME      NOT NULL,
    CONSTRAINT pk_pago PRIMARY KEY (id_pago),
    CONSTRAINT fk_pago_turno FOREIGN KEY (id_turno) REFERENCES turno (id_turno),
    CONSTRAINT uq_pago_turno UNIQUE (id_turno),
    CONSTRAINT ck_pago_monto CHECK (monto >= 0)
);

-- RF-12: franjas bloqueadas por el peluquero (trámite, almuerzo, etc.).
-- No figura en el DER; se agrega porque el requerimiento la necesita.
CREATE TABLE bloqueo (
    id_bloqueo   BIGINT       NOT NULL AUTO_INCREMENT,
    id_barbero   BIGINT       NOT NULL,
    fecha        DATE         NOT NULL,
    hora_inicio  TIME         NOT NULL,
    hora_fin     TIME         NOT NULL,
    motivo       VARCHAR(120) NULL,
    CONSTRAINT pk_bloqueo PRIMARY KEY (id_bloqueo),
    CONSTRAINT fk_bloqueo_barbero FOREIGN KEY (id_barbero) REFERENCES barbero (id_barbero),
    CONSTRAINT ck_bloqueo_rango CHECK (hora_inicio < hora_fin)
);
CREATE INDEX ix_bloqueo_barbero_fecha ON bloqueo (id_barbero, fecha);
