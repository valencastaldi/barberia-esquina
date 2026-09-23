package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.servicio.DisponibilidadServicio;
import com.barberiaesquina.turnos.web.dto.DisponibilidadDtos.Dia;
import com.barberiaesquina.turnos.web.dto.DisponibilidadDtos.ResumenDia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** M4 — Disponibilidad (público). */
@RestController
@RequestMapping("/api/v1/disponibilidad")
public class DisponibilidadControlador {

    private final DisponibilidadServicio disponibilidad;

    public DisponibilidadControlador(DisponibilidadServicio disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    /** GET /disponibilidad?servicio=1&fecha=2026-09-25[&barbero=2] */
    @GetMapping
    public Dia delDia(@RequestParam Long servicio,
                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                      @RequestParam(required = false) Long barbero) {
        return disponibilidad.delDia(fecha, servicio, barbero);
    }

    /** GET /disponibilidad/dias?servicio=1[&barbero=2][&cantidad=14]: para el selector de días. */
    @GetMapping("/dias")
    public List<ResumenDia> proximosDias(@RequestParam Long servicio,
                                         @RequestParam(required = false) Long barbero,
                                         @RequestParam(defaultValue = "14") @Min(1) @Max(31) int cantidad) {
        return disponibilidad.proximosDias(servicio, barbero, cantidad);
    }
}
