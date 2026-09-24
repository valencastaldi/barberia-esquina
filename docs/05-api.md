# 5. API REST

Base: `http://localhost:8080/api/v1`. Todo en JSON. Fechas `AAAA-MM-DD`, horas `HH:mm`.

**Acceso:** 🔓 público · 🔑 con login (cualquier rol) · 👑 solo dueño.
Con login, cada pedido lleva el header `Authorization: Bearer <token>`.

## Resumen

### Los 19 endpoints de la Etapa 4

| Módulo | Endpoint | Acceso |
|---|---|---|
| M1 Autenticación | `POST /auth/login` · `POST /auth/logout` · `GET /auth/me` | 🔓 · 🔑 · 🔑 |
| M2 Servicios | `GET /servicios` · `POST /servicios` · `PUT /servicios/{id}` · `PATCH /servicios/{id}/estado` · `DELETE /servicios/{id}` | 🔓 · 👑 · 👑 · 👑 · 👑 |
| M3 Horarios | `GET /horarios` · `PUT /horarios` | 🔓 · 👑 |
| M4 Disponibilidad | `GET /disponibilidad` | 🔓 |
| M5 Turnos | `POST /turnos` · `GET /turnos` · `PATCH /turnos/{id}/estado` · `PATCH /turnos/cancelar/{token}` | 🔓 · 🔑 · 🔑 · 🔓 |
| M6 Encuestas | `GET /encuestas/{idTurno}` · `POST /encuestas/{idTurno}` | 🔓 (con token) |
| M7 Dashboard | `GET /dashboard/resumen` · `GET /dashboard/evolucion` | 👑 |

### Agregados

| Endpoint | Acceso | Por qué |
|---|---|---|
| `GET /disponibilidad/dias` | 🔓 | Selector de días: cuántos horarios libres tiene cada uno |
| `GET /turnos/cancelar/{token}` | 🔓 | Mostrar el turno antes de cancelarlo |
| `GET /resenas` | 🔓 | Satisfacción y comentarios para la home del cliente |
| `GET /barberos` | 🔓 | *Extensión:* peluqueros activos para elegir con quién |
| `GET /barberos/equipo` · `POST /barberos` · `PUT /barberos/{id}` · `PATCH /barberos/{id}/estado` | 👑 | *Extensión:* equipo |
| `GET /bloqueos` · `POST /bloqueos` · `DELETE /bloqueos/{id}` | 🔑 · 👑 · 👑 | RF-12: bloquear franjas |
| `GET /clientes` · `GET /clientes/{id}` | 👑 | *Extensión:* clientes |
| `GET /pagos` · `POST /pagos` | 👑 · 🔑 | *Extensión:* cobros y liquidación |
| `GET /dashboard/barberos` | 👑 | *Extensión:* rendimiento por peluquero |
| `GET /opiniones` | 🔑 | Encuestas con promedio por peluquero, para todo el equipo |

## Errores

Todos los errores tienen la misma forma ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)); `detail` es un mensaje listo
para mostrar:

```json
{ "title": "Conflicto", "status": 409, "detail": "Ese horario se acaba de ocupar. Elegí otro." }
```

Los de validación suman los campos con problemas:

```json
{
  "title": "Datos inválidos", "status": 400, "detail": "Revisá los datos marcados",
  "errores": { "cliente.email": "must be a well-formed email address" }
}
```

| Código | Cuándo |
|---|---|
| 400 | Datos mal formados o incompletos |
| 401 | Sin login, token inválido o vencido, credenciales incorrectas |
| 403 | Con login pero sin permiso (por ejemplo, un barbero pidiendo `/pagos`) |
| 404 | No existe (o el link/token no es válido) |
| 409 | Choca con el estado actual: horario tomado, ya cobrado, ya respondida, email repetido |
| 422 | Regla de negocio: completar un turno que no empezó, bloquear un día pasado, link vencido |
| 429 | Más de 10 reservas por hora desde la misma conexión |

---

## M1 · Autenticación

### `POST /auth/login` 🔓

