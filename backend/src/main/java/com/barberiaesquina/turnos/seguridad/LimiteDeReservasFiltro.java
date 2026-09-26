package com.barberiaesquina.turnos.seguridad;

import com.barberiaesquina.turnos.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

/**
 * RNF: como máximo N reservas por IP por hora (10 por defecto), para que nadie
 * llene la agenda con turnos falsos.
 */
@Component
public class LimiteDeReservasFiltro extends OncePerRequestFilter {

    private final VentanaPorIp reservas;

    public LimiteDeReservasFiltro(AppProperties props, Clock reloj) {
        this.reservas = new VentanaPorIp(Duration.ofHours(1), () -> props.turnos().reservasPorIpHora(), reloj);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/api/v1/turnos".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        // Detrás del proxy HTTPS de producción, server.forward-headers-strategy hace que
        // getRemoteAddr() ya sea la IP real. No se lee X-Forwarded-For a mano porque el
        // cliente lo puede falsificar para esquivar el límite.
        if (!reservas.registrar(request.getRemoteAddr())) {
            VentanaPorIp.responderDemasiados(response, "Demasiadas reservas",
                    "Se alcanzó el límite de reservas desde esta conexión. Probá de nuevo en un rato.");
            return;
        }
        cadena.doFilter(request, response);
    }

    /** Para los tests. */
    public void reiniciar() {
        reservas.reiniciar();
    }
}
