# Barbería Esquina — Sistema de turnos

Sistema web de gestión de turnos para **Barbería Esquina 1290** (Córdoba, Argentina).
Trabajo final de la materia *Taller de Desarrollo de Aplicaciones*.

Hoy los turnos se coordinan por WhatsApp y llamadas. Con este sistema:

- **El cliente**, desde el celular y sin crear cuenta, elige servicio, peluquero, día y horario en 3 pasos,
  recibe la confirmación por email, puede cancelar con un link y al terminar responde una encuesta.
- **La barbería**, desde el panel, maneja la agenda, los cobros, los clientes, el equipo, los servicios,
  los horarios y ve métricas.

## Estructura

| Carpeta | Qué es | Stack |
|---|---|---|
| [`backend/`](backend/) | API REST `/api/v1` | Java 21 · Spring Boot 4 · Spring Security (JWT) · JPA · Flyway · MySQL 8 |
| [`frontend/`](frontend/) | SPA: sitio del cliente y panel de gestión | React 18 · Vite 5 · React Router |
| [`maqueta/`](maqueta/) | Prototipo navegable de todas las pantallas | HTML · CSS · JS |

## Cómo correrlo

Requisitos: Java 21, Node 18+, MySQL 8.

1. Crear la base: ejecutar [`backend/db/crear-base.sql`](backend/db/crear-base.sql) como root (por ejemplo en MySQL Workbench).
2. API:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
3. Front, en otra terminal:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
4. Abrir <http://localhost:5173> (sitio del cliente) o <http://localhost:5173/admin> (panel).

La primera vez se cargan datos de ejemplo (3 peluqueros, 150 clientes, ~550 turnos).
Usuario del panel: `agustin@barberiaesquina.com` / `esquina1290`, **solo para desarrollo**.

Más detalle en el [README del backend](backend/README.md) (endpoints, seguridad, diferencias con la documentación)
y en el [del front](frontend/README.md).

## Estado

- [x] Maqueta de todas las pantallas
- [x] API completa: 19 endpoints de la documentación + extensión (peluqueros, clientes, pagos, bloqueos) · 31 tests
- [x] Sitio del cliente: inicio, reserva en 3 pasos, confirmación, cancelación, encuesta
- [x] Panel de gestión: agenda, pagos, clientes, dashboard, peluqueros, servicios, horarios
- [ ] Datos reales de la barbería (precios, dirección, teléfono)
