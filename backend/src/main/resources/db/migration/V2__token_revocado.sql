-- Tokens de sesión cerrados antes de vencer ("Cerrar sesión" en el panel).
-- Se guarda el id del JWT (claim jti) hasta que el token vence; después ya no
-- sirve igual y la fila se borra sola (LimpiezaDeTokens).
CREATE TABLE token_revocado (
    jti    VARCHAR(64) NOT NULL,
    vence  DATETIME    NOT NULL,
    CONSTRAINT pk_token_revocado PRIMARY KEY (jti)
);
CREATE INDEX ix_token_revocado_vence ON token_revocado (vence);
