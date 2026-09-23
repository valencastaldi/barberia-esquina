# API de turnos — Barbería Esquina

Backend del sistema de turnos (trabajo final de Taller de Desarrollo de Aplicaciones).
Java 21 · Spring Boot 4.1 · Spring Security (JWT) · JPA/Hibernate · Flyway · MySQL 8.

## Cómo correrlo

### Con MySQL instalado en la máquina (lo normal)

1. **Una sola vez:** abrir [`db/crear-base.sql`](db/crear-base.sql) en MySQL Workbench,
   conectado como `root`, y ejecutarlo (⚡). Crea la base `esquina_turnos` y el usuario `esquina`.
2. Arrancar la API:

   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

   La primera vez Flyway crea las tablas y se cargan los datos de ejemplo.

- API en <http://localhost:8080/api/v1>
- En Workbench se puede abrir una conexión con el usuario `esquina` / `esquina` para ver las tablas.
- Los emails (confirmación, encuesta) no se mandan en desarrollo: se anotan en el log.
  Para mandarlos de verdad: `MAIL_HABILITADO=true` + los datos de un SMTP (ver abajo).

Para empezar de cero: `DROP DATABASE esquina_turnos;` en Workbench, volver a correr el script y reiniciar la API.

### Sin MySQL (H2 en archivo)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

Usa una base H2 en `backend/data/` en modo MySQL, con las mismas migraciones.
Útil para probar algo rápido; la entrega usa MySQL.

### Usuario inicial

Al arrancar con la base vacía se crea el dueño y, en desarrollo, una barbería de
ejemplo (3 peluqueros, 150 clientes, ~550 turnos de las últimas 6 semanas y la próxima).

| Usuario | Contraseña | Rol |
|---|---|---|
| `agustin@barberiaesquina.com` | `esquina1290` | Dueño (super admin) |
| `santiago@barberiaesquina.com` | `esquina1290` | Barbero |
| `joaquin@barberiaesquina.com` | `esquina1290` | Barbero |

### Tests

```bash
./mvnw test
```

34 tests de integración sobre H2 con un reloj fijo (martes 22/09/2026 11:00):
disponibilidad, reserva, reservas simultáneas, cancelación, encuesta, pagos, permisos por rol y rate limiting.

## Arquitectura en capas

```
web/          Controladores REST + DTOs (records) + manejo de errores (RFC 9457)
servicio/     Lógica de negocio y transacciones
repositorio/  Spring Data JPA
modelo/       Entidades JPA y enums
seguridad/    JWT, roles, límite de reservas por IP
config/       Propiedades (app.*) y reloj con zona horaria de Córdoba
demo/         Carga del dueño y de los datos de ejemplo
```

El esquema lo maneja Flyway: `src/main/resources/db/migration/V1__esquema.sql`.
Hibernate no crea ni modifica tablas (`ddl-auto: none`).

## Endpoints

Todo bajo `/api/v1`. 🔓 = público (lo usa el cliente sin cuenta) · 🔑 = con login · 👑 = solo dueño.

### Los 19 de la Etapa 4

| Módulo | Método y ruta | Acceso | Qué hace |
|---|---|---|---|
| M1 Auth | `POST /auth/login` | 🔓 | Devuelve el JWT (vence en 8 h) |
| | `POST /auth/logout` | 🔑 | 204; el front descarta el token |
| | `GET /auth/me` | 🔑 | Usuario logueado |
| M2 Servicios | `GET /servicios` | 🔓 | Activos; `?todos=true` con login incluye ocultos |
| | `POST /servicios` | 👑 | Alta |
| | `PUT /servicios/{id}` | 👑 | Edición |
| | `PATCH /servicios/{id}/estado` | 👑 | `{"activo": false}` lo oculta |
| | `DELETE /servicios/{id}` | 👑 | Solo si nunca tuvo turnos (si no, 409: desactivarlo) |
| M3 Horarios | `GET /horarios` | 🔓 | De la barbería; `?barbero=ID` el de un peluquero |
| | `PUT /horarios` | 🔑 | Semana propia; el dueño puede `?barbero=ID` |
| M4 Disponibilidad | `GET /disponibilidad?servicio=&fecha=[&barbero=]` | 🔓 | Horarios del día, libres y ocupados |
| M5 Turnos | `POST /turnos` | 🔓 | Reserva (máx. 10 por IP por hora) |
| | `GET /turnos?desde=&hasta=&barbero=&estado=` | 🔑 | Agenda (por defecto, hoy). Un barbero no recibe contacto, cobro ni calificación de turnos ajenos |
| | `PATCH /turnos/{id}/estado` | 🔑 | completado / ausente / cancelado |
| | `PATCH /turnos/cancelar/{token}` | 🔓 | Cancelación desde el email |
| M6 Encuestas | `GET /encuestas/{idTurno}?token=` | 🔓 | Datos para la pantalla de encuesta |
| | `POST /encuestas/{idTurno}?token=` | 🔓 | `{"calificacion": 1-5, "comentario": "..."}` |
| M7 Dashboard | `GET /dashboard/resumen?desde=&hasta=&barbero=` | 👑 | KPIs, estrellas, comentarios (por defecto, 30 días) |
| | `GET /dashboard/evolucion?desde=&hasta=&barbero=` | 👑 | Turnos por día para el gráfico |

