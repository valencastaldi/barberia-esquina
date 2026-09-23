package com.barberiaesquina.turnos.repositorio;

import com.barberiaesquina.turnos.modelo.Encuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EncuestaRepositorio extends JpaRepository<Encuesta, Long> {

    Optional<Encuesta> findByTurnoId(Long idTurno);

    List<Encuesta> findByTurnoIdIn(Collection<Long> idsTurnos);

    /** Encuestas de turnos en el rango de fechas, con todo lo necesario para mostrarlas. */
    @Query("""
            select e from Encuesta e
            join fetch e.turno t join fetch t.cliente join fetch t.servicio join fetch t.barbero
            where t.fecha between :desde and :hasta
            order by e.fechaRespuesta desc
            """)
    List<Encuesta> delPeriodo(LocalDate desde, LocalDate hasta);
}
