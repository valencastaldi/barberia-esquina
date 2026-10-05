package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.config.AppProperties;
import com.barberiaesquina.turnos.modelo.*;
import com.barberiaesquina.turnos.repositorio.*;
import com.barberiaesquina.turnos.seguridad.SesionActual;
import com.barberiaesquina.turnos.servicio.excepcion.ConflictoException;
import com.barberiaesquina.turnos.servicio.excepcion.NoEncontradoException;
import com.barberiaesquina.turnos.servicio.excepcion.ReglaNegocioException;
import com.barberiaesquina.turnos.web.dto.TurnoDtos.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class TurnoServicio {

    private final TurnoRepositorio turnos;
    private final ClienteRepositorio clientes;
    private final BarberoRepositorio barberos;
    private final ServicioRepositorio servicios;
    private final PagoRepositorio pagos;
    private final EncuestaRepositorio encuestas;
    private final DisponibilidadServicio disponibilidad;
    private final Calendario calendario;
    private final SesionActual sesion;
    private final ApplicationEventPublisher eventos;
    private final AppProperties props;

    public TurnoServicio(TurnoRepositorio turnos, ClienteRepositorio clientes, BarberoRepositorio barberos,
                         ServicioRepositorio servicios, PagoRepositorio pagos, EncuestaRepositorio encuestas, DisponibilidadServicio disponibilidad,
                         Calendario calendario, SesionActual sesion, ApplicationEventPublisher eventos,
                         AppProperties props) {
        this.turnos = turnos;
        this.clientes = clientes;
        this.barberos = barberos;
        this.servicios = servicios;
        this.pagos = pagos;
        this.encuestas = encuestas;
        this.disponibilidad = disponibilidad;
        this.calendario = calendario;
        this.sesion = sesion;
        this.eventos = eventos;
        this.props = props;
    }

    // ------------------------------------------------------------------
    // Reserva (RF-01, RF-03, RF-04)
    // ------------------------------------------------------------------

    /**
     * READ_COMMITTED es necesario: MySQL usa REPEATABLE_READ por defecto y ahí,
     * aunque la reserva espere el bloqueo del peluquero, después seguiría leyendo
     * la "foto" de la base tomada al empezar y no vería el turno que otro cliente
     * acaba de guardar en ese mismo horario (se probó: quedaban turnos duplicados).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservaRespuesta reservar(ReservaPedido pedido) {
        Servicio servicio = disponibilidad.servicioReservable(pedido.idServicio());
        Barbero barbero = barberoLibre(servicio, pedido.idBarbero(), pedido.fecha(), pedido.hora());
        Turno turno = guardarTurno(servicio, barbero, pedido.fecha(), pedido.hora(), registrarCliente(pedido.cliente()));

        return new ReservaRespuesta(turno.getId(), turno.getFecha(), turno.getHoraInicio(), turno.getHoraFin(),
                servicio.getNombre(), barbero.nombreCompleto(), turno.getPrecio(),
                turno.getTokenCancelacion(), cancelableHasta(turno));
    }

    /**
     * [Extensión] Turno cargado desde el panel: por teléfono o de alguien que vino sin reservar.
     * Mismas reglas de disponibilidad que la reserva pública. El dueño se lo puede cargar a
     * cualquiera; un barbero solo a sí mismo. Si hay email, el cliente recibe la confirmación.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Detalle reservarDesdePanel(TurnoPanelPedido pedido) {
        Long idBarbero = pedido.idBarbero();
        if (!sesion.esDueno()) {
            if (idBarbero != null && !idBarbero.equals(sesion.idBarbero())) {
                throw new AccessDeniedException("Solo el dueño puede cargar turnos a otro peluquero");
            }
            idBarbero = sesion.idBarbero();
        }
        Servicio servicio = disponibilidad.servicioReservable(pedido.idServicio());
        Barbero barbero = barberoLibre(servicio, idBarbero, pedido.fecha(), pedido.hora());
        Turno turno = guardarTurno(servicio, barbero, pedido.fecha(), pedido.hora(),
                registrarClienteDelPanel(pedido.cliente()));
        return detalles(List.of(turno)).getFirst();
    }

    private Barbero barberoLibre(Servicio servicio, Long idBarbero, LocalDate fecha, LocalTime hora) {
        if (disponibilidad.fueraDeRango(fecha)) {
            throw new ReglaNegocioException("Solo se puede reservar desde hoy hasta dentro de "
                    + props.turnos().diasAnticipacion() + " días");
        }
        return elegirBarberoLibre(servicio, idBarbero, fecha, hora)
                .orElseThrow(() -> new ConflictoException("Ese horario se acaba de ocupar. Elegí otro."));
    }

    private Turno guardarTurno(Servicio servicio, Barbero barbero, LocalDate fecha, LocalTime hora, Cliente cliente) {
        LocalDateTime ahora = calendario.ahora();
        Turno turno = new Turno();
        turno.setCliente(cliente);
        turno.setServicio(servicio);
        turno.setBarbero(barbero);
        turno.setFecha(fecha);
        turno.setHoraInicio(hora);
        turno.setHoraFin(hora.plusMinutes(servicio.getDuracionMinutos()));
        turno.setPrecio(servicio.getPrecio());
        turno.setEstado(EstadoTurno.PENDIENTE);
        turno.setTokenCancelacion(Tokens.nuevo());
        turno.setTokenVencimiento(ahora.plusHours(props.turnos().cancelacionHoras()));
        turno.setFechaCreacion(ahora);
        turnos.save(turno);

        eventos.publishEvent(new EventosTurno.TurnoReservado(datosEmail(turno), turno.getTokenCancelacion()));
        return turno;
    }

    /**
     * Bloquea (SELECT ... FOR UPDATE) a cada candidato antes de mirar su agenda:
     * si dos clientes piden el mismo horario a la vez, el segundo espera y ve el
     * turno del primero. Se bloquea en orden de id para no generar deadlocks.
     * Entre los libres gana el que tiene menos turnos ese día, para repartir el trabajo.
     */
    private Optional<Barbero> elegirBarberoLibre(Servicio servicio, Long idBarbero, LocalDate fecha, LocalTime hora) {
        List<Barbero> libres = new ArrayList<>();
        for (Barbero candidato : disponibilidad.candidatos(servicio, idBarbero)) {
            Barbero bloqueado = barberos.bloquearParaReservar(candidato.getId()).orElseThrow();
            if (disponibilidad.estaLibre(bloqueado, servicio, fecha, hora)) libres.add(bloqueado);
        }
        return libres.stream().min(Comparator.comparingInt(
                (Barbero b) -> turnos.delDia(b.getId(), fecha, EstadoTurno.CANCELADO).size()));
    }

    /**
     * El cliente no tiene cuenta: se lo reconoce por el email. Si ya existe, sus
     * datos no se tocan: la reserva es pública y cualquiera que conozca un email
     * podría cambiarle el nombre o el teléfono que ve la barbería.
     */
    private Cliente registrarCliente(ClientePedido datos) {
        return clientes.findByEmailIgnoreCase(datos.email().trim()).orElseGet(() -> {
            Cliente nuevo = new Cliente();
            nuevo.setEmail(datos.email().trim().toLowerCase());
            nuevo.setFechaAlta(calendario.ahora());
            nuevo.setNombre(datos.nombre().trim());
            nuevo.setApellido(datos.apellido().trim());
            nuevo.setTelefono(datos.telefono().trim());
            return clientes.save(nuevo);
        });
    }

    /**
     * Desde el panel el email es opcional. Con email se lo reconoce igual que en la reserva
     * pública; sin email, por el teléfono entre los clientes que tampoco tienen email.
     */
    private Cliente registrarClienteDelPanel(ClientePanelPedido datos) {
        String email = datos.email() == null || datos.email().isBlank() ? null : datos.email().trim().toLowerCase();
        String telefono = datos.telefono().trim();
        Optional<Cliente> existente = email != null
                ? clientes.findByEmailIgnoreCase(email)
                : clientes.findFirstByTelefonoAndEmailIsNullOrderByIdAsc(telefono);
        return existente.orElseGet(() -> {
            Cliente nuevo = new Cliente();
            nuevo.setEmail(email);
            nuevo.setFechaAlta(calendario.ahora());
            nuevo.setNombre(datos.nombre().trim());
            nuevo.setApellido(datos.apellido().trim());
            nuevo.setTelefono(telefono);
            return clientes.save(nuevo);
        });
    }

    // ------------------------------------------------------------------
    // Cancelación con el link del email (RF-06, RNF token de un solo uso)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Publico verPorToken(String token) {
        Turno t = porToken(token);
        return new Publico(t.getId(), t.getFecha(), t.getHoraInicio(), t.getHoraFin(), t.getServicio().getNombre(),
                t.getBarbero().getNombre(), t.getPrecio(), t.getCliente().getNombre(), t.getEstado(),
                esCancelable(t), cancelableHasta(t));
    }

    public Publico cancelarPorToken(String token) {
        Turno t = porToken(token);
        if (t.getEstado() != EstadoTurno.PENDIENTE) {
            throw new ConflictoException("Este turno ya está " + t.getEstado().valor());
        }
        if (!esCancelable(t)) {
            throw new ReglaNegocioException("El link de cancelación venció. Comunicate con la barbería.");
        }
        t.setEstado(EstadoTurno.CANCELADO);   // al dejar de estar pendiente, el token ya no sirve
        eventos.publishEvent(new EventosTurno.TurnoCancelado(datosEmail(t)));
        return verPorToken(token);
    }

    private Turno porToken(String token) {
        return turnos.porTokenCancelacion(token)
                .orElseThrow(() -> new NoEncontradoException("El link no es válido"));
    }

    private boolean esCancelable(Turno t) {
        LocalDateTime ahora = calendario.ahora();
        return t.getEstado() == EstadoTurno.PENDIENTE
                && ahora.isBefore(t.getTokenVencimiento())
                && ahora.isBefore(t.getFecha().atTime(t.getHoraInicio()));
    }

    private LocalDateTime cancelableHasta(Turno t) {
        LocalDateTime inicio = t.getFecha().atTime(t.getHoraInicio());
        return t.getTokenVencimiento().isBefore(inicio) ? t.getTokenVencimiento() : inicio;
    }

    // ------------------------------------------------------------------
    // Agenda del panel (RF-09, RF-10, RF-11, RF-14)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Detalle> listar(LocalDate desde, LocalDate hasta, Long idBarbero, EstadoTurno estado) {
        Calendario.validarRango(desde, hasta);
        List<Turno> lista = turnos.entre(desde, hasta, idBarbero).stream()
                .filter(t -> estado == null || t.getEstado() == estado)
                .toList();
        List<Detalle> resultado = detalles(lista);
        if (sesion.esDueno()) return resultado;
        // Un barbero ve la agenda de todo el equipo, pero de los turnos ajenos no ve datos del negocio.
        Long propio = sesion.idBarbero();
        return resultado.stream()
                .map(d -> d.barbero().id().equals(propio) ? d : d.sinDatosPrivados())
                .toList();
    }

    public Detalle cambiarEstado(Long idTurno, EstadoTurno nuevo) {
        Turno t = propioOdelDueno(idTurno);
        if (nuevo == EstadoTurno.PENDIENTE) {
            throw new ReglaNegocioException("Un turno no puede volver a pendiente");
        }
        if (t.getEstado().esFinal()) {
            throw new ConflictoException("El turno ya está " + t.getEstado().valor() + " y no se puede cambiar");
        }
        boolean yaEmpezo = !calendario.ahora().isBefore(t.getFecha().atTime(t.getHoraInicio()));
        if ((nuevo == EstadoTurno.COMPLETADO || nuevo == EstadoTurno.AUSENTE) && !yaEmpezo) {
            throw new ReglaNegocioException("Un turno se marca como " + nuevo.valor() + " recién cuando llega su horario");
        }

        t.setEstado(nuevo);
        switch (nuevo) {
            case COMPLETADO -> {
                t.setTokenEncuesta(Tokens.nuevo());
                eventos.publishEvent(new EventosTurno.TurnoCompletado(datosEmail(t), t.getId(), t.getTokenEncuesta()));
            }
            case CANCELADO -> eventos.publishEvent(new EventosTurno.TurnoCancelado(datosEmail(t)));
            default -> { }
        }
        return detalles(List.of(t)).getFirst();
    }

    // ------------------------------------------------------------------
    // [Extensión] Corregir o borrar un turno completado
    // ------------------------------------------------------------------

    /**
     * Arregla un turno completado que se cargó mal (otro servicio, otro precio, otro medio de pago).
     * El horario no se toca: ya pasó. Si el turno está cobrado, el monto del cobro pasa a ser el
     * precio nuevo, así la pantalla de Pagos y la liquidación quedan con lo corregido.
     */
    public Detalle editar(Long idTurno, EdicionPedido pedido) {
        Turno t = completado(idTurno);
        if (pedido.idBarbero() != null && !pedido.idBarbero().equals(t.getBarbero().getId())) {
            if (!sesion.esDueno()) {
                throw new AccessDeniedException("Solo el dueño puede pasar un turno a otro peluquero");
            }
            t.setBarbero(barberos.findById(pedido.idBarbero())
                    .orElseThrow(() -> NoEncontradoException.de("Peluquero", pedido.idBarbero())));
        }
        t.setServicio(servicios.findById(pedido.idServicio())
                .orElseThrow(() -> NoEncontradoException.de("Servicio", pedido.idServicio())));
        t.setPrecio(pedido.precio());

        pagos.findByTurnoId(t.getId()).ifPresent(p -> {
            if (pedido.medio() == null) throw new ReglaNegocioException("Falta el medio de pago del cobro");
            p.setMonto(pedido.precio());
            p.setMedio(pedido.medio());
        });
        return detalles(List.of(t)).getFirst();
    }

    /**
     * Borra un turno completado cargado por error, junto con su cobro y su encuesta:
     * si quedara, el cobro seguiría sumando en Pagos y las cuentas del mes no cerrarían.
     */
    public void borrar(Long idTurno) {
        Turno t = completado(idTurno);
        pagos.findByTurnoId(t.getId()).ifPresent(pagos::delete);
        encuestas.findByTurnoId(t.getId()).ifPresent(encuestas::delete);
        turnos.delete(t);
    }

    private Turno completado(Long idTurno) {
        Turno t = propioOdelDueno(idTurno);
        if (t.getEstado() != EstadoTurno.COMPLETADO) {
            throw new ReglaNegocioException("Solo se pueden corregir o borrar turnos completados");
        }
        return t;
    }

    /** El dueño toca cualquier turno; un barbero, solo los suyos. */
    private Turno propioOdelDueno(Long idTurno) {
        Turno t = turnos.conDetalle(idTurno).orElseThrow(() -> NoEncontradoException.de("Turno", idTurno));
        if (!sesion.esDueno() && !t.getBarbero().getId().equals(sesion.idBarbero())) {
            throw new AccessDeniedException("Solo el dueño puede modificar turnos de otro peluquero");
        }
        return t;
    }

    /** Arma los DTO trayendo pagos y encuestas de todos los turnos en dos consultas. */
    @Transactional(readOnly = true)
    public List<Detalle> detalles(List<Turno> lista) {
        if (lista.isEmpty()) return List.of();
        Set<Long> ids = lista.stream().map(Turno::getId).collect(Collectors.toSet());
        Map<Long, Pago> pagoPorTurno = pagos.findByTurnoIdIn(ids).stream()
                .collect(Collectors.toMap(p -> p.getTurno().getId(), Function.identity()));
        Map<Long, Encuesta> encuestaPorTurno = encuestas.findByTurnoIdIn(ids).stream()
                .collect(Collectors.toMap(e -> e.getTurno().getId(), Function.identity()));
        return lista.stream()
                .map(t -> Detalle.de(t, pagoPorTurno.get(t.getId()), encuestaPorTurno.get(t.getId())))
                .toList();
    }

    private static EventosTurno.DatosEmail datosEmail(Turno t) {
        return new EventosTurno.DatosEmail(t.getCliente().getEmail(), t.getCliente().getNombre(),
                t.getServicio().getNombre(), t.getBarbero().getNombre(), t.getFecha(), t.getHoraInicio(),
                t.getPrecio());
    }
}
