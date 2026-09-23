package com.barberiaesquina.turnos.repositorio;

import com.barberiaesquina.turnos.modelo.Barbero;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BarberoRepositorio extends JpaRepository<Barbero, Long> {

    Optional<Barbero> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "servicios")
    List<Barbero> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "servicios")
    List<Barbero> findByActivoTrueOrderByIdAsc();

    /**
     * Bloquea la fila del peluquero durante la reserva: dos clientes que piden el
     * mismo horario al mismo tiempo se atienden de a uno y el segundo ve el conflicto.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Barbero b where b.id = :id")
    Optional<Barbero> bloquearParaReservar(Long id);
}
