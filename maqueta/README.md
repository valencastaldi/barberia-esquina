# Barbería Esquina — Maqueta del sistema de turnos

Prototipo navegable (HTML + CSS + JS, sin build ni backend) del turnero de
**Barbería Esquina 1290**. Sirve para validar pantallas y flujos antes de
programar la API y la base de datos.

## Cómo verla

```bash
python -m http.server 4173 --directory peluquerias/maqueta
```

Después abrir <http://localhost:4173>. También funciona abriendo los `.html`
directo desde el explorador.

## Identidad

Tomada del logo, no del documento de la Etapa 5 (que proponía dorado):

| Token | Valor | Uso |
|---|---|---|
| `--negro` | `#08090B` | Fondo |
| `--superficie` | `#14171C` | Tarjetas y campos |
| `--azul` / `--azul-claro` | `#1E46D2` / `#4B78FF` | Color de marca, acciones, selección |
| `--rojo` | `#E4322B` | Solo destructivo (cancelar, ausente) |
| `--blanco` | `#F3F1ED` | Texto |
| Tipografías | Playfair Display + Inter | Títulos / interfaz |

## Pantallas

### Cliente — mobile-first, sin login (RNF-09, viewport mínimo 320px)

| Archivo | Pantalla | Requerimientos |
|---|---|---|
| `index.html` | Home: servicios, precios, horarios, CTA | RF-02 (catálogo) |
| `reservar.html` | Flujo de reserva en 3 pasos | RF-01, RF-02, RF-03, RF-27, RNF-11 |
| `confirmacion.html` | Resumen del turno reservado | RF-04, RF-05 |
| `cancelar.html` | Cancelación con link único del email | RF-06, RNF-07 |
| `encuesta.html` | Encuesta 1–5 estrellas + comentario | RF-15, RF-16, RF-17 |

El paso 2 arranca ya posicionado en el primer día con lugar y muestra los
horarios ocupados tachados, para que se vea de un toque cuándo hay turno.

### Barbero — escritorio (RNF-10, viewport mínimo 1024px)

| Archivo | Pantalla | Requerimientos |
|---|---|---|
| `admin/login.html` | Ingreso del super admin | RF-08 |
| `admin/agenda.html` | Agenda del día por peluquero, cambio de estado y cobro al completar | RF-09, RF-10, RF-11, RF-14 |
| `admin/dashboard.html` | Métricas, evolución y satisfacción | RF-18, RF-19, RF-20, RF-21 |
| `admin/servicios.html` | Catálogo, precios, activar/desactivar | RF-22, RF-23, RF-28, RF-29 |
| `admin/horarios.html` | Horario semanal y slot base | RF-24, RF-25, RF-26 |

### Extensión — no está en la documentación original

La tesis está pensada para **un solo barbero** y no contempla pagos ni una
gestión de clientes. Estas pantallas la extienden y llevan la etiqueta
"Extensión" en el título:

| Archivo | Pantalla | Qué agrega al modelo |
|---|---|---|
| `admin/peluqueros.html` | Equipo: días, servicios, comisión y rendimiento de 30 días | Barbero con rol, comisión y servicios que hace |
| `admin/clientes.html` | Buscador, filtros (frecuentes, nuevos, con ausencias, dejaron de venir) y ficha con historial | Solo lectura sobre la tabla Cliente existente |
| `admin/pagos.html` | Cobrado por período, por medio de pago, liquidación por peluquero y cobros pendientes | Tabla nueva `Pago` (id_pago, id_turno, monto, medio, fecha) |

Los números de las tres salen de un historial de 60 días generado con semilla
fija en `datos.js` (`TURNOS`), así que coinciden entre pantallas.

Abajo de 1024px el panel no se rompe: la barra lateral pasa arriba en
horizontal y las columnas se apilan.

## Estructura

```
maqueta/
├── index.html, reservar.html, confirmacion.html, cancelar.html, encuesta.html
├── admin/  login.html, agenda.html, dashboard.html, servicios.html, horarios.html,
│           peluqueros.html, clientes.html, pagos.html
└── assets/
    ├── css/  base.css (design system) · cliente.css · admin.css
    ├── js/   datos.js (mock + utilidades) · reservar.js · admin.js
    └── img/  logo.jpg
```

## Qué reemplazar al conectar el backend

Todo el estado vive en `assets/js/datos.js`. Cada lugar donde la maqueta
simula una llamada tiene el endpoint anotado en un comentario:

| Constante / función | Endpoint real |
|---|---|
| `SERVICIOS` | `GET /api/v1/servicios` |
| `HORARIOS` | `GET /api/v1/horarios` · `PUT /api/v1/horarios` |
| `generarSlots()` | `GET /api/v1/disponibilidad` |
| Submit del paso 3 | `POST /api/v1/turnos` |
| `TURNOS_HOY` | `GET /api/v1/turnos` |
| Botones de estado en la agenda | `PATCH /api/v1/turnos/:id_turno/estado` |
| Botón de la pantalla de cancelación | `PATCH /api/v1/turnos/cancelar/:token_cancelacion` |
| Envío de la encuesta | `POST /api/v1/encuestas/:id_turno` |
| KPIs y gráfico del dashboard | `GET /api/v1/dashboard/resumen` · `/evolucion` |
| Login | `POST /api/v1/auth/login` |
| `PELUQUEROS` *(extensión)* | `GET/POST/PUT /api/v1/peluqueros` · `PATCH /api/v1/peluqueros/:id/estado` |
| `CLIENTES` + `resumenCliente()` *(extensión)* | `GET /api/v1/clientes` · `GET /api/v1/clientes/:id` |
| Registrar cobro *(extensión)* | `GET /api/v1/pagos` · `POST /api/v1/pagos` |

## Pendiente

- Precios, dirección y teléfono son de relleno: hay que cargar los reales.
- Falta la pantalla de bloquear franjas horarias (RF-12), hoy es un botón sin acción.
- Los filtros Semana/Mes de la agenda cambian el botón pero no filtran.
- El cliente todavía no elige peluquero al reservar (hace falta si hay más de uno).
- El dashboard no desglosa por peluquero.
