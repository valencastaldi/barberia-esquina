# 12. Pendientes

## Antes de la entrega

| Tarea | Detalle |
|---|---|
| **Datos reales de la barbería** | Precios y duraciones reales (se cargan desde el panel → Servicios). Dirección, teléfono, WhatsApp e Instagram en `frontend/src/config.js`. |
| **Verificar la trazabilidad** | Revisar la redacción de los RF contra la Etapa 1, completar RF-07 y RF-13 y unificar si son 24 o 29 RF ([9. Trazabilidad](09-trazabilidad.md)). |
| **Documentar las diferencias en la tesis** | Varios peluqueros, pagos, bloqueos, endpoints agregados, paleta azul. |
| **Diagrama ER final** | Exportarlo desde Workbench (*Reverse Engineer*) para la entrega. |

## Para publicarlo

| Tarea | Detalle |
|---|---|
| Servidor y dominio | Hosting para la API (Java 21) y MySQL; el front compilado (`dist/`) se puede servir desde el mismo dominio. |
| HTTPS | Certificado en el proxy (por ejemplo Nginx o Caddy con Let's Encrypt). |
| Variables de producción | `SPRING_PROFILES_ACTIVE=prod` y todas las de [2. Instalación](02-instalacion.md#variables-de-entorno), con contraseñas nuevas. |
| Email real | Un SMTP (Gmail con contraseña de aplicación, Brevo, etc.) y `MAIL_HABILITADO=true`. |
| Backups | `mysqldump` diario con 7 días de retención (RNF). |
| Contraseñas | Cambiar las de los usuarios de ejemplo o crear los reales y desactivar los de prueba. |

## Mejoras posibles

| Mejora | Por qué |
|---|---|
| Tests del frontend (Vitest + Testing Library) | Hoy solo el backend tiene tests automáticos. |
| Prueba de carga | Verificar el RNF de 50 usuarios concurrentes (por ejemplo con k6). |
| Recordatorio por email el día anterior | Baja el ausentismo. Una tarea programada (`@Scheduled`). |
| Revocar tokens al cerrar sesión | Hoy el JWT sigue valiendo hasta que vence. |
| Límite de reservas compartido | Vive en memoria: con más de una instancia de la API habría que moverlo a Redis. |
| Que el cliente vea sus turnos | Hoy solo tiene el link de cada turno. Se podría mandar un link a "mis turnos" por email. |
| Pagos online (Mercado Pago) | Hoy se registra cómo pagó en el local; no hay cobro online. |
