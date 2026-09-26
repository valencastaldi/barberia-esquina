package com.barberiaesquina.turnos.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

/**
 * Frena la prueba de contraseñas: después de 10 logins fallidos desde una IP en
 * 15 minutos, esa IP no puede intentar más hasta que se vayan venciendo.
 * Los logins correctos no cuentan, así el equipo no se bloquea en el uso normal.
 */
@Component
public class LimiteDeLoginFiltro extends OncePerRequestFilter {

    static final int FALLIDOS_MAXIMOS = 10;

    private final VentanaPorIp fallidos;

    public LimiteDeLoginFiltro(Clock reloj) {
        this.fallidos = new VentanaPorIp(Duration.ofMinutes(15), () -> FALLIDOS_MAXIMOS, reloj);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/api/v1/auth/login".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();   // misma aclaración que en LimiteDeReservasFiltro
        if (fallidos.excedido(ip)) {
            VentanaPorIp.responderDemasiados(response, "Demasiados intentos",
                    "Hubo muchos intentos fallidos. Esperá unos minutos y probá de nuevo.");
            return;
        }
        cadena.doFilter(request, response);
        if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) fallidos.registrar(ip);
    }

    /** Para los tests. */
    public void reiniciar() {
        fallidos.reiniciar();
    }
}
