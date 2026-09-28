-- [EXT] Foto de perfil de cada peluquero.
-- La imagen va en una tabla aparte para que cargar la lista del equipo no traiga los bytes.
-- Se guarda en la base (y no en una carpeta del servidor) para que viaje con los backups.
CREATE TABLE barbero_foto (
    id_barbero  BIGINT      NOT NULL,
    contenido   MEDIUMBLOB  NOT NULL,   -- hasta 16 MB; el front la manda recortada a 400x400
    tipo        VARCHAR(20) NOT NULL,   -- image/jpeg, image/png o image/webp
    CONSTRAINT pk_barbero_foto PRIMARY KEY (id_barbero),
    CONSTRAINT fk_barbero_foto_barbero FOREIGN KEY (id_barbero) REFERENCES barbero (id_barbero)
);

-- Cuándo cambió la foto: va en la URL (?v=…) para que el navegador no muestre una vieja.
ALTER TABLE barbero ADD COLUMN foto_actualizada DATETIME NULL;
