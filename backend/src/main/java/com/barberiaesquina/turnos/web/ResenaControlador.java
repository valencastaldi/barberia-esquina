package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.servicio.Calendario;
import com.barberiaesquina.turnos.servicio.DashboardServicio;
import com.barberiaesquina.turnos.web.dto.DashboardDtos.Resenas;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Público: la satisfacción promedio y algunos comentarios para la home del cliente. */
@RestController
@RequestMapping("/api/v1/resenas")
public class ResenaControlador {

    private final DashboardServicio dashboard;
    private final Calendario calendario;

    public ResenaControlador(DashboardServicio dashboard, Calendario calendario) {
        this.dashboard = dashboard;
        this.calendario = calendario;
    }

    @GetMapping
    public Resenas resenas() {
        return dashboard.resenas(calendario.hoy());
    }
}