### Agregados

| Ruta | Acceso | Por qué |
|---|---|---|
| `GET /disponibilidad/dias?servicio=[&barbero=][&cantidad=14]` | 🔓 | Selector de días del paso 2: cuántos horarios libres tiene cada día |
| `GET /resenas` | 🔓 | Home del cliente: satisfacción promedio, atendidos del mes y 3 comentarios buenos (nombre corto) |
| `GET /turnos/cancelar/{token}` | 🔓 | La pantalla de cancelación necesita mostrar el turno antes de cancelar |
| `GET /bloqueos` · `POST /bloqueos` · `DELETE /bloqueos/{id}` | 🔑 | RF-12 (bloquear franjas) no tenía endpoint |
| `GET /barberos` | 🔓 | *Extensión:* el cliente elige con quién atenderse |
| `GET /barberos/equipo` · `POST` · `PUT /{id}` · `PATCH /{id}/estado` | 👑 | *Extensión:* equipo de peluqueros |
| `GET /clientes?q=&filtro=&pagina=&tamano=` · `GET /clientes/{id}` | 👑 | *Extensión:* listado con filtros y ficha con historial |
| `GET /pagos?desde=&hasta=` | 👑 | *Extensión:* cobrado, por medio de pago, liquidación |
| `POST /pagos` | 🔑 | *Extensión:* registrar el cobro de un turno completado (el barbero, solo de los suyos) |
| `GET /dashboard/barberos` | 👑 | *Extensión:* rendimiento por peluquero |

Errores: siempre `{"title", "status", "detail"}`; los de validación suman `"errores": {campo: mensaje}`.
Códigos: 400 datos inválidos · 401 sin login · 403 sin permiso · 404 · 409 conflicto
(horario tomado, ya cobrado…) · 422 regla de negocio · 429 demasiadas reservas.

## Diferencias con la documentación

| Tema | Documento | Implementación | Motivo |
|---|---|---|---|
| Barberos | Uno solo (super admin) | Varios, con rol `dueno`/`barbero`, comisión y servicios que hace | Extensión pedida |
| Servicio | Tenía `id_barbero` | Catálogo de la barbería + tabla `barbero_servicio` | Con varios peluqueros el catálogo es compartido |
| Pagos | No existía | Tabla `pago` (1 por turno) | Extensión pedida |
| Bloqueos (RF-12) | Sin tabla | Tabla `bloqueo` | El requerimiento la necesita |
| Turno | — | Suma `precio`, `token_vencimiento`, `token_encuesta` | Precio histórico; vencimiento de 48 h (RNF); que nadie responda la encuesta de otro |
| Cliente | Email sin restricción | Email único | Reconocer al cliente que vuelve |
| Encuesta | `POST /encuestas/:id_turno` | Igual, pero con `?token=` del email | Seguridad |

## Requerimientos no funcionales cubiertos

- **bcrypt costo 10** para contraseñas; login con el mismo mensaje y tiempo si el email no existe.
- **JWT con vencimiento** (8 h), firmado HS256; secreto por variable de entorno en producción.
- **Token de cancelación** aleatorio de 256 bits, de un solo uso y con vencimiento de 48 h.
- **Rate limiting**: 10 reservas por IP por hora (429).
- **Validación en tiempo real**: antes de guardar, la reserva bloquea la fila del peluquero
  (`SELECT … FOR UPDATE`) y vuelve a verificar el horario. Dos clientes no pueden quedarse con el mismo turno.
- **Emails asíncronos** después del commit: la reserva responde enseguida.
- **Preparado para múltiples barberos**: ya lo usa.
- **HTTPS**: se resuelve en el proxy de producción; el perfil `prod` toma la IP real (`forward-headers-strategy`).

## Variables de entorno (producción: `SPRING_PROFILES_ACTIVE=prod`)

| Variable | Obligatoria en prod | Ejemplo |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | sí | `jdbc:mysql://db:3306/esquina_turnos` |
| `JWT_SECRET` | sí (≥ 32 caracteres) | |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | la contraseña sí | dueño inicial |
| `FRONTEND_URL` | sí | `https://turnos.barberiaesquina.com` (links de los emails) |
| `ORIGENES_PERMITIDOS` | sí | mismo dominio del front (CORS) |
| `MAIL_HABILITADO`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD` | sí | `true` + SMTP real (p. ej. Gmail con contraseña de aplicación) |

## Pendiente

- Backups diarios con retención de 7 días: se configuran en el servidor de MySQL, no en la API.
- El límite de reservas por IP vive en memoria: con más de una instancia de la API habría que moverlo a Redis.
- Revocar tokens al cerrar sesión (hoy el JWT sigue valiendo hasta que vence).
