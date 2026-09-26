package com.barberiaesquina.turnos.seguridad;

import com.barberiaesquina.turnos.repositorio.TokenRevocadoRepositorio;
import com.barberiaesquina.turnos.servicio.Calendario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cada hora borra los tokens revocados que ya vencieron: un token vencido no
 * sirve de todas formas, así que la tabla solo guarda sesiones cerradas de las últimas horas.
 */
@Component
public class LimpiezaDeTokens {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaDeTokens.class);

    private final TokenRevocadoRepositorio revocados;
    private final Calendario calendario;

    public LimpiezaDeTokens(TokenRevocadoRepositorio revocados, Calendario calendario) {
        this.revocados = revocados;
        this.calendario = calendario;
    }

    @Scheduled(initialDelayString = "PT10M", fixedDelayString = "PT1H")
    @Transactional
    public void borrarVencidos() {
        int borrados = revocados.borrarVencidos(calendario.ahora());
        if (borrados > 0) log.info("Se borraron {} tokens revocados que ya vencieron", borrados);
    }
}
