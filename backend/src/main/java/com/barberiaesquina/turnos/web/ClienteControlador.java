package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.servicio.ClienteServicio;
import com.barberiaesquina.turnos.servicio.ClienteServicio.Filtro;
import com.barberiaesquina.turnos.web.dto.ClienteDtos.Ficha;
import com.barberiaesquina.turnos.web.dto.ClienteDtos.Pagina;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** [Extensión] Clientes. Datos del negocio: solo el dueño. */
@RestController
@PreAuthorize("hasRole('DUENO')")
@RequestMapping("/api/v1/clientes")
public class ClienteControlador {

    private final ClienteServicio clientes;

    public ClienteControlador(ClienteServicio clientes) {
        this.clientes = clientes;
    }

    /** GET /clientes?q=mateo&filtro=frecuentes&pagina=0&tamano=25 */
    @GetMapping
    public Pagina listar(@RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "todos") Filtro filtro,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "25") int tamano) {
        return clientes.listar(q, filtro, pagina, tamano);
    }

    @GetMapping("/{id}")
    public Ficha ficha(@PathVariable Long id) {
        return clientes.ficha(id);
    }
}
