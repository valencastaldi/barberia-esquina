package com.barberiaesquina.turnos.repositorio;

import com.barberiaesquina.turnos.modelo.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PagoRepositorio extends JpaRepository<Pago, Long> {

    boolean existsByTurnoId(Long idTurno);

    List<Pago> findByTurnoIdIn(Collection<Long> idsTurnos);
}
