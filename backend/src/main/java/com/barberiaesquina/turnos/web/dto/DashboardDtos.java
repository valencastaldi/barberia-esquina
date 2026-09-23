package com.barberiaesquina.turnos.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {

    private DashboardDtos() {}

    /**
     * RF-18 a RF-21. tasaAusentismo = ausentes / (completados + ausentes):
     * solo cuenta los turnos que tendrían que haberse atendido.
     */
    public record Resumen(long turnos, long pendientes, long completados, long ausentes, long cancelados,
                          double tasaAusentismo, BigDecimal facturacion, Double satisfaccion, long encuestas,
                          Map<Integer, Long> distribucionEstrellas, List<Comentario> ultimosComentarios) {}

    public record Comentario(String cliente, int calificacion, String comentario, LocalDateTime fecha) {}

    /** Lo que muestra la home del cliente: solo números agregados y comentarios buenos, con nombre corto. */
    public record Resenas(Double promedio, long encuestas, long atendidosMes, List<Comentario> comentarios) {}

    public record PuntoEvolucion(LocalDate fecha, long turnos, long completados) {}

    /** [Extensión] Rendimiento de cada peluquero en el período. */
    public record RendimientoBarbero(Long idBarbero, String nombre, long completados, long ausentes,
                                     BigDecimal facturado, Double satisfaccion) {}
}
