package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.seguridad.SesionActual;
import com.barberiaesquina.turnos.servicio.HorarioServicio;
import com.barberiaesquina.turnos.web.dto.HorarioDtos.Dia;
import com.barberiaesquina.turnos.web.dto.HorarioDtos.Pedido;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** M3 — Horarios. */
@RestController
@RequestMapping("/api/v1/horarios")
public class HorarioControlador {

    private final HorarioServicio horarios;
    private final SesionActual sesion;

    public HorarioControlador(HorarioServicio horarios, SesionActual sesion) {
        this.horarios = horarios;
        this.sesion = sesion;
    }

    /** Público. Sin ?barbero devuelve el horario general de la barbería. */
    @GetMapping
    public List<Dia> ver(@RequestParam(required = false) Long barbero) {
        return barbero == null ? horarios.deLaBarberia() : horarios.deBarbero(barbero);
    }

    /** Sin ?barbero se guarda el horario de quien está logueado. */
    @PutMapping
    public List<Dia> guardar(@RequestParam(required = false) Long barbero, @Valid @RequestBody Pedido pedido) {
        return horarios.guardar(barbero != null ? barbero : sesion.idBarbero(), pedido.dias());
    }
}
