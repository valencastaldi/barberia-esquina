package com.barberiaesquina.turnos.repositorio;

import com.barberiaesquina.turnos.modelo.Bloqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface BloqueoRepositorio extends JpaRepository<Bloqueo, Long> {

    List<Bloqueo> findByBarberoIdAndFecha(Long idBarbero, LocalDate fecha);

    @Query("""
            select b from Bloqueo b join fetch b.barbero
            where b.fecha between :desde and :hasta
              and (:idBarbero is null or b.barbero.id = :idBarbero)
            order by b.fecha, b.horaInicio
            """)
    List<Bloqueo> entre(LocalDate desde, LocalDate hasta, Long idBarbero);
}
