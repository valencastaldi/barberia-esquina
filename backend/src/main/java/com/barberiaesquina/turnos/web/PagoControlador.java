package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.servicio.Calendario;
import com.barberiaesquina.turnos.servicio.PagoServicio;
import com.barberiaesquina.turnos.web.dto.PagoDtos.Pedido;
import com.barberiaesquina.turnos.web.dto.PagoDtos.Reporte;
import com.barberiaesquina.turnos.web.dto.PagoDtos.Respuesta;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** [Extensión] Pagos. Cualquier peluquero registra cobros; el reporte con la liquidación es del dueño. */
@RestController
@RequestMapping("/api/v1/pagos")
public class PagoControlador {

    private final PagoServicio pagos;
    private final Calendario calendario;

    public PagoControlador(PagoServicio pagos, Calendario calendario) {
        this.pagos = pagos;
        this.calendario = calendario;
    }

    /** Sin fechas devuelve el día de hoy. */
    @GetMapping
    @PreAuthorize("hasRole('DUENO')")
    public Reporte reporte(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate d = desde != null ? desde : calendario.hoy();
        return pagos.reporte(d, hasta != null ? hasta : calendario.hoy());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Respuesta registrar(@Valid @RequestBody Pedido pedido) {
        return pagos.registrar(pedido);
    }
}
