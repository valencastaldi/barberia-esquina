# 10. Pruebas

## Tests automáticos del backend

```bash
cd backend
./mvnw test
```

**69 tests de integración.** Levantan la API completa (Spring, seguridad, JPA, Flyway) sobre **H2 en memoria en modo MySQL**,
con las mismas migraciones que la base real, y le hacen pedidos HTTP con MockMvc.

Usan un **reloj fijo**: martes 22/09/2026 a las 11:00 (Córdoba). Así "hoy" y "ya pasó" dan siempre lo mismo, se corran el
día que se corran. Antes de cada test la base se vacía y se carga una barbería mínima:

- Corte (30 min, $9.000) y Corte + Barba (60 min, $15.000)
- Agustín (dueño) y Santiago (barbero, comisión 50 %), los dos atienden martes de 10 a 20

La base común está en `PruebaDeIntegracion.java`.

### Qué cubren

**`DisponibilidadTest`** (12) — cálculo de horarios
- no ofrece horarios que ya pasaron
- un servicio largo no puede empezar si pisa un turno
- los cancelados no ocupan lugar
- sin peluquero elegido alcanza con que uno tenga lugar
- los libres vienen ordenados por quien tiene menos trabajo ese día
- una franja bloqueada no se ofrece
- un día que nadie atiende no tiene horarios
- no se ofrecen días pasados ni demasiado lejanos
- el selector de días cuenta los libres
- el primer día con lugar trae sus horarios (los mismos que `GET /disponibilidad`)
- los días no hacen una consulta por peluquero y por día (14 días con dos peluqueros: 5 consultas como máximo)
- un peluquero que no hace el servicio da error

**`FlujoDeTurnosTest`** (13) — reserva, cancelación, encuesta, cobro
- reservar ocupa el horario y no se puede reservar dos veces
- **8 reservas simultáneas al mismo horario guardan una sola**
- sin peluquero elegido se asigna uno libre
- el cliente que vuelve se reconoce por su email
- no se reserva en el pasado ni fuera de horario
- los datos del cliente se validan
- el cliente cancela con el link y el horario se libera
- el link de cancelación vence a las 48 h
- completar habilita la encuesta una sola vez
- no se completa un turno que todavía no empezó
- cobrar un turno y verlo en el reporte (liquidación incluida)
- la agenda lista los turnos del día con su cobro
- sin aceptar los términos no se reserva

**`TurnoDesdePanelTest`** (7) — turnos cargados desde el panel
- sin sesión no se puede
- el dueño carga un turno sin email para otro peluquero
- el cliente sin email se reconoce por el teléfono; con email, se usa el cliente que ya existe
- un barbero solo se carga turnos a sí mismo
- respeta la disponibilidad
- el email, si viene, tiene que ser válido

**`EdicionDeTurnoTest`** (6) — corregir o borrar un turno completado
- el barbero corrige su turno cobrado y Pagos lo refleja
- solo el dueño pasa un turno a otro peluquero
- borrar un turno cobrado lo saca de Pagos; un barbero no borra turnos ajenos
- solo se corrigen o borran turnos completados
- un turno cobrado no queda sin medio de pago

**`ConsultasDelPanelTest`** (3) — pantallas del panel en un pedido
- `GET /clientes` trae los totales de cada filtro (los mismos que pidiéndolos uno por uno); buscar no los cambia
- `GET /dashboard` da lo mismo que `/dashboard/resumen`, `/evolucion` y `/barberos` juntos
- Clientes cuesta 6 consultas como máximo y el Dashboard 5 (antes, con todos sus pedidos, 30 y 12)

**`SeguridadTest`** (22) — login y permisos
- el panel pide login; lo que usa el cliente es público
- login con credenciales incorrectas; un peluquero dado de baja no entra
- el token identifica al usuario
- solo el dueño administra catálogo y equipo
- un barbero no ve la información del negocio (clientes, dashboard, pagos, equipo)
- en la agenda un barbero no ve contacto ni cobros de turnos ajenos
- un barbero no cambia horarios ni bloquea franjas
- todo el equipo ve las opiniones
- un barbero solo cobra y maneja sus propios turnos
- siempre queda un dueño activo
- más de diez reservas por hora desde la misma IP se cortan
- después de diez logins fallidos la IP tiene que esperar
- un dueño pasado a barbero pierde los permisos al instante, sin esperar a que venza el token
- el token de un peluquero dado de baja deja de servir; cambiar la contraseña cierra las sesiones anteriores
- cerrar sesión anula ese token pero no las otras sesiones del mismo usuario
- la limpieza borra solo los tokens revocados que ya vencieron
- una reserva pública no cambia los datos de un cliente que ya existe
- los reportes del panel no aceptan rangos de más de un año

**`FotoBarberoTest`** (3) — foto de perfil
- el dueño sube la foto y se ve sin login
- solo se aceptan imágenes de verdad (se mira el contenido, no lo que dice el navegador)
- un barbero no cambia fotos

**`CacheHttpTest`** (3) — qué puede guardar el navegador
- servicios, peluqueros, horario y reseñas se guardan un minuto (`Cache-Control: max-age=60`)
- con sesión no se guarda nada: el panel tiene que ver lo que acaba de editar
- la disponibilidad no se guarda nunca

### H2 no es MySQL

Los tests corren sobre H2 para ser rápidos y no depender de nada instalado, pero H2 **no se comporta igual** que MySQL en
todo. El caso concreto: las reservas simultáneas pasaban en H2 y fallaban en MySQL (ver
[8. Reglas de negocio](08-reglas-de-negocio.md#reservas-simultáneas)). Por eso los cambios que tocan transacciones o SQL se
prueban también contra MySQL real, con la API levantada.

## Prueba manual (checklist)

Con la API y el front corriendo sobre MySQL:

**Cliente (en el celular o con el navegador en 375 px)**
- [ ] La home muestra servicios, "Abierto hoy…", reseñas y horarios
- [ ] Reservar con "Cualquiera": al tocar un horario aparece quién atiende
- [ ] Reservar con un peluquero elegido
- [ ] Confirmar con datos vacíos marca los campos
- [ ] La confirmación muestra el número de turno
- [ ] El link de cancelar cancela; abrirlo de nuevo dice "ya estaba cancelado"
- [ ] A 320 px no hay scroll horizontal

**Panel como dueño (1366 y 1024 px)**
- [ ] Agenda: Día / Semana / Mes y ‹ Hoy › cambian lo que se ve
- [ ] Completar con cobro actualiza la fila y los números
- [ ] Completar un turno futuro muestra el aviso
- [ ] Bloquear encima de un turno avisa; en una franja libre se bloquea y se quita
- [ ] Pagos, Clientes (búsqueda y ficha), Dashboard, Opiniones, Peluqueros, Servicios, Horarios cargan

**Panel como barbero**
- [ ] El menú muestra solo Agenda, Opiniones y Mis horarios
- [ ] En turnos ajenos no hay teléfono, precio ni botones
- [ ] Escribir `/admin/pagos` en la barra lleva a la agenda
- [ ] Mis horarios no se puede editar

## Frontend

No tiene tests automáticos todavía (ver [12. Pendientes](12-pendientes.md)). Para verificar que compila:

```bash
cd frontend && npm run build
```
