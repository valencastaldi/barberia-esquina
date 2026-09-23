package com.barberiaesquina.turnos.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Eventos que publica TurnoServicio. Llevan datos planos (no entidades) porque
 * se procesan en otro hilo, después de que la transacción ya cerró.
 */
public final class EventosTurno {

    private EventosTurno() {}

    public record DatosEmail(String email, String nombreCliente, String servicio, String barbero,
                             LocalDate fecha, LocalTime hora, BigDecimal precio) {}

    /** RF-04: confirmación con el link único de cancelación. */
    public record TurnoReservado(DatosEmail datos, String tokenCancelacion) {}

    /** RF-14: al completar el turno se envía la encuesta. */
    public record TurnoCompletado(DatosEmail datos, Long idTurno, String tokenEncuesta) {}

    /** Aviso de que el turno quedó cancelado (por el cliente o por la barbería). */
    public record TurnoCancelado(DatosEmail datos) {}
}
