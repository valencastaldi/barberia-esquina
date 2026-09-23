package com.barberiaesquina.turnos.seguridad;

import com.barberiaesquina.turnos.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RNF: como máximo N reservas por IP por hora (10 por defecto), para que nadie
 * llene la agenda con turnos falsos. Ventana deslizante en memoria: alcanza para
 * una sola instancia de la API.
 */
@Component
public class LimiteDeReservasFiltro extends OncePerRequestFilter {

    private static final Duration VENTANA = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> pedidosPorIp = new ConcurrentHashMap<>();
    private final AppProperties props;
    private final Clock reloj;

    public LimiteDeReservasFiltro(AppProperties props, Clock reloj) {
        this.props = props;
        this.reloj = reloj;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/api/v1/turnos".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        Instant ahora = reloj.instant();
        // Detrás del proxy HTTPS de producción, server.forward-headers-strategy hace que
        // getRemoteAddr() ya sea la IP real. No se lee X-Forwarded-For a mano porque el
        // cliente lo puede falsificar para esquivar el límite.
        Deque<Instant> pedidos = pedidosPorIp.computeIfAbsent(request.getRemoteAddr(), k -> new ArrayDeque<>());

        boolean excedido;
        synchronized (pedidos) {
            while (!pedidos.isEmpty() && pedidos.peekFirst().isBefore(ahora.minus(VENTANA))) {
                pedidos.pollFirst();
            }
            excedido = pedidos.size() >= props.turnos().reservasPorIpHora();
            if (!excedido) pedidos.addLast(ahora);
        }

        if (excedido) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {"title":"Demasiadas reservas","status":429,\
                    "detail":"Se alcanzó el límite de reservas desde esta conexión. Probá de nuevo en un rato."}""");
            return;
        }
        cadena.doFilter(request, response);
    }

    /** Para los tests. */
    public void reiniciar() {
        pedidosPorIp.clear();
    }
}
