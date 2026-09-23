package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.servicio.BloqueoServicio;
import com.barberiaesquina.turnos.servicio.Calendario;
import com.barberiaesquina.turnos.web.dto.BloqueoDtos.Pedido;
import com.barberiaesquina.turnos.web.dto.BloqueoDtos.Respuesta;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** RF-12 — Bloqueo de franjas horarias. */
@RestController
@RequestMapping("/api/v1/bloqueos")
public class BloqueoControlador {

    private final BloqueoServicio bloqueos;
    private final Calendario calendario;

    public BloqueoControlador(BloqueoServicio bloqueos, Calendario calendario) {
        this.bloqueos = bloqueos;
        this.calendario = calendario;
    }

    /** Sin fechas: de hoy a 30 días. */
    @GetMapping
    public List<Respuesta> listar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                  @RequestParam(required = false) Long barbero) {
        LocalDate d = desde != null ? desde : calendario.hoy();
        return bloqueos.listar(d, hasta != null ? hasta : d.plusDays(30), barbero);
    }

    /** Bloquear o liberar franjas cambia la disponibilidad: lo hace el dueño. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DUENO')")
    public Respuesta crear(@Valid @RequestBody Pedido pedido) {
        return bloqueos.crear(pedido);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('DUENO')")
    public void eliminar(@PathVariable Long id) {
        bloqueos.eliminar(id);
    }
}
