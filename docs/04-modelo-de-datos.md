# 4. Modelo de datos

Base MySQL `esquina_turnos`. El esquema completo está en
[`backend/src/main/resources/db/migration/V1__esquema.sql`](../backend/src/main/resources/db/migration/V1__esquema.sql)
y lo aplica Flyway al arrancar la API.

## Diagrama entidad-relación

```mermaid
erDiagram
    BARBERO ||--o{ HORARIO_ATENCION : "atiende en"
    BARBERO }o--o{ SERVICIO : "hace (barbero_servicio)"
    BARBERO ||--o{ TURNO : "atiende"
    BARBERO ||--o{ BLOQUEO : "bloquea"
    CLIENTE ||--o{ TURNO : "reserva"
    SERVICIO ||--o{ TURNO : "es de"
    TURNO ||--o| ENCUESTA : "recibe"
    TURNO ||--o| PAGO : "se cobra con"

    BARBERO {
        bigint id_barbero PK
        varchar nombre
        varchar apellido
        varchar dni UK
        date fecha_nacimiento
        varchar email UK
        varchar password_hash
        varchar telefono
        datetime fecha_alta
        enum rol "dueno | barbero"
        tinyint comision_pct
        boolean activo
    }
    SERVICIO {
        bigint id_servicio PK
        varchar nombre
        varchar descripcion
        smallint duracion_minutos
        decimal precio
        boolean activo
    }
    HORARIO_ATENCION {
        bigint id_horario PK
        bigint id_barbero FK
        tinyint dia_semana "0 dom … 6 sáb"
        time hora_inicio
        time hora_fin
        smallint duracion_slot_min
        boolean activo
    }
    CLIENTE {
        bigint id_cliente PK
        varchar nombre
        varchar apellido
        varchar email UK
        varchar telefono
        datetime fecha_alta
    }
    TURNO {
        bigint id_turno PK
        bigint id_cliente FK
        bigint id_servicio FK
        bigint id_barbero FK
        date fecha
        time hora_inicio
        time hora_fin
        enum estado "pendiente | completado | ausente | cancelado"
        decimal precio
        varchar token_cancelacion UK
        datetime token_vencimiento
        varchar token_encuesta UK
        datetime fecha_creacion
    }
    ENCUESTA {
        bigint id_encuesta PK
        bigint id_turno FK "UK"
        tinyint calificacion "1 a 5"
        varchar comentario
        datetime fecha_respuesta
    }
    PAGO {
        bigint id_pago PK
        bigint id_turno FK "UK"
        decimal monto
        enum medio "efectivo | transferencia | mercadopago"
        datetime fecha
    }
    BLOQUEO {
        bigint id_bloqueo PK
        bigint id_barbero FK
        date fecha
        time hora_inicio
        time hora_fin
        varchar motivo
    }
```

## Tablas

| Tabla | Qué guarda | Notas |
|---|---|---|
| `barbero` | Quienes usan el panel | `rol` = dueño (super admin) o barbero. `comision_pct`: parte que cobra el peluquero. Nunca se borra: se desactiva. |
| `servicio` | Catálogo (corte, barba…) | `activo = false` lo oculta al cliente sin perder el historial. |
| `barbero_servicio` | Qué servicios hace cada peluquero | Un servicio nuevo se asigna a todos los activos. |
| `horario_atencion` | Semana de cada peluquero | Uno por peluquero y día (`UNIQUE`). `duracion_slot_min`: cada cuánto arranca un turno posible. |
| `cliente` | Quienes reservaron | Se crea sola al reservar. Se reconoce por email (`UNIQUE`). |
| `turno` | Cada reserva | Guarda el `precio` del momento, así los cambios de precio no alteran el historial. |
| `encuesta` | Respuesta de satisfacción | Una por turno (`UNIQUE id_turno`). |
| `pago` | Cobro de un turno completado | Uno por turno (`UNIQUE id_turno`). |
| `bloqueo` | Franjas en que un peluquero no atiende | RF-12. No se superpone con turnos pendientes. |

Restricciones (`CHECK`) que protegen los datos aunque alguien escriba directo en la base: calificación entre 1 y 5, comisión
entre 0 y 100, precio y monto no negativos, duración mayor a 0, día de la semana entre 0 y 6, y hora de inicio anterior a la
de fin en horarios, turnos y bloqueos.

## Ciclo de vida del turno

```mermaid
stateDiagram-v2
    [*] --> pendiente: el cliente reserva
    pendiente --> completado: el peluquero lo marca<br/>(desde su hora de inicio)
    pendiente --> ausente: el cliente no vino<br/>(desde su hora de inicio)
    pendiente --> cancelado: el cliente con el link<br/>o la barbería
    completado --> [*]: se envía la encuesta
    ausente --> [*]
    cancelado --> [*]: el horario queda libre
```

Un turno cambia de estado **una sola vez**. Los cancelados no ocupan lugar en la agenda.

## Diferencias con la Etapa 2

En el SQL están marcadas con `[EXT]` (extensión) o `[AJUSTE]`.

| Tema | Documento | Implementación | Motivo |
|---|---|---|---|
| Barbero | Solo datos personales y login | Suma `rol`, `comision_pct`, `activo` | Varios peluqueros (extensión) |
| Servicio | Tenía `id_barbero` | Sin `id_barbero`; tabla `barbero_servicio` | Con varios peluqueros el catálogo es de la barbería |
| Turno | — | Suma `precio`, `token_vencimiento`, `token_encuesta` | Precio histórico; vencimiento de 48 h del link (RNF); que nadie responda la encuesta de otro |
| Cliente | Email sin restricción | Email `UNIQUE` | Reconocer al cliente que vuelve |
| Pago | No existía | Tabla `pago` | Extensión |
| Bloqueo | No estaba en el DER | Tabla `bloqueo` | La pide RF-12 |

## Cambiar el esquema

**Nunca** se edita `V1__esquema.sql` una vez que alguien corrió la API: Flyway detecta el cambio y no arranca. Para cambiar
algo se agrega un archivo nuevo, por ejemplo `V2__agrega_notas_a_turno.sql`. Ver [11. Guía de desarrollo](11-guia-de-desarrollo.md#cambiar-la-base).

Para ver el diagrama real en Workbench: *Database → Reverse Engineer…* sobre `esquina_turnos`.
