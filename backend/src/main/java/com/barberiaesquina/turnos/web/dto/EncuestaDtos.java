package com.barberiaesquina.turnos.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class EncuestaDtos {

    private EncuestaDtos() {}

    public record Info(Long idTurno, LocalDate fecha, String servicio, String barbero, String cliente,
                       boolean respondida, Integer calificacion, String comentario) {}

    public record Pedido(@NotNull @Min(1) @Max(5) Integer calificacion, @Size(max = 500) String comentario) {}
}
