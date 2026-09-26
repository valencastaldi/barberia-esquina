package com.barberiaesquina.turnos.repositorio;

import com.barberiaesquina.turnos.modelo.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface TokenRevocadoRepositorio extends JpaRepository<TokenRevocado, String> {

    @Modifying
    @Query("delete from TokenRevocado t where t.vence < :ahora")
    int borrarVencidos(LocalDateTime ahora);
}
