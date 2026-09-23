package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.modelo.*;
import com.barberiaesquina.turnos.repositorio.BarberoRepositorio;
import com.barberiaesquina.turnos.repositorio.PagoRepositorio;
import com.barberiaesquina.turnos.repositorio.TurnoRepositorio;
import com.barberiaesquina.turnos.servicio.excepcion.ConflictoException;
import com.barberiaesquina.turnos.servicio.excepcion.NoEncontradoException;
import com.barberiaesquina.turnos.servicio.excepcion.ReglaNegocioException;
import com.barberiaesquina.turnos.web.dto.PagoDtos.*;
import com.barberiaesquina.turnos.web.dto.TurnoDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** [Extensión] Cobros de turnos completados y liquidación por peluquero. */
@Service
@Transactional
public class PagoServicio {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final PagoRepositorio pagos;
    private final TurnoRepositorio turnos;
    private final BarberoRepositorio barberos;
    private final TurnoServicio turnoServicio;
    private final Calendario calendario;

    public PagoServicio(PagoRepositorio pagos, TurnoRepositorio turnos, BarberoRepositorio barberos,
                        TurnoServicio turnoServicio, Calendario calendario) {
        this.pagos = pagos;
        this.turnos = turnos;
        this.barberos = barberos;
        this.turnoServicio = turnoServicio;
        this.calendario = calendario;
    }

    public Respuesta registrar(Pedido pedido) {
        Turno turno = turnos.findById(pedido.idTurno())
                .orElseThrow(() -> NoEncontradoException.de("Turno", pedido.idTurno()));
        if (turno.getEstado() != EstadoTurno.COMPLETADO) {
            throw new ReglaNegocioException("Solo se cobran turnos completados");
        }
        if (pagos.existsByTurnoId(turno.getId())) {
            throw new ConflictoException("Ese turno ya está cobrado");
        }
        Pago p = new Pago();
        p.setTurno(turno);
        p.setMonto(pedido.monto());
        p.setMedio(pedido.medio());
        p.setFecha(calendario.ahora());
        pagos.save(p);
        return new Respuesta(p.getId(), turno.getId(), p.getMonto(), p.getMedio(), p.getFecha());
    }

    /**
     * Lo que muestra la pantalla de Pagos. El período se toma por la fecha del
     * turno. Los completados sin cobrar van primero en los movimientos.
     */
    @Transactional(readOnly = true)
    public Reporte reporte(LocalDate desde, LocalDate hasta) {
        List<Turno> completados = turnos.entre(desde, hasta, null).stream()
                .filter(t -> t.getEstado() == EstadoTurno.COMPLETADO)
                .toList();
        Map<Long, Pago> pagoPorTurno = completados.isEmpty() ? Map.of()
                : pagos.findByTurnoIdIn(completados.stream().map(Turno::getId).toList()).stream()
                        .collect(Collectors.toMap(p -> p.getTurno().getId(), Function.identity()));

        List<Turno> cobrados = completados.stream().filter(t -> pagoPorTurno.containsKey(t.getId())).toList();
        List<Turno> sinCobrar = completados.stream().filter(t -> !pagoPorTurno.containsKey(t.getId())).toList();

        BigDecimal cobrado = suma(cobrados.stream().map(t -> pagoPorTurno.get(t.getId()).getMonto()));
        BigDecimal paraLaCasa = suma(cobrados.stream().map(t -> {
            BigDecimal monto = pagoPorTurno.get(t.getId()).getMonto();
            return monto.subtract(comision(monto, t.getBarbero()));
        }));
        BigDecimal ticket = cobrados.isEmpty() ? BigDecimal.ZERO
                : cobrado.divide(BigDecimal.valueOf(cobrados.size()), 0, RoundingMode.HALF_UP);

        var resumen = new Resumen(cobrado, cobrados.size(), ticket, paraLaCasa,
                suma(sinCobrar.stream().map(Turno::getPrecio)), sinCobrar.size());

        List<PorMedio> porMedio = Arrays.stream(MedioPago.values()).map(medio -> {
            BigDecimal monto = suma(pagoPorTurno.values().stream().filter(p -> p.getMedio() == medio).map(Pago::getMonto));
            int pct = cobrado.signum() == 0 ? 0 : monto.multiply(CIEN).divide(cobrado, 0, RoundingMode.HALF_UP).intValue();
            return new PorMedio(medio, monto, pct);
        }).toList();

        // Un peluquero dado de baja aparece solo si facturó algo en el período.
        List<Liquidacion> liquidacion = new ArrayList<>();
        for (Barbero b : barberos.findAllByOrderByIdAsc()) {
            BigDecimal facturado = suma(cobrados.stream()
                    .filter(t -> t.getBarbero().getId().equals(b.getId()))
                    .map(t -> pagoPorTurno.get(t.getId()).getMonto()));
            if (b.isActivo() || facturado.signum() > 0) {
                liquidacion.add(new Liquidacion(b.getId(), b.getNombre(), b.getRol(), b.getComisionPct(),
                        facturado, comision(facturado, b)));
            }
        }

        // Movimientos: sin cobrar primero; después lo más reciente arriba.
        Comparator<Turno> recientes = Comparator.comparing(Turno::getFecha).thenComparing(Turno::getHoraInicio).reversed();
        List<Turno> orden = new ArrayList<>(sinCobrar.stream().sorted(recientes).toList());
        orden.addAll(cobrados.stream().sorted(recientes).toList());
        List<TurnoDtos.Detalle> movimientos = turnoServicio.detalles(orden);

        return new Reporte(resumen, porMedio, liquidacion, movimientos);
    }

    private static BigDecimal comision(BigDecimal monto, Barbero b) {
        return monto.multiply(BigDecimal.valueOf(b.getComisionPct())).divide(CIEN, 0, RoundingMode.HALF_UP);
    }

    private static BigDecimal suma(java.util.stream.Stream<BigDecimal> montos) {
        return montos.reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
