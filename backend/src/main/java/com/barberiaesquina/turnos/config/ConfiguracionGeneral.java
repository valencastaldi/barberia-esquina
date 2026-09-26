package com.barberiaesquina.turnos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class ConfiguracionGeneral {

    /**
     * Toda la lógica de "ahora" (horarios que ya pasaron, vencimiento de tokens)
     * usa este reloj con la hora de Córdoba. En los tests se reemplaza por uno fijo.
     */
    @Bean
    Clock reloj(AppProperties props) {
        return Clock.system(ZoneId.of(props.zonaHoraria()));
    }
}
