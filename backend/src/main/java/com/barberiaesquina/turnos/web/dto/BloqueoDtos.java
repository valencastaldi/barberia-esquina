package com.barberiaesquina.turnos.web.dto;

import com.barberiaesquina.turnos.modelo.Bloqueo;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public final class BloqueoDtos {

    private BloqueoDtos() {}

    /** idBarbero es opcional: si no viene, se bloquea la agenda de quien está logueado. */
    public record Pedido(
            Long idBarbero,
            @NotNull LocalDate fecha,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
            @Size(max = 120) String motivo
    ) {}

    public record Respuesta(Long id, Long idBarbero, String barbero, LocalDate fecha,
                            @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
                            @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
                            String motivo) {
        public static Respuesta de(Bloqueo b) {
            return new Respuesta(b.getId(), b.getBarbero().getId(), b.getBarbero().getNombre(), b.getFecha(),
                    b.getHoraInicio(), b.getHoraFin(), b.getMotivo());
        }
    }
}
