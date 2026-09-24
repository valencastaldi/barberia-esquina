# 6. Frontend

React 18 + Vite 5 + React Router 6, en JavaScript. Se eligió Vite 5 porque funciona con Node 18.

## Estructura

```
frontend/src/
├── main.jsx                 Arranque: router + estilos globales
├── App.jsx                  Rutas del cliente; /admin/* carga el panel aparte (React.lazy)
├── config.js                Datos fijos del local (⚠ de relleno: dirección, teléfono, WhatsApp)
├── api/
│   ├── api.js               fetch a /api/v1: arma la URL, manda el JSON, convierte errores en ErrorApi
│   └── usePedido.js         Hook para cargar datos: { datos, error, cargando, recargar }
├── lib/
│   ├── formato.js           Precios ($9.000) y fechas ("jue 24 de septiembre")
│   ├── horarios.js          "Abierto hoy hasta las 20:00" y "Lun a Mié 10 a 20"
│   └── memoria.js           Lo que se recuerda en el navegador (datos del cliente, última reserva)
├── componentes/             Íconos y piezas del sitio del cliente
├── paginas/cliente/         Inicio, Reservar, Confirmacion, Cancelar, Encuesta, NoEncontrada
├── admin/
│   ├── AdminApp.jsx         Rutas del panel y layout con barra lateral
│   ├── sesion.jsx           Login, token, pedidos con token, RequiereSesion, SoloDueno
│   ├── fechas.js            Rangos día/semana/mes, "hace 3 h", porcentajes
│   ├── componentes/         Lateral (menú), ui.jsx (Kpi, Modal, Segmentos, Interruptor, avisos…)
│   └── paginas/             Login, Agenda, Pagos, Clientes, Dashboard, Opiniones, Peluqueros, Servicios, Horarios
└── estilos/
    ├── base.css             Design system: colores, tipografías, botones, campos, chips de estado
    ├── cliente.css          Sitio del cliente (todo bajo body.cliente)
    ├── admin.css            Panel, portado de la maqueta (todo bajo body.admin)
    └── panel.css            Agregados del panel en React (bajo body.admin)
```

## Sitio del cliente

Mobile-first, desde 320 px (RNF-09). En escritorio se ve como una columna de 430 px centrada.

| Ruta | Pantalla | Qué hace |
|---|---|---|
| `/` | Inicio | Servicios con precio, "Abierto hoy hasta…", satisfacción y comentarios, horarios, dirección |
| `/reservar[?servicio=id]` | Reservar | 3 pasos (RNF-11); con `?servicio` arranca en el paso 2 |
| `/turno/confirmado` | Confirmación | Resumen, número de turno, link para cancelar |
| `/cancelar/:token` | Cancelar | Link del email. Distingue: cancelable, ya cancelado, ya pasó, link vencido |
| `/encuesta/:idTurno?token=` | Encuesta | Link del email. Estrellas + comentario; si ya respondió, agradece |

### El flujo de reserva por dentro

- **Paso 1:** servicios activos (`GET /servicios`).
- **Paso 2:**
  - "Con quién": solo los peluqueros que hacen ese servicio, más "Cualquiera".
  - Tira de días (`GET /disponibilidad/dias`): arranca sola en el primer día con lugar.
  - Horarios (`GET /disponibilidad`): agrupados en mañana, tarde y noche, con los ocupados tachados.
  - Con "Cualquiera", al tocar un horario aparece **"A las 11:00 te atiende: Santiago"** (el menos cargado ese día) y se
    puede cambiar. La reserva se hace con ese peluquero.
- **Paso 3:** datos del cliente, validados en la pantalla y en la API. Si el horario se ocupó mientras tanto (409), vuelve
  al paso 2 con un aviso y los horarios actualizados.
- Nombre, email y celular se recuerdan en el navegador para la próxima reserva.

## Panel de gestión

Escritorio, desde 1024 px (RNF-10). Detalle de qué ve cada rol en [7. Roles y permisos](07-roles-y-permisos.md).

| Ruta | Pantalla |
|---|---|
| `/admin/login` | Ingreso |
| `/admin/agenda` | Día / semana / mes, navegación ‹ Hoy ›, filtro por peluquero, completar (pregunta cómo pagó), ausente, cancelar, bloquear franja |
| `/admin/pagos` | Hoy / 7 / 30 días: cobrado, ticket promedio, para la casa, sin cobrar, por medio de pago, liquidación, movimientos |
| `/admin/clientes` | Buscador, filtros, paginado de 25, ficha lateral con historial |
| `/admin/dashboard` | 7 / 30 / 90 días: KPIs, gráfico diario (SVG propio), estrellas, comentarios, rendimiento por peluquero |
| `/admin/opiniones` | Promedio por peluquero (tocar una tarjeta filtra), lista con filtros "con comentario" y "3 estrellas o menos" |
| `/admin/peluqueros` | Tarjetas del equipo con números de 30 días; alta y edición |
| `/admin/servicios` | Tabla del catálogo; alta, edición, ocultar, borrar |
| `/admin/horarios` | Semana de cada peluquero y slot base ("Mis horarios", sin editar, para el barbero) |

### Sesión

`admin/sesion.jsx` guarda `{ token, vence, usuario }` en `localStorage` hasta que vence (8 h) o se cierra sesión. Todo
pedido del panel pasa por `pedir()`, que agrega el token; si la API responde 401, se borra la sesión y se vuelve al login
recordando a qué pantalla se quería ir.

## Estilos

- **Identidad:** negro + azul del logo (no el dorado del documento). Playfair Display para títulos y precios, Inter para
  la interfaz. Contraste AA, texto mínimo 12 px.
- **Tokens** en `base.css`: `--negro`, `--superficie`, `--azul`, `--azul-claro`, `--rojo` (solo destructivo), `--verde`
  (completado), `--ambar` (estrellas), `--celeste` (Mercado Pago)…
- **Aislamiento:** el sitio y el panel comparten nombres de clase (`.vacio`, `.hora`, `.pie`) con reglas distintas. Por eso
  `cliente.css` va entero dentro de `body.cliente { … }` y `admin.css` dentro de `body.admin { … }` (CSS anidado; Vite lo
  aplana al compilar). Cada layout pone la clase en el `<body>`.
- **Barra inferior del cliente:** se monta en `<body>` con un portal. Si quedara dentro de una sección animada, el
  `transform` de la animación haría que `position: fixed` se ubique respecto de la sección y no de la pantalla.

## La maqueta

`maqueta/` es el prototipo HTML con datos inventados. Quedó como **referencia de diseño**: no se conecta a la API y no tiene
lo que se agregó después (elegir peluquero, opiniones, permisos). Se puede abrir con
`python -m http.server 4173 --directory maqueta`.

## Compilar

```bash
npm run build     # genera dist/
```

El panel sale en archivos separados (`AdminApp-*.js/css`): quien reserva desde el celular descarga unos 62 KB comprimidos y
no baja el código del panel.
