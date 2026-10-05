package com.barberiaesquina.turnos.web.dto;

import com.barberiaesquina.turnos.modelo.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class TurnoDtos {

    private TurnoDtos() {}

    // ---------- Reserva (cliente, sin login) ----------

    public record ClientePedido(
            @NotBlank @Size(max = 60) String nombre,
            @NotBlank @Size(max = 60) String apellido,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Pattern(regexp = "[0-9 +()-]{8,30}", message = "teléfono inválido") String telefono
    ) {}

    /**
     * idBarbero es opcional: sin él se asigna cualquier peluquero libre.
     * El cliente tiene que aceptar los términos y la política de privacidad para reservar.
     */
    public record ReservaPedido(
            @NotNull Long idServicio,
            Long idBarbero,
            @NotNull LocalDate fecha,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime hora,
            @NotNull @Valid ClientePedido cliente,
            @NotNull(message = "Tenés que aceptar los términos y condiciones")
            @AssertTrue(message = "Tenés que aceptar los términos y condiciones") Boolean aceptaTerminos
    ) {}

    public record ReservaRespuesta(
            Long idTurno, LocalDate fecha,
            @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
            @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
            String servicio, String barbero, BigDecimal precio,
            String tokenCancelacion, LocalDateTime cancelableHasta
    ) {}

    /** Lo que muestra la pantalla de cancelación a partir del token del email. */
    public record Publico(
            Long idTurno, LocalDate fecha,
            @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
            @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
            String servicio, String barbero, BigDecimal precio, String cliente,
            EstadoTurno estado, boolean cancelable, LocalDateTime cancelableHasta
    ) {}

    // ---------- Panel ----------

    /** [Extensión] Como ClientePedido, pero el email es opcional: el turno puede ser por teléfono o sin reserva. */
    public record ClientePanelPedido(
            @NotBlank @Size(max = 60) String nombre,
            @NotBlank @Size(max = 60) String apellido,
            @Email @Size(max = 120) String email,
            @NotBlank @Pattern(regexp = "[0-9 +()-]{8,30}", message = "teléfono inválido") String telefono
    ) {}

    /** [Extensión] Turno cargado desde el panel. Un barbero solo puede cargarse turnos a sí mismo. */
    public record TurnoPanelPedido(
            @NotNull Long idServicio,
            Long idBarbero,
            @NotNull LocalDate fecha,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime hora,
            @NotNull @Valid ClientePanelPedido cliente
    ) {}

    public record CambioEstadoPedido(@NotNull EstadoTurno estado) {}

    /**
     * [Extensión] Corrección de un turno completado: el servicio que se hizo, cuánto salió y,
     * si ya se cobró, con qué medio. El peluquero solo lo cambia el dueño (idBarbero null = no se toca).
     */
    public record EdicionPedido(
            @NotNull Long idServicio,
            Long idBarbero,
            @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal precio,
            MedioPago medio
    ) {}

    public record Ref(Long id, String nombre) {}

    public record ClienteRef(Long id, String nombre, String apellido, String telefono, String email) {}

    public record PagoRef(MedioPago medio, BigDecimal monto, LocalDateTime fecha) {}

    public record Detalle(
            Long id, LocalDate fecha,
            @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
            @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
            EstadoTurno estado, BigDecimal precio,
            ClienteRef cliente, Ref servicio, Ref barbero,
            PagoRef pago, Integer calificacion
    ) {
        public static Detalle de(Turno t, Pago pago, Encuesta encuesta) {
            Cliente c = t.getCliente();
            return new Detalle(t.getId(), t.getFecha(), t.getHoraInicio(), t.getHoraFin(), t.getEstado(), t.getPrecio(),
                    new ClienteRef(c.getId(), c.getNombre(), c.getApellido(), c.getTelefono(), c.getEmail()),
                    new Ref(t.getServicio().getId(), t.getServicio().getNombre()),
                    new Ref(t.getBarbero().getId(), t.getBarbero().getNombre()),
                    pago == null ? null : new PagoRef(pago.getMedio(), pago.getMonto(), pago.getFecha()),
                    encuesta == null ? null : encuesta.getCalificacion());
        }

        /**
         * Lo que ve un barbero de un turno de otro peluquero: quién, qué y cuándo,
         * sin el contacto del cliente, el cobro ni la calificación.
         */
        public Detalle sinDatosPrivados() {
            return new Detalle(id, fecha, horaInicio, horaFin, estado, precio,
                    new ClienteRef(cliente.id(), cliente.nombre(), cliente.apellido(), null, null),
                    servicio, barbero, null, null);
        }
    }
}
