package com.barberiaesquina.turnos.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class DisponibilidadDtos {

    private DisponibilidadDtos() {}

    /**
     * Un horario posible. Los ocupados también vienen (libre = false) porque la
     * pantalla del cliente los muestra tachados. barberos = quiénes están libres.
     */
    public record Slot(@JsonFormat(pattern = "HH:mm") LocalTime hora, boolean libre, List<Long> barberos) {}

    public record Dia(LocalDate fecha, Long idServicio, Integer duracionMinutos, List<Slot> slots) {}

    /**
     * Para el selector de días: cuántos horarios libres tiene cada día. El primero con lugar
     * trae además sus horarios (slots), así la pantalla los muestra sin hacer otro pedido.
     */
    public record ResumenDia(LocalDate fecha, boolean atiende, int libres,
                             @JsonInclude(JsonInclude.Include.NON_NULL) List<Slot> slots) {}
}
