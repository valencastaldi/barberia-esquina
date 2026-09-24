# 10. Pruebas

## Tests automáticos del backend

```bash
cd backend
./mvnw test
```

**36 tests de integración.** Levantan la API completa (Spring, seguridad, JPA, Flyway) sobre **H2 en memoria en modo MySQL**,
con las mismas migraciones que la base real, y le hacen pedidos HTTP con MockMvc.

Usan un **reloj fijo**: martes 22/09/2026 a las 11:00 (Córdoba). Así "hoy" y "ya pasó" dan siempre lo mismo, se corran el
día que se corran. Antes de cada test la base se vacía y se carga una barbería mínima:

- Corte (30 min, $9.000) y Corte + Barba (60 min, $15.000)
- Agustín (dueño) y Santiago (barbero, comisión 50 %), los dos atienden martes de 10 a 20

La base común está en `PruebaDeIntegracion.java`.

### Qué cubren

**`DisponibilidadTest`** (10) — cálculo de horarios
- no ofrece horarios que ya pasaron
- un servicio largo no puede empezar si pisa un turno
- los cancelados no ocupan lugar
- sin peluquero elegido alcanza con que uno tenga lugar
- los libres vienen ordenados por quien tiene menos trabajo ese día
- una franja bloqueada no se ofrece
- un día que nadie atiende no tiene horarios
- no se ofrecen días pasados ni demasiado lejanos
- el selector de días cuenta los libres
- un peluquero que no hace el servicio da error

**`FlujoDeTurnosTest`** (12) — reserva, cancelación, encuesta, cobro
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

**`SeguridadTest`** (14) — login y permisos
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
