package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.seguridad.SesionActual;
import com.barberiaesquina.turnos.servicio.ServicioServicio;
import com.barberiaesquina.turnos.web.dto.ActivoPedido;
import com.barberiaesquina.turnos.web.dto.ServicioDtos.Pedido;
import com.barberiaesquina.turnos.web.dto.ServicioDtos.Respuesta;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** M2 — Servicios. */
@RestController
@RequestMapping("/api/v1/servicios")
public class ServicioControlador {

    private final ServicioServicio servicios;
    private final SesionActual sesion;

    public ServicioControlador(ServicioServicio servicios, SesionActual sesion) {
        this.servicios = servicios;
        this.sesion = sesion;
    }

    /** Público: el cliente ve solo los activos. Con ?todos=true el panel ve también los ocultos. */
    @GetMapping
    public List<Respuesta> listar(@RequestParam(defaultValue = "false") boolean todos) {
        return servicios.listar(todos && sesion.estaAutenticado());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DUENO')")
    public Respuesta crear(@Valid @RequestBody Pedido pedido) {
        return servicios.crear(pedido);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DUENO')")
    public Respuesta actualizar(@PathVariable Long id, @Valid @RequestBody Pedido pedido) {
        return servicios.actualizar(id, pedido);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('DUENO')")
    public Respuesta cambiarEstado(@PathVariable Long id, @Valid @RequestBody ActivoPedido pedido) {
        return servicios.cambiarEstado(id, pedido.activo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('DUENO')")
    public void eliminar(@PathVariable Long id) {
        servicios.eliminar(id);
    }
}
