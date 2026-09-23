package com.barberiaesquina.turnos.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public final class HorarioDtos {

    private HorarioDtos() {}

    /** Un día de la semana. diaSemana: 0 = domingo ... 6 = sábado. */
    public record Dia(
            @NotNull @Min(0) @Max(6) Integer diaSemana,
            @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
            @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
            @NotNull @Min(5) @Max(120) Integer duracionSlotMin,
            boolean activo
    ) {}

    public record Pedido(@NotEmpty @Valid List<Dia> dias) {}
}
