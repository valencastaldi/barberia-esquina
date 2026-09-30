package com.barberiaesquina.turnos.repositorio;

import com.barberiaesquina.turnos.modelo.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmailIgnoreCase(String email);

    /** Para reconocer a un cliente cargado desde el panel sin email. */
    Optional<Cliente> findFirstByTelefonoAndEmailIsNullOrderByIdAsc(String telefono);
}
