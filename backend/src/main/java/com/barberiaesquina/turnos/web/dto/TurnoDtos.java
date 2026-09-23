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

    /** idBarbero es opcional: sin él se asigna cualquier peluquero libre. */
    public record ReservaPedido(
            @NotNull Long idServicio,
            Long idBarbero,
            @NotNull LocalDate fecha,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime hora,
            @NotNull @Valid ClientePedido cliente
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

    public record CambioEstadoPedido(@NotNull EstadoTurno estado) {}

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
    }
}
