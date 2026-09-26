package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.modelo.Bloqueo;
import com.barberiaesquina.turnos.modelo.EstadoTurno;
import com.barberiaesquina.turnos.repositorio.BarberoRepositorio;
import com.barberiaesquina.turnos.repositorio.BloqueoRepositorio;
import com.barberiaesquina.turnos.repositorio.TurnoRepositorio;
import com.barberiaesquina.turnos.seguridad.SesionActual;
import com.barberiaesquina.turnos.servicio.excepcion.ConflictoException;
import com.barberiaesquina.turnos.servicio.excepcion.NoEncontradoException;
import com.barberiaesquina.turnos.servicio.excepcion.ReglaNegocioException;
import com.barberiaesquina.turnos.web.dto.BloqueoDtos.Pedido;
import com.barberiaesquina.turnos.web.dto.BloqueoDtos.Respuesta;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** RF-12: bloquear franjas horarias. */
@Service
@Transactional
public class BloqueoServicio {

    private final BloqueoRepositorio bloqueos;
    private final BarberoRepositorio barberos;
    private final TurnoRepositorio turnos;
    private final SesionActual sesion;
    private final Calendario calendario;

    public BloqueoServicio(BloqueoRepositorio bloqueos, BarberoRepositorio barberos, TurnoRepositorio turnos,
                           SesionActual sesion, Calendario calendario) {
        this.bloqueos = bloqueos;
        this.barberos = barberos;
        this.turnos = turnos;
        this.sesion = sesion;
        this.calendario = calendario;
    }

    @Transactional(readOnly = true)
    public List<Respuesta> listar(LocalDate desde, LocalDate hasta, Long idBarbero) {
        Calendario.validarRango(desde, hasta);
        return bloqueos.entre(desde, hasta, idBarbero).stream().map(Respuesta::de).toList();
    }

    /** No se puede bloquear encima de turnos pendientes: primero hay que cancelarlos (y avisarles). */
    public Respuesta crear(Pedido p) {
        Long idBarbero = p.idBarbero() != null ? p.idBarbero() : sesion.idBarbero();
        exigirPermiso(idBarbero);
        if (!p.horaInicio().isBefore(p.horaFin())) {
            throw new ReglaNegocioException("La hora de inicio tiene que ser anterior a la de fin");
        }
        if (p.fecha().isBefore(calendario.hoy())) throw new ReglaNegocioException("No se puede bloquear un día pasado");

        long pisados = turnos.delDia(idBarbero, p.fecha(), EstadoTurno.CANCELADO).stream()
                .filter(t -> t.getEstado() == EstadoTurno.PENDIENTE && t.ocupa(p.horaInicio(), p.horaFin()))
                .count();
        if (pisados > 0) {
            throw new ConflictoException("Hay " + pisados + " turno(s) pendiente(s) en esa franja. Cancelalos antes de bloquearla.");
        }

        Bloqueo b = new Bloqueo();
        b.setBarbero(barberos.findById(idBarbero).orElseThrow(() -> NoEncontradoException.de("Peluquero", idBarbero)));
        b.setFecha(p.fecha());
        b.setHoraInicio(p.horaInicio());
        b.setHoraFin(p.horaFin());
        b.setMotivo(p.motivo());
        return Respuesta.de(bloqueos.save(b));
    }

    public void eliminar(Long id) {
        Bloqueo b = bloqueos.findById(id).orElseThrow(() -> NoEncontradoException.de("Bloqueo", id));
        exigirPermiso(b.getBarbero().getId());
        bloqueos.delete(b);
    }

    private void exigirPermiso(Long idBarbero) {
        if (!sesion.esDueno() && !idBarbero.equals(sesion.idBarbero())) {
            throw new AccessDeniedException("Solo el dueño puede bloquear la agenda de otro peluquero");
        }
    }
}
