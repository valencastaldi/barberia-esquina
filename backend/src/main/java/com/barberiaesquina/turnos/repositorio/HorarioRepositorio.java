package com.barberiaesquina.turnos.repositorio;

import com.barberiaesquina.turnos.modelo.HorarioAtencion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HorarioRepositorio extends JpaRepository<HorarioAtencion, Long> {

    List<HorarioAtencion> findByBarberoIdOrderByDiaSemanaAsc(Long idBarbero);

    Optional<HorarioAtencion> findByBarberoIdAndDiaSemana(Long idBarbero, Integer diaSemana);

    @Query("select h from HorarioAtencion h join fetch h.barbero b where b.activo = true and h.activo = true")
    List<HorarioAtencion> activosDeBarberosActivos();
}
