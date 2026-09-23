-- =========================================================
-- Crea la base y el usuario que usa la API.
-- Correr UNA sola vez en MySQL Workbench conectado como root
-- (botón del rayo ⚡ o Ctrl+Shift+Enter).
--
-- Las tablas NO se crean acá: las crea la API al arrancar
-- (Flyway corre src/main/resources/db/migration/V1__esquema.sql).
-- =========================================================

CREATE DATABASE IF NOT EXISTS esquina_turnos
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- Usuario solo para la app, con permisos únicamente sobre su base.
-- La contraseña es la de desarrollo que figura en application.yml;
-- en producción se usa otra y se pasa por la variable DB_PASSWORD.
CREATE USER IF NOT EXISTS 'esquina'@'localhost' IDENTIFIED BY 'esquina';
GRANT ALL PRIVILEGES ON esquina_turnos.* TO 'esquina'@'localhost';
FLUSH PRIVILEGES;

-- Comprobación: tiene que aparecer esquina_turnos.
SHOW DATABASES LIKE 'esquina_turnos';
