-- [EXT] Turnos cargados desde el panel (por teléfono o de alguien que vino sin reservar):
-- el cliente puede no tener email. UNIQUE admite varios NULL, así que sigue sin haber emails repetidos.
ALTER TABLE cliente MODIFY email VARCHAR(120) NULL;
