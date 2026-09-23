package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.modelo.Encuesta;
import com.barberiaesquina.turnos.modelo.EstadoTurno;
import com.barberiaesquina.turnos.modelo.Turno;
import com.barberiaesquina.turnos.repositorio.EncuestaRepositorio;
import com.barberiaesquina.turnos.repositorio.TurnoRepositorio;
import com.barberiaesquina.turnos.servicio.excepcion.ConflictoException;
import com.barberiaesquina.turnos.servicio.excepcion.NoEncontradoException;
import com.barberiaesquina.turnos.web.dto.EncuestaDtos.Info;
import com.barberiaesquina.turnos.web.dto.EncuestaDtos.Pedido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Encuesta de satisfacción (RF-15, RF-16, RF-17). */
@Service
@Transactional
public class EncuestaServicio {

    private final TurnoRepositorio turnos;
    private final EncuestaRepositorio encuestas;
    private final Calendario calendario;

    public EncuestaServicio(TurnoRepositorio turnos, EncuestaRepositorio encuestas, Calendario calendario) {
        this.turnos = turnos;
        this.encuestas = encuestas;
        this.calendario = calendario;
    }

    @Transactional(readOnly = true)
    public Info ver(Long idTurno, String token) {
        Turno t = turnoConToken(idTurno, token);
        var encuesta = encuestas.findByTurnoId(idTurno);
        return new Info(t.getId(), t.getFecha(), t.getServicio().getNombre(), t.getBarbero().getNombre(),
                t.getCliente().getNombre(), encuesta.isPresent(),
                encuesta.map(Encuesta::getCalificacion).orElse(null),
                encuesta.map(Encuesta::getComentario).orElse(null));
    }

    public Info responder(Long idTurno, String token, Pedido pedido) {
        Turno t = turnoConToken(idTurno, token);
        if (encuestas.findByTurnoId(idTurno).isPresent()) {
            throw new ConflictoException("Esta encuesta ya fue respondida. ¡Gracias!");
        }
        Encuesta e = new Encuesta();
        e.setTurno(t);
        e.setCalificacion(pedido.calificacion());
        String comentario = pedido.comentario() == null ? null : pedido.comentario().trim();
        e.setComentario(comentario == null || comentario.isEmpty() ? null : comentario);
        e.setFechaRespuesta(calendario.ahora());
        encuestas.save(e);
        return ver(idTurno, token);
    }

    /**
     * La encuesta se abre desde el link del email, que trae el token. Si el turno
     * no existe o el token no coincide se responde lo mismo (404), para no dar pistas.
     */
    private Turno turnoConToken(Long idTurno, String token) {
        return turnos.conDetalle(idTurno)
                .filter(t -> t.getEstado() == EstadoTurno.COMPLETADO)
                .filter(t -> Tokens.iguales(t.getTokenEncuesta(), token))
                .orElseThrow(() -> new NoEncontradoException("La encuesta no existe o el link no es válido"));
    }
}