```json
{ "email": "agustin@barberiaesquina.com", "password": "esquina1290" }
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9…",
  "vence": "2026-09-24T07:30:00Z",
  "usuario": { "id": 1, "nombre": "Agustín", "apellido": "Rosas", "email": "agustin@barberiaesquina.com", "rol": "dueno" }
}
```

Incorrecto → 401 `"Email o contraseña incorrectos"` (el mismo mensaje si el email no existe o si el peluquero está desactivado).

### `POST /auth/logout` 🔑 → 204

El token no se guarda en el servidor: cerrar sesión es descartarlo en el front.

### `GET /auth/me` 🔑 → el `usuario` del login.

## M2 · Servicios

### `GET /servicios` 🔓

Activos. Con login y `?todos=true`, también los ocultos.

```json
[{ "id": 1, "nombre": "Corte Corto", "descripcion": "Máquina y tijera, lavado incluido",
   "duracionMinutos": 30, "precio": 9000.00, "activo": true }]
```

### `POST /servicios` · `PUT /servicios/{id}` 👑

```json
{ "nombre": "Color", "descripcion": "Decoloración y matiz", "duracionMinutos": 90, "precio": 25000 }
```

Duración entre 5 y 480 minutos. Un servicio nuevo se asigna a todos los peluqueros activos.

### `PATCH /servicios/{id}/estado` 👑 — `{ "activo": false }` lo oculta.

### `DELETE /servicios/{id}` 👑 — 204, o 409 si ya tiene turnos (en ese caso se oculta).

## M3 · Horarios

### `GET /horarios` 🔓

Sin parámetros: el horario **de la barbería** (un día está abierto si atiende algún peluquero). Con `?barbero=2`: el de ese
peluquero. Siempre 7 días, 0 = domingo:

```json
[{ "diaSemana": 0, "horaInicio": null, "horaFin": null, "duracionSlotMin": 30, "activo": false },
 { "diaSemana": 1, "horaInicio": "10:00", "horaFin": "20:00", "duracionSlotMin": 30, "activo": true }]
```

### `PUT /horarios?barbero=2` 👑

```json
{ "dias": [{ "diaSemana": 1, "horaInicio": "10:00", "horaFin": "20:00", "duracionSlotMin": 30, "activo": true }] }
```

No modifica turnos ya reservados.

## M4 · Disponibilidad

### `GET /disponibilidad?servicio=3&fecha=2026-09-25[&barbero=2]` 🔓

Todos los horarios del día, **libres y ocupados** (la pantalla muestra los ocupados tachados). `barberos` son los libres a esa
hora, **ordenados por quién tiene menos turnos ese día**:

```json
{
  "fecha": "2026-09-25", "idServicio": 3, "duracionMinutos": 60,
  "slots": [
    { "hora": "10:00", "libre": false, "barberos": [] },
    { "hora": "10:30", "libre": true,  "barberos": [2, 1] }
  ]
}
```

Días pasados, a más de 30 días o sin atención → `slots: []`. Si el peluquero no hace el servicio → 422.

### `GET /disponibilidad/dias?servicio=3[&barbero=2][&cantidad=14]` 🔓

```json
[{ "fecha": "2026-09-24", "atiende": true, "libres": 15 },
 { "fecha": "2026-09-27", "atiende": false, "libres": 0 }]
```

## M5 · Turnos

### `POST /turnos` 🔓 — reservar

```json
{
  "idServicio": 3, "idBarbero": 2, "fecha": "2026-09-25", "hora": "10:30",
  "cliente": { "nombre": "Lucas", "apellido": "Ferreyra", "email": "lucas@gmail.com", "telefono": "351 711-0043" }
}
```

`idBarbero` puede ser `null`: se asigna el libre con menos turnos ese día. Respuesta 201:

```json
{
  "idTurno": 562, "fecha": "2026-09-25", "horaInicio": "10:30", "horaFin": "11:30",
  "servicio": "Corte + Barba", "barbero": "Santiago Molina", "precio": 15000.00,
  "tokenCancelacion": "CNPaUSXqe_DFoct41HGUUGBISEPJa3-GCSPwkdmZFDA",
  "cancelableHasta": "2026-09-25T10:30:00"
}
```

