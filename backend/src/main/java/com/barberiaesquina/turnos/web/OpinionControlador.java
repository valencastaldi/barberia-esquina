package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.servicio.Calendario;
import com.barberiaesquina.turnos.servicio.OpinionServicio;
import com.barberiaesquina.turnos.web.dto.OpinionDtos.Reporte;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Opiniones de los clientes: las ve todo el equipo (dueño y barberos). Sin fechas, los últimos 90 días. */
@RestController
@RequestMapping("/api/v1/opiniones")
public class OpinionControlador {

    private final OpinionServicio opiniones;
    private final Calendario calendario;

    public OpinionControlador(OpinionServicio opiniones, Calendario calendario) {
        this.opiniones = opiniones;
        this.calendario = calendario;
    }

    @GetMapping
    public Reporte reporte(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                           @RequestParam(required = false) Long barbero) {
        LocalDate h = hasta != null ? hasta : calendario.hoy();
        return opiniones.reporte(desde != null ? desde : h.minusDays(89), h, barbero);
    }
}
