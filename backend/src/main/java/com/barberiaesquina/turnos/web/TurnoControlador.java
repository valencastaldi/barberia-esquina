package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.modelo.EstadoTurno;
import com.barberiaesquina.turnos.servicio.Calendario;
import com.barberiaesquina.turnos.servicio.TurnoServicio;
import com.barberiaesquina.turnos.web.dto.TurnoDtos.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** M5 — Turnos. */
@RestController
@RequestMapping("/api/v1/turnos")
public class TurnoControlador {

    private final TurnoServicio turnos;
    private final Calendario calendario;

    public TurnoControlador(TurnoServicio turnos, Calendario calendario) {
        this.turnos = turnos;
        this.calendario = calendario;
    }

    /** Público: el cliente reserva sin cuenta. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaRespuesta reservar(@Valid @RequestBody ReservaPedido pedido) {
        return turnos.reservar(pedido);
    }

    /** Agenda del panel. Sin fechas devuelve la de hoy. */
    @GetMapping
    public List<Detalle> listar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                @RequestParam(required = false) Long barbero,
                                @RequestParam(required = false) EstadoTurno estado) {
        LocalDate d = desde != null ? desde : calendario.hoy();
        LocalDate h = hasta != null ? hasta : d;
        return turnos.listar(d, h, barbero, estado);
    }

    @PatchMapping("/{id}/estado")
    public Detalle cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoPedido pedido) {
        return turnos.cambiarEstado(id, pedido.estado());
    }

    /** Público: datos del turno para la pantalla de cancelación (link del email). */
    @GetMapping("/cancelar/{token}")
    public Publico verParaCancelar(@PathVariable String token) {
        return turnos.verPorToken(token);
    }

    @PatchMapping("/cancelar/{token}")
    public Publico cancelar(@PathVariable String token) {
        return turnos.cancelarPorToken(token);
    }
}