409 si el horario se ocupó · 422 si la fecha está fuera de rango · 429 si se pasó el límite por IP.
Después del commit se envía el email de confirmación con el link `/cancelar/{token}`.

### `GET /turnos?desde=&hasta=&barbero=&estado=` 🔑 — agenda

Sin fechas: hoy. Rango máximo de un año.

```json
[{
  "id": 518, "fecha": "2026-09-23", "horaInicio": "11:00", "horaFin": "12:00",
  "estado": "completado", "precio": 15000.00,
  "cliente": { "id": 40, "nombre": "Alan", "apellido": "Medina", "telefono": "351 …", "email": "…" },
  "servicio": { "id": 3, "nombre": "Corte + Barba" },
  "barbero": { "id": 1, "nombre": "Agustín" },
  "pago": { "medio": "transferencia", "monto": 15000.00, "fecha": "2026-09-23T12:00:00" },
  "calificacion": 5
}]
```

Para un **barbero**, en los turnos de otros peluqueros `cliente.telefono`, `cliente.email`, `pago` y `calificacion` vienen vacíos.

### `PATCH /turnos/{id}/estado` 🔑

```json
{ "estado": "completado" }
```

`completado` o `ausente` solo desde la hora de inicio (si no, 422). Un turno cerrado no cambia más (409). El barbero solo
cambia los suyos (403). `completado` envía la encuesta; `cancelado`, un aviso al cliente.

### `GET /turnos/cancelar/{token}` · `PATCH /turnos/cancelar/{token}` 🔓

```json
{
  "idTurno": 562, "fecha": "2026-09-25", "horaInicio": "10:30", "horaFin": "11:30",
  "servicio": "Corte + Barba", "barbero": "Santiago", "precio": 15000.00, "cliente": "Lucas",
  "estado": "pendiente", "cancelable": true, "cancelableHasta": "2026-09-25T10:30:00"
}
```

El PATCH devuelve lo mismo con `estado: "cancelado"`. Segunda vez → 409. Vencido → 422. Token inexistente → 404.

## M6 · Encuestas (link del email)

### `GET /encuestas/{idTurno}?token=…` 🔓

```json
{ "idTurno": 518, "fecha": "2026-09-23", "servicio": "Corte Corto", "barbero": "Santiago", "cliente": "Franco",
  "respondida": false, "calificacion": null, "comentario": null }
```

### `POST /encuestas/{idTurno}?token=…` 🔓 → 201

```json
{ "calificacion": 5, "comentario": "Impecable" }
```

Token incorrecto o turno no completado → 404. Ya respondida → 409.

## M7 · Dashboard 👑

Sin fechas: los últimos 30 días.

### `GET /dashboard/resumen?desde=&hasta=&barbero=`

```json
{
  "turnos": 340, "pendientes": 22, "completados": 290, "ausentes": 16, "cancelados": 28,
  "tasaAusentismo": 0.052, "facturacion": 2784000.00, "satisfaccion": 4.47, "encuestas": 175,
  "distribucionEstrellas": { "5": 109, "4": 48, "3": 12, "2": 2, "1": 4 },
  "ultimosComentarios": [{ "cliente": "Santino R.", "calificacion": 5, "comentario": "…", "fecha": "…" }]
}
```

`tasaAusentismo` = ausentes / (completados + ausentes).

### `GET /dashboard/evolucion` — `[{ "fecha": "2026-09-01", "turnos": 12, "completados": 10 }, …]` (días sin turnos en 0).

### `GET /dashboard/barberos` — `[{ "idBarbero": 1, "nombre": "Agustín Rosas", "completados": 108, "ausentes": 5, "facturado": 1074000.00, "satisfaccion": 4.4 }]`

## Extensión

### `GET /barberos` 🔓 — `[{ "id": 1, "nombre": "Agustín", "apellido": "Rosas", "servicios": [1, 2, 3, 4, 5] }]`

