package com.barberiaesquina.turnos.repositorio;

import com.barberiaesquina.turnos.modelo.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PagoRepositorio extends JpaRepository<Pago, Long> {

    boolean existsByTurnoId(Long idTurno);

    Optional<Pago> findByTurnoId(Long idTurno);

    List<Pago> findByTurnoIdIn(Collection<Long> idsTurnos);
}
