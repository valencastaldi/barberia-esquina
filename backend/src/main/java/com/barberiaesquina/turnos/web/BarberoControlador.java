package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.servicio.BarberoServicio;
import com.barberiaesquina.turnos.servicio.FotoBarberoServicio;
import com.barberiaesquina.turnos.web.dto.ActivoPedido;
import com.barberiaesquina.turnos.web.dto.BarberoDtos.Detalle;
import com.barberiaesquina.turnos.web.dto.BarberoDtos.Pedido;
import com.barberiaesquina.turnos.web.dto.BarberoDtos.Publico;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** [Extensión] Peluqueros. */
@RestController
@RequestMapping("/api/v1/barberos")
public class BarberoControlador {

    private final BarberoServicio barberos;
    private final FotoBarberoServicio fotos;
    private final CachePublica cache;

    public BarberoControlador(BarberoServicio barberos, FotoBarberoServicio fotos, CachePublica cache) {
        this.barberos = barberos;
        this.fotos = fotos;
        this.cache = cache;
    }

    /** Público: para que el cliente elija con quién atenderse. */
    @GetMapping
    public ResponseEntity<List<Publico>> publicos() {
        return cache.ok(barberos.publicos());
    }

    /** Ficha completa del equipo (email, comisión…): solo el dueño. */
    @GetMapping("/equipo")
    @PreAuthorize("hasRole('DUENO')")
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

    // ---------- Foto de perfil ----------

    /**
     * Público: la muestra el sitio del cliente. La URL lleva ?v=… con la fecha de la foto,
     * así que se puede guardar en caché por mucho tiempo: si cambia, cambia la URL.
     */
    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable Long id) {
        var imagen = fotos.ver(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.tipo()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(imagen.contenido());
    }

    /** Multipart con el campo "archivo" (JPG, PNG o WEBP). Devuelve la URL nueva. */
    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DUENO')")
    public Map<String, String> subirFoto(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        return Map.of("foto", fotos.guardar(id, archivo.getBytes()));
    }

    @DeleteMapping("/{id}/foto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('DUENO')")
    public void quitarFoto(@PathVariable Long id) {
        fotos.quitar(id);
    }
}
