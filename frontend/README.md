# Front — Barbería Esquina

SPA en React 18 + Vite 5 + React Router 6 (JavaScript). Diseño portado de `../maqueta`
(mismo `base.css` y `cliente.css`).

## Cómo correrlo

Con la API levantada en el puerto 8080 (ver `../backend/README.md`):

```bash
cd frontend
npm install        # la primera vez
npm run dev
```

Abrir <http://localhost:5173>. Vite reenvía `/api` a `http://localhost:8080`, así que no hace falta configurar CORS.

`npm run build` genera `dist/` para publicar. Si la API queda en otro dominio: `VITE_API_URL=https://api.ejemplo.com/api/v1`.

## Sitio del cliente (mobile-first, desde 320px)

| Ruta | Pantalla | Endpoints |
|---|---|---|
| `/` | Inicio: servicios, abierto/cerrado, reseñas, horarios | `GET /servicios`, `/horarios`, `/resenas` |
| `/reservar` | 3 pasos: servicio → peluquero, día y hora → datos | `GET /barberos`, `/disponibilidad/dias`, `/disponibilidad` · `POST /turnos` |
| `/turno/confirmado` | Resumen, número de turno, link para cancelar | — (respuesta del POST) |
| `/cancelar/:token` | Link del email: ver y cancelar el turno | `GET` y `PATCH /turnos/cancelar/:token` |
| `/encuesta/:idTurno?token=` | Link del email: 1 a 5 estrellas + comentario | `GET` y `POST /encuestas/:idTurno` |

Detalles:
- Con "Cualquiera", al tocar un horario aparece quién lo atiende (sugerido: el peluquero con menos
  turnos ese día, según el orden que devuelve la API). Si hay varios libres se puede cambiar ahí mismo,
  y la reserva se hace con ese peluquero.
- Si el horario se ocupa mientras el cliente completa sus datos (409), vuelve al paso 2 con un aviso y los horarios actualizados.
- Los datos del cliente se recuerdan en el navegador para la próxima reserva (`localStorage`, opcional).
- Los datos del local que no están en la base (dirección, teléfono) viven en `src/config.js` y **son de relleno**.

## Panel de gestión (escritorio, desde 1024px)

Entrar en <http://localhost:5173/admin> con `agustin@barberiaesquina.com` / `esquina1290` (dueño)
o `santiago@barberiaesquina.com` / `esquina1290` (barbero). Solo desarrollo.

| Ruta | Pantalla | Quién |
|---|---|---|
| `/admin/login` | Ingreso (JWT, vence a las 8 h) | Todos |
| `/admin/agenda` | Día / semana / mes, filtro por peluquero, completar con cobro, ausente, cancelar | Todos (el barbero, sus turnos) |
| `/admin/opiniones` | Encuestas: promedio de cada peluquero y comentarios, con filtros | Todos |
| `/admin/horarios` | Semana de cada peluquero y slot base; bloquear franjas (RF-12) se hace desde la agenda | Edita el dueño; el barbero ve "Mis horarios" |
| `/admin/pagos` | Cobrado, medios de pago, liquidación por peluquero, cobros pendientes | Dueño |
| `/admin/clientes` | Buscador, filtros, paginado y ficha con historial | Dueño |
| `/admin/dashboard` | KPIs, gráfico diario, estrellas, comentarios, rendimiento por peluquero | Dueño |
| `/admin/peluqueros` | Equipo: alta, edición, servicios, activar/desactivar | Dueño |
| `/admin/servicios` | Catálogo: alta, edición, ocultar, borrar si no tiene turnos | Dueño |

**El barbero** ve la agenda de todo el equipo pero solo toca y cobra sus turnos. No cambia horarios ni bloquea franjas.
Ve las opiniones de todo el equipo (las suyas y las de los demás). De los turnos ajenos
no recibe el contacto del cliente, el cobro ni la calificación (lo filtra la API, no solo la pantalla),
y los números de la agenda son solo los suyos.

El panel se carga aparte (`React.lazy`): quien reserva desde el celular no descarga su código.
`cliente.css` y `admin.css` viven bajo `body.cliente` / `body.admin` para que no se pisen.

## Estructura

```
src/
├── api/          api.js (fetch + errores) · usePedido.js (hook de carga)
├── lib/          formato (fechas, precios) · horarios (abierto/cerrado) · memoria (localStorage)
├── componentes/  Iconos · cliente/ (layout, barra inferior, resumen, tarjeta de servicio)
├── paginas/      cliente/ (Inicio, Reservar, Confirmacion, Cancelar, Encuesta)
├── admin/        panel: sesion.jsx (login y token) · componentes/ · paginas/ (Agenda, Pagos, Clientes, …)
├── estilos/      base.css (design system) · cliente.css · admin.css + panel.css
└── config.js     datos fijos del local
```

## Pendiente

- Datos reales del local en `src/config.js`.
