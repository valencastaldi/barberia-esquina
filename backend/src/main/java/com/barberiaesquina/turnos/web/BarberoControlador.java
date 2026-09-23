package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.servicio.BarberoServicio;
import com.barberiaesquina.turnos.web.dto.ActivoPedido;
import com.barberiaesquina.turnos.web.dto.BarberoDtos.Detalle;
import com.barberiaesquina.turnos.web.dto.BarberoDtos.Pedido;
import com.barberiaesquina.turnos.web.dto.BarberoDtos.Publico;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** [Extensión] Peluqueros. */
@RestController
@RequestMapping("/api/v1/barberos")
public class BarberoControlador {

    private final BarberoServicio barberos;

    public BarberoControlador(BarberoServicio barberos) {
        this.barberos = barberos;
    }

    /** Público: para que el cliente elija con quién atenderse. */
    @GetMapping
    public List<Publico> publicos() {
        return barberos.publicos();
    }

    @GetMapping("/equipo")
    public List<Detalle> equipo() {
        return barberos.equipo();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DUENO')")
    public Detalle crear(@Valid @RequestBody Pedido pedido) {
        return barberos.crear(pedido);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DUENO')")
    public Detalle actualizar(@PathVariable Long id, @Valid @RequestBody Pedido pedido) {
        return barberos.actualizar(id, pedido);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('DUENO')")
    public Detalle cambiarEstado(@PathVariable Long id, @Valid @RequestBody ActivoPedido pedido) {
        return barberos.cambiarEstado(id, pedido.activo());
    }
}
