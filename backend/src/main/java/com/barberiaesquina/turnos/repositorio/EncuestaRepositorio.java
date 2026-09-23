package com.barberiaesquina.turnos.repositorio;

import com.barberiaesquina.turnos.modelo.Encuesta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EncuestaRepositorio extends JpaRepository<Encuesta, Long> {

    Optional<Encuesta> findByTurnoId(Long idTurno);

    List<Encuesta> findByTurnoIdIn(Collection<Long> idsTurnos);
}
