# 9. Trazabilidad con la tesis

Relaciona lo que pide la documentación de la materia (Etapas 0 a 5) con dónde se cumple en el sistema.

> **Para completar antes de la entrega:** las descripciones completas de cada RF están en la Etapa 1 (Drive), que no
> forma parte del repo. Esta tabla agrupa los RF por el número con el que se trabajaron en la maqueta y el código. Hay que
> verificar la redacción contra la Etapa 1, y en especial **RF-07 y RF-13**, que no quedaron asociados a ninguna pantalla.
> Además, el resumen del PDF habla de **24 RF** y las etapas de **29**: conviene unificar el número.

## Requerimientos funcionales

| RF | Tema (según cómo se usó en la maqueta y el código — verificar) | Dónde se cumple | Estado |
|---|---|---|---|
| RF-01 | Reservar un turno sin cuenta | Sitio: `/reservar` · API: `POST /turnos` | ✅ |
| RF-02 | Ver servicios con precio y duración | Sitio: inicio y paso 1 · API: `GET /servicios` | ✅ |
| RF-03 | Elegir día y horario disponible | Sitio: paso 2 · API: `GET /disponibilidad` | ✅ |
| RF-04 | Confirmación por email | Email "Tu turno está confirmado" (`EmailServicio`) | ✅ (con SMTP configurado) |
| RF-05 | Pantalla de turno confirmado | Sitio: `/turno/confirmado` | ✅ |
| RF-06 | Cancelar con link único sin login | Sitio: `/cancelar/:token` · API: `PATCH /turnos/cancelar/{token}` | ✅ |
| RF-07 | *Verificar en la Etapa 1* | — | ❓ |
| RF-08 | Login del barbero | Panel: `/admin/login` · API: `POST /auth/login` | ✅ |
| RF-09 | Agenda diaria / semanal / mensual | Panel: Agenda (Día · Semana · Mes) · API: `GET /turnos` | ✅ |
| RF-10 | Marcar turno completado | Panel: Agenda · API: `PATCH /turnos/{id}/estado` | ✅ |
| RF-11 | Marcar ausente / cancelar desde el panel | Panel: Agenda · API: `PATCH /turnos/{id}/estado` | ✅ |
| RF-12 | Bloquear franjas horarias | Panel: Agenda → "Bloquear franja" · API: `/bloqueos` | ✅ |
| RF-13 | *Verificar en la Etapa 1* | — | ❓ |
| RF-14 | Al completar, enviar la encuesta | `TurnoServicio.cambiarEstado` → email "¿Cómo te atendimos?" | ✅ (con SMTP configurado) |
| RF-15 | Encuesta de 1 a 5 estrellas | Sitio: `/encuesta/:id` · API: `/encuestas/{idTurno}` | ✅ |
| RF-16 | Comentario opcional | Idem (hasta 500 caracteres) | ✅ |
| RF-17 | Una respuesta por turno | Tabla `encuesta` con `UNIQUE id_turno`; 409 si se repite | ✅ |
| RF-18 | Dashboard: cantidad de turnos | Panel: Dashboard · API: `GET /dashboard/resumen` | ✅ |
| RF-19 | Dashboard: satisfacción promedio | Idem + distribución de estrellas | ✅ |
| RF-20 | Dashboard: tasa de ausentismo | Idem | ✅ |
| RF-21 | Dashboard: evolución / comentarios | Gráfico diario (`/dashboard/evolucion`) y últimos comentarios | ✅ |
| RF-22 | Alta de servicios | Panel: Servicios · API: `POST /servicios` | ✅ |
| RF-23 | Edición de servicios y precios | Panel: Servicios · API: `PUT /servicios/{id}` | ✅ |
| RF-24 | Configurar horario de atención | Panel: Horarios · API: `PUT /horarios` | ✅ |
| RF-25 | Cerrar un día sin perder su horario | Interruptor por día (`activo`) | ✅ |
| RF-26 | Duración del slot base | Panel: Horarios → slot base | ✅ |
| RF-27 | Calcular horarios disponibles | `DisponibilidadServicio` ([detalle](08-reglas-de-negocio.md#disponibilidad-rf-27)) | ✅ |
| RF-28 | Activar / ocultar servicios | Panel: Servicios (interruptor) · API: `PATCH /servicios/{id}/estado` | ✅ |
| RF-29 | Eliminar servicios | API: `DELETE /servicios/{id}` (solo sin turnos; si no, se oculta) | ✅ |

## Requerimientos no funcionales

| RNF | Dónde se cumple | Estado |
|---|---|---|
| Respuesta en menos de 2 s | Consultas con índices (`turno(id_barbero, fecha)`, `turno(fecha)`); agregados con `GROUP BY` en clientes | ✅ en local (sin medición formal) |
| 50 usuarios concurrentes | API sin estado (JWT), pool de conexiones de Hikari | ⚠ no se hizo prueba de carga |
| Emails en menos de 2 min | Envío asíncrono inmediato después del commit | ✅ (depende del SMTP) |
| Contraseñas con bcrypt costo ≥ 10 | `BCryptPasswordEncoder(10)` | ✅ |
| JWT con expiración | 8 h, validado con el reloj de la app | ✅ |
| HTTPS / TLS 1.2+ | Se resuelve en el proxy del servidor de producción | ⏳ al publicar |
| RNF-07 · Token de cancelación de un solo uso, 48 h | `token_cancelacion` + `token_vencimiento` | ✅ |
| Rate limiting: 10 reservas por IP por hora | `LimiteDeReservasFiltro` | ✅ |
| RNF-09 · Cliente usable desde 320 px | Probado a 320 y 375 px sin scroll horizontal | ✅ |
| RNF-10 · Panel desde 1024 px | Probado a 1024 y 1366 px | ✅ |
| RNF-11 · Reserva en ≤ 3 pasos | Servicio → peluquero, día y hora → datos | ✅ |
| Uptime 99 % | Depende del hosting | ⏳ al publicar |
| Validación de disponibilidad en tiempo real antes de confirmar | Bloqueo + reverificación ([detalle](08-reglas-de-negocio.md#reservas-simultáneas)) | ✅ |
| Backups diarios, retención 7 días | Se configuran en el servidor de MySQL (`mysqldump` programado) | ⏳ al publicar |
| Arquitectura en capas | `web → servicio → repositorio → modelo` | ✅ |
| SPA + API REST | React + Spring Boot | ✅ |
| Preparado para múltiples barberos | Ya los usa (extensión) | ✅ |

## Etapas de la documentación

| Etapa | Contenido | Dónde está en el sistema |
|---|---|---|
| 0 · Contextualización, MoSCoW, MVP | El MVP (reservar, cancelar, agenda, encuesta, dashboard) está completo | [1. Visión general](01-vision-general.md) |
| 1 · Requerimientos | Tablas de arriba | Este documento |
| 2 · DER, diccionario, SQL | `V1__esquema.sql` | [4. Modelo de datos](04-modelo-de-datos.md) |
| 3 · Componentes, casos de uso, estados, secuencia | Diagramas de arquitectura, secuencia y estados | [3. Arquitectura](03-arquitectura.md) · [8. Reglas](08-reglas-de-negocio.md) |
| 4 · Módulos y 19 endpoints | M1 a M7 implementados, más agregados | [5. API](05-api.md) |
| 5 · Design system, user flows, mockups | `maqueta/` y `frontend/src/estilos/` | [6. Frontend](06-frontend.md) |

## Diferencias con lo documentado

Resumidas en [1. Visión general](01-vision-general.md#alcance-lo-que-pide-la-tesis-y-lo-que-se-agregó) y, para la base de
datos, en [4. Modelo de datos](04-modelo-de-datos.md#diferencias-con-la-etapa-2). Las más importantes para la defensa:

1. **Varios peluqueros con roles** en lugar de un único barbero.
2. **Cobros y liquidación**: tabla `pago`, que no estaba en el DER.
3. **Tabla `bloqueo`** para RF-12, que no tenía tabla ni endpoint.
4. **Endpoints agregados** a los 19 (disponibilidad por días, reseñas, opiniones, clientes, pagos…), todos justificados en
   [5. API](05-api.md#agregados).
5. **Paleta negro + azul** del logo real en lugar de negro + dorado.
