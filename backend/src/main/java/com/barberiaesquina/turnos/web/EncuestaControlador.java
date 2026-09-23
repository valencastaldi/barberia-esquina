package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.servicio.EncuestaServicio;
import com.barberiaesquina.turnos.web.dto.EncuestaDtos.Info;
import com.barberiaesquina.turnos.web.dto.EncuestaDtos.Pedido;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** M6 — Encuestas (público, con el token que llega por email). */
@RestController
@RequestMapping("/api/v1/encuestas/{idTurno}")
public class EncuestaControlador {

    private final EncuestaServicio encuestas;

    public EncuestaControlador(EncuestaServicio encuestas) {
        this.encuestas = encuestas;
    }

    @GetMapping
    public Info ver(@PathVariable Long idTurno, @RequestParam String token) {
        return encuestas.ver(idTurno, token);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Info responder(@PathVariable Long idTurno, @RequestParam String token,
                          @Valid @RequestBody Pedido pedido) {
        return encuestas.responder(idTurno, token, pedido);
    }
}
