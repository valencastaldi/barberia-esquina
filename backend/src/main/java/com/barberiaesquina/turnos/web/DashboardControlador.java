package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.servicio.Calendario;
import com.barberiaesquina.turnos.servicio.DashboardServicio;
import com.barberiaesquina.turnos.web.dto.DashboardDtos.PuntoEvolucion;
import com.barberiaesquina.turnos.web.dto.DashboardDtos.RendimientoBarbero;
import com.barberiaesquina.turnos.web.dto.DashboardDtos.Resumen;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** M7 — Dashboard. Sin fechas, toma los últimos 30 días. */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardControlador {

    private final DashboardServicio dashboard;
    private final Calendario calendario;

    public DashboardControlador(DashboardServicio dashboard, Calendario calendario) {
        this.dashboard = dashboard;
        this.calendario = calendario;
    }

    @GetMapping("/resumen")
    public Resumen resumen(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                           @RequestParam(required = false) Long barbero) {
        return dashboard.resumen(desde(desde), hasta(hasta), barbero);
    }

    @GetMapping("/evolucion")
    public List<PuntoEvolucion> evolucion(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                          @RequestParam(required = false) Long barbero) {
        return dashboard.evolucion(desde(desde), hasta(hasta), barbero);
    }

    /** [Extensión] */
    @GetMapping("/barberos")
    public List<RendimientoBarbero> porBarbero(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return dashboard.porBarbero(desde(desde), hasta(hasta));
    }

    private LocalDate desde(LocalDate desde) {
        return desde != null ? desde : calendario.hoy().minusDays(29);
    }

    private LocalDate hasta(LocalDate hasta) {
        return hasta != null ? hasta : calendario.hoy();
    }
}
