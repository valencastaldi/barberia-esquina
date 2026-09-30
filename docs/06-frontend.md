# 6. Frontend

React 18 + Vite 6 + React Router 7, en JavaScript. Vite 6 es la última versión que funciona con Node 18 (la 7 pide Node 20).

## Estructura

```
frontend/src/
├── main.jsx                 Arranque: router + estilos globales
├── App.jsx                  Rutas del cliente; /admin/* carga el panel aparte (React.lazy)
├── config.js                Datos fijos del local (⚠ de relleno: dirección, teléfono, WhatsApp)
├── api/
│   ├── api.js               fetch a /api/v1: arma la URL, manda el JSON, convierte errores en ErrorApi
│   ├── usePedido.js         Hook para cargar datos: { datos, error, cargando, recargar }
│   ├── cache.js             Respuestas recientes en memoria, para no pedir dos veces lo mismo
│   └── disponibilidad.js    Pedidos del paso 2 de la reserva (días y horarios)
├── lib/
│   ├── formato.js           Precios ($9.000) y fechas ("jue 24 de septiembre")
│   ├── horarios.js          "Abierto hoy hasta las 20:00" y "Lun a Mié 10 a 20"
│   └── memoria.js           Lo que se recuerda en el navegador (datos del cliente, última reserva)
├── componentes/             Íconos, Logo (AVIF/WebP del tamaño justo) y piezas del sitio del cliente
├── paginas/cliente/         Inicio, Reservar, Confirmacion, Cancelar, Encuesta, NoEncontrada
├── admin/
│   ├── AdminApp.jsx         Rutas del panel y layout con barra lateral
│   ├── sesion.jsx           Login, token, pedidos con token, RequiereSesion, SoloDueno
│   ├── fechas.js            Rangos día/semana/mes, "hace 3 h", porcentajes
│   ├── componentes/         Lateral (menú), ui.jsx (Kpi, Modal, Segmentos, Interruptor, avisos…)
│   └── paginas/             Login, Agenda, Pagos, Clientes, Dashboard, Opiniones, Peluqueros, Servicios, Horarios
└── estilos/
    ├── fuentes.css          Inter y Oswald servidas desde el sitio (public/fuentes)
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
  - Tira de días (`GET /disponibilidad/dias`): arranca sola en el primer día con lugar, cuyos horarios vienen en la misma
    respuesta.
  - Horarios (`GET /disponibilidad`, al tocar otro día): agrupados en mañana, tarde y noche, con los ocupados tachados.
  - Con "Cualquiera", al tocar un horario aparece **"A las 11:00 te atiende: Santiago"** (el menos cargado ese día) y se
    puede cambiar. La reserva se hace con ese peluquero.
- **Paso 3:** datos del cliente, validados en la pantalla y en la API. Si el horario se ocupó mientras tanto (409), vuelve
  al paso 2 con un aviso y los horarios actualizados.
- Nombre, email y celular se recuerdan en el navegador para la próxima reserva.

### Fluidez

Todo esto sin cambiar cómo se ve:

- **Nada salta mientras carga.** En la home, el cartel "Abierto hoy…" tiene su lugar reservado desde el principio, y lo
  que va debajo de los servicios (opiniones, dónde estamos, pie) aparece recién cuando llegaron todas las respuestas: así
  ninguna empuja lo que el cliente ya está mirando.
- **Lo que ya se ve no desaparece.** `usePedido` conserva los datos anteriores mientras llegan los nuevos. Al tocar otro
  día u otro peluquero, los horarios de antes quedan atenuados (y sin responder a toques) en lugar de vaciarse la pantalla.
- **El paso 2 en un solo viaje.** Antes eran tres pedidos en cadena (servicios → días → horarios). Ahora:
  - los servicios y los peluqueros los trae la home y quedan en memoria (`api/cache.js`, 5 minutos);
  - los días se piden apenas se elige el servicio (o apenas se entra, si viene en la URL);
  - el primer día con lugar trae sus horarios en la misma respuesta.

  La disponibilidad guardada vale 30 segundos y se olvida al reservar, al cancelar o si un horario se ocupó.
- **Fuentes propias.** Inter y Oswald se sirven desde el sitio (`public/fuentes`, los mismos archivos que entregaba Google
  Fonts) y se precargan en `index.html`: no hay que conectarse a otros dominios antes de mostrar texto.
- **Logo del tamaño justo.** `public/img` tiene el escudo en AVIF (6 a 36 KB según la pantalla) y WebP sin pérdida para
  navegadores que no leen AVIF, en lugar del JPG de 1254 px y 172 KB. El original quedó en `maqueta/assets/img/logo.jpg`.
- **Scroll liviano.** El brillo azul del fondo va en una capa fija aparte (con `background-attachment: fixed` el navegador
  repintaba todo el fondo en cada cuadro) y el punto verde de "Abierto" late con `transform`/`opacity`, que no repintan.

## Panel de gestión

Escritorio, desde 1024 px (RNF-10). Detalle de qué ve cada rol en [7. Roles y permisos](07-roles-y-permisos.md).

| Ruta | Pantalla |
|---|---|
| `/admin/login` | Ingreso |
| `/admin/agenda` | Dos vistas: **Lista** (día / semana / mes / **próximos**: todo lo pendiente de hoy a 30 días, agrupado por día) y **Cronograma** (con una tira de los próximos 14 días y cuántos turnos tiene cada uno para saltar a ese día; el día en una grilla con una columna por peluquero, cada turno como un bloque del largo de su duración, colores por estado, franjas bloqueadas y línea de "ahora"; tocando un bloque se abre el detalle con las mismas acciones). En ambas: navegación ‹ Hoy ›, filtro por peluquero, completar (pregunta cómo pagó), ausente, cancelar, registrar cobro, bloquear franja. La vista elegida se recuerda en el navegador |
| `/admin/pagos` | Hoy / 7 / 30 días: cobrado, ticket promedio, para la casa, sin cobrar, por medio de pago, liquidación, movimientos |
| `/admin/clientes` | Buscador, filtros, paginado de 25, ficha lateral con historial |
| `/admin/dashboard` | 7 / 30 / 90 días: KPIs, gráfico diario (SVG propio), estrellas, comentarios, rendimiento por peluquero |
| `/admin/opiniones` | Promedio por peluquero (tocar una tarjeta filtra), lista con filtros "con comentario" y "3 estrellas o menos" |
| `/admin/peluqueros` | Tarjetas del equipo con números de 30 días; alta y edición, con foto de perfil |
| `/admin/servicios` | Tabla del catálogo; alta, edición, ocultar, borrar |
| `/admin/horarios` | Semana de cada peluquero y slot base ("Mis horarios", sin editar, para el barbero) |

### Sesión

`admin/sesion.jsx` guarda `{ token, vence, usuario }` en `localStorage` hasta que vence (8 h) o se cierra sesión. Todo
pedido del panel pasa por `pedir()`, que agrega el token; si la API responde 401, se borra la sesión y se vuelve al login
recordando a qué pantalla se quería ir.

Guardar el token en `localStorage` es cómodo, pero lo expondría si alguna vez se colara un XSS. Por eso el build
agrega una Content-Security-Policy (`vite.config.js`): solo se ejecutan scripts propios y el navegador no puede mandar
datos a otro dominio que no sea la API. React además escapa todo lo que muestra y no se usa `dangerouslySetInnerHTML`.
La política no se aplica en `npm run dev` porque Vite necesita scripts inline para la recarga en caliente.

## Estilos

- **Identidad:** negro + azul del logo (no el dorado del documento). Oswald para títulos, nombres y números (precios, horarios, días; token `--display`), Inter para
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

El panel sale en archivos separados (`AdminApp-*.js/css`): quien reserva desde el celular descarga unos 75 KB comprimidos
de JS y CSS y no baja el código del panel.
