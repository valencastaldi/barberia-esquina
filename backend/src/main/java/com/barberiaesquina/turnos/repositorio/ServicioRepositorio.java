package com.barberiaesquina.turnos.repositorio;

import com.barberiaesquina.turnos.modelo.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicioRepositorio extends JpaRepository<Servicio, Long> {

    List<Servicio> findAllByOrderByIdAsc();

    List<Servicio> findByActivoTrueOrderByIdAsc();
}
