package com.barberiaesquina.turnos.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Opiniones de las encuestas, para que todo el equipo vea cómo lo calificaron. */
public final class OpinionDtos {

    private OpinionDtos() {}

    /** El cliente va con nombre corto ("Mateo G."): no hace falta más para leer una opinión. */
    public record Opinion(Long idTurno, LocalDate fecha, String servicio, TurnoDtos.Ref barbero, String cliente,
                          int calificacion, String comentario, LocalDateTime fechaRespuesta) {}

    public record PorBarbero(Long idBarbero, String nombre, Double promedio, long cantidad,
                             Map<Integer, Long> distribucion) {}

    public record Reporte(Double promedio, long cantidad, List<PorBarbero> porBarbero, List<Opinion> opiniones) {}
}
