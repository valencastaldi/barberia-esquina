package com.barberiaesquina.turnos.web.dto;

import com.barberiaesquina.turnos.modelo.MedioPago;
import com.barberiaesquina.turnos.modelo.Rol;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PagoDtos {

    private PagoDtos() {}

    public record Pedido(
            @NotNull Long idTurno,
            @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal monto,
            @NotNull MedioPago medio
    ) {}

    public record Respuesta(Long id, Long idTurno, BigDecimal monto, MedioPago medio, LocalDateTime fecha) {}

    /** Todo lo que muestra la pantalla de Pagos para un período. */
    public record Reporte(Resumen resumen, List<PorMedio> porMedio, List<Liquidacion> liquidacion,
                          List<TurnoDtos.Detalle> movimientos) {}

    public record Resumen(BigDecimal cobrado, int cobros, BigDecimal ticketPromedio, BigDecimal paraLaCasa,
                          BigDecimal sinCobrarMonto, int sinCobrarCantidad) {}

    public record PorMedio(MedioPago medio, BigDecimal monto, int porcentaje) {}

    public record Liquidacion(Long idBarbero, String nombre, Rol rol, Integer comisionPct,
                              BigDecimal facturado, BigDecimal leToca) {}
}
