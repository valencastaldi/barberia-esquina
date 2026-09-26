package com.barberiaesquina.turnos.seguridad;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;

/**
 * Cuenta eventos por IP en una ventana deslizante, en memoria (alcanza para una
 * sola instancia de la API). Las IP que no tienen eventos recientes se borran,
 * así el mapa no crece sin límite con conexiones que no vuelven.
 */
final class VentanaPorIp {

    private static final int LIMPIAR_CADA = 1_000;

    private final Map<String, Deque<Instant>> eventosPorIp = new ConcurrentHashMap<>();
    private final Duration ventana;
    private final IntSupplier maximo;
    private final Clock reloj;
    private int hastaLimpiar = LIMPIAR_CADA;

    VentanaPorIp(Duration ventana, IntSupplier maximo, Clock reloj) {
        this.ventana = ventana;
        this.maximo = maximo;
        this.reloj = reloj;
    }

    /** ¿Esta IP ya llegó al máximo dentro de la ventana? */
    boolean excedido(String ip) {
        Deque<Instant> eventos = eventosPorIp.get(ip);
        if (eventos == null) return false;
        synchronized (eventos) {
            descartarViejos(eventos, reloj.instant());
            return eventos.size() >= maximo.getAsInt();
        }
    }

    /** Anota un evento. Devuelve false (y no lo anota) si la IP ya estaba en el máximo. */
    boolean registrar(String ip) {
        limpiarDeVezEnCuando();
        Instant ahora = reloj.instant();
        Deque<Instant> eventos = eventosPorIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (eventos) {
            descartarViejos(eventos, ahora);
            if (eventos.size() >= maximo.getAsInt()) return false;
            eventos.addLast(ahora);
            return true;
        }
    }

    void reiniciar() {
        eventosPorIp.clear();
    }

    private void descartarViejos(Deque<Instant> eventos, Instant ahora) {
        Instant limite = ahora.minus(ventana);
        while (!eventos.isEmpty() && eventos.peekFirst().isBefore(limite)) eventos.pollFirst();
    }

    private void limpiarDeVezEnCuando() {
        synchronized (this) {
            if (--hastaLimpiar > 0) return;
            hastaLimpiar = LIMPIAR_CADA;
        }
        Instant ahora = reloj.instant();
        eventosPorIp.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                descartarViejos(e.getValue(), ahora);
                return e.getValue().isEmpty();
            }
        });
    }

    static void responderDemasiados(HttpServletResponse response, String titulo, String detalle) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"title":"%s","status":429,"detail":"%s"}""".formatted(titulo, detalle));
    }
}