### `GET /barberos/equipo` 👑 — ficha completa: email, teléfono, rol, comisión, activo, servicios y `diasAtencion`.

### `POST /barberos` · `PUT /barberos/{id}` 👑

```json
{ "nombre": "Joaquín", "apellido": "Vera", "email": "joaquin@barberiaesquina.com", "telefono": "351 555-0187",
  "rol": "barbero", "comisionPct": 45, "servicios": [1, 4], "password": "contraseña-inicial" }
```

`password` es obligatoria al crear y opcional al editar. Email repetido → 409.

### `PATCH /barberos/{id}/estado` 👑 — `{ "activo": false }`. No se puede dejar la barbería sin dueño activo (422).

### `GET /bloqueos?desde=&hasta=&barbero=` 🔑 · `POST /bloqueos` 👑 · `DELETE /bloqueos/{id}` 👑

```json
{ "idBarbero": 2, "fecha": "2026-09-26", "horaInicio": "13:00", "horaFin": "14:00", "motivo": "Trámite" }
```

Si hay turnos pendientes en la franja → 409 (primero hay que cancelarlos).

### `GET /clientes?q=&filtro=&pagina=0&tamano=25` 👑

`filtro`: `todos`, `frecuentes` (4+ visitas), `nuevos` (primer turno en 30 días), `ausencias`, `perdidos` (3+ visitas, no
vuelven hace más de 3 semanas y no tienen turno sacado).

```json
{ "contenido": [{ "id": 1, "nombre": "Mateo", "apellido": "Giménez", "email": "…", "telefono": "…",
                  "visitas": 6, "ausencias": 0, "gastado": 54000.00, "ultimaVisita": "2026-09-22",
                  "primeraVisita": "2026-08-10", "habitual": { "id": 1, "nombre": "Agustín" } }],
  "total": 151, "pagina": 0, "tamano": 25 }
```

### `GET /clientes/{id}` 👑 — `{ cliente, satisfaccion, proximoTurno, historial: [turnos] }`

### `GET /pagos?desde=&hasta=` 👑 — sin fechas, hoy

```json
{
  "resumen": { "cobrado": 50000, "cobros": 4, "ticketPromedio": 12500, "paraLaCasa": 32500,
               "sinCobrarMonto": 15000, "sinCobrarCantidad": 1 },
  "porMedio": [{ "medio": "efectivo", "monto": 24000, "porcentaje": 48 }],
  "liquidacion": [{ "idBarbero": 2, "nombre": "Santiago", "rol": "barbero", "comisionPct": 50,
                    "facturado": 21000, "leToca": 10500 }],
  "movimientos": [ /* turnos completados; los sin cobrar primero */ ]
}
```

### `POST /pagos` 🔑

```json
{ "idTurno": 518, "monto": 15000, "medio": "transferencia" }
```

Solo turnos completados (422), uno por turno (409). El barbero solo cobra los suyos (403).

### `GET /opiniones?desde=&hasta=&barbero=` 🔑 — sin fechas, 90 días

```json
{
  "promedio": 4.4, "cantidad": 257,
  "porBarbero": [{ "idBarbero": 2, "nombre": "Santiago", "promedio": 4.4, "cantidad": 75,
                   "distribucion": { "5": 45, "4": 20, "3": 7, "2": 1, "1": 2 } }],
  "opiniones": [{ "idTurno": 518, "fecha": "2026-09-23", "servicio": "Corte Corto",
                  "barbero": { "id": 2, "nombre": "Santiago" }, "cliente": "Franco B.",
                  "calificacion": 4, "comentario": "Muy buen corte", "fechaRespuesta": "…" }]
}
```

`porBarbero` siempre trae a todo el equipo, aunque se filtre por uno, para poder compararse.

### `GET /resenas` 🔓

```json
{ "promedio": 4.4, "encuestas": 256, "atendidosMes": 223,
  "comentarios": [{ "cliente": "Santino R.", "calificacion": 5, "comentario": "Buena onda y buen precio.", "fecha": "…" }] }
```

Últimos 90 días; solo comentarios de 4 o 5 estrellas.
