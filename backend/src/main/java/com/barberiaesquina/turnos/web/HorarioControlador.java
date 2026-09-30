package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.seguridad.SesionActual;
import com.barberiaesquina.turnos.servicio.HorarioServicio;
import com.barberiaesquina.turnos.web.dto.HorarioDtos.Dia;
import com.barberiaesquina.turnos.web.dto.HorarioDtos.Pedido;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** M3 — Horarios. */
@RestController
@RequestMapping("/api/v1/horarios")
public class HorarioControlador {

    private final HorarioServicio horarios;
    private final SesionActual sesion;
    private final CachePublica cache;

    public HorarioControlador(HorarioServicio horarios, SesionActual sesion, CachePublica cache) {
        this.horarios = horarios;
        this.sesion = sesion;
        this.cache = cache;
    }

    /** Público. Sin ?barbero devuelve el horario general de la barbería. */
    @GetMapping
    public ResponseEntity<List<Dia>> ver(@RequestParam(required = false) Long barbero) {
        return cache.ok(barbero == null ? horarios.deLaBarberia() : horarios.deBarbero(barbero));
    }

    /** Los horarios los define el dueño. Sin ?barbero se guarda el suyo. */
    @PutMapping
    @PreAuthorize("hasRole('DUENO')")
    public List<Dia> guardar(@RequestParam(required = false) Long barbero, @Valid @RequestBody Pedido pedido) {
        return horarios.guardar(barbero != null ? barbero : sesion.idBarbero(), pedido.dias());
    }
}
