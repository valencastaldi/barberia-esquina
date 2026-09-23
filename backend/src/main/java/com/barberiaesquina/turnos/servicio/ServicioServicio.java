package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.modelo.Barbero;
import com.barberiaesquina.turnos.modelo.Servicio;
import com.barberiaesquina.turnos.repositorio.BarberoRepositorio;
import com.barberiaesquina.turnos.repositorio.ServicioRepositorio;
import com.barberiaesquina.turnos.repositorio.TurnoRepositorio;
import com.barberiaesquina.turnos.servicio.excepcion.ConflictoException;
import com.barberiaesquina.turnos.servicio.excepcion.NoEncontradoException;
import com.barberiaesquina.turnos.web.dto.ServicioDtos.Pedido;
import com.barberiaesquina.turnos.web.dto.ServicioDtos.Respuesta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Catálogo de servicios (RF-22, RF-23, RF-28, RF-29). */
@Service
@Transactional
public class ServicioServicio {

    private final ServicioRepositorio servicios;
    private final BarberoRepositorio barberos;
    private final TurnoRepositorio turnos;

    public ServicioServicio(ServicioRepositorio servicios, BarberoRepositorio barberos, TurnoRepositorio turnos) {
        this.servicios = servicios;
        this.barberos = barberos;
        this.turnos = turnos;
    }

    @Transactional(readOnly = true)
    public List<Respuesta> listar(boolean incluirOcultos) {
        var lista = incluirOcultos ? servicios.findAllByOrderByIdAsc() : servicios.findByActivoTrueOrderByIdAsc();
        return lista.stream().map(Respuesta::de).toList();
    }

    /** Un servicio nuevo lo hacen todos los peluqueros activos; después se ajusta en cada ficha. */
    public Respuesta crear(Pedido pedido) {
        Servicio s = new Servicio();
        aplicar(s, pedido);
        servicios.save(s);
        barberos.findByActivoTrueOrderByIdAsc().forEach(b -> b.getServicios().add(s));
        return Respuesta.de(s);
    }

    public Respuesta actualizar(Long id, Pedido pedido) {
        Servicio s = buscar(id);
        aplicar(s, pedido);
        return Respuesta.de(s);
    }

    public Respuesta cambiarEstado(Long id, boolean activo) {
        Servicio s = buscar(id);
        s.setActivo(activo);
        return Respuesta.de(s);
    }

    /** Solo se borra si nunca se usó; si tiene turnos se desactiva, así no se pierde el historial. */
    public void eliminar(Long id) {
        Servicio s = buscar(id);
        if (turnos.existsByServicioId(id)) {
            throw new ConflictoException("El servicio tiene turnos registrados: desactivalo en lugar de borrarlo");
        }
        for (Barbero b : barberos.findAllByOrderByIdAsc()) {
            b.getServicios().remove(s);
        }
        servicios.delete(s);
    }

    private Servicio buscar(Long id) {
        return servicios.findById(id).orElseThrow(() -> NoEncontradoException.de("Servicio", id));
    }

    private static void aplicar(Servicio s, Pedido p) {
        s.setNombre(p.nombre().trim());
        s.setDescripcion(p.descripcion() == null ? null : p.descripcion().trim());
        s.setDuracionMinutos(p.duracionMinutos());
        s.setPrecio(p.precio());
    }
}
