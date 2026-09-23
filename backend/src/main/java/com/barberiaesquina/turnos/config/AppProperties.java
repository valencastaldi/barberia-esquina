package com.barberiaesquina.turnos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Todo lo configurable de la app, bajo el prefijo "app" en application.yml. */
@ConfigurationProperties("app")
public record AppProperties(
        String zonaHoraria,
        String frontendUrl,
        List<String> origenesPermitidos,
        Jwt jwt,
        Turnos turnos,
        Admin admin,
        Mail mail,
        boolean datosDemo
) {

    public record Jwt(String secreto, long expiracionMinutos) {}

    /**
     * @param cancelacionHoras   RNF: el link de cancelación vence a las 48 h.
     * @param diasAnticipacion   hasta cuántos días adelante se puede reservar.
     * @param reservasPorIpHora  RNF: rate limiting de 10 reservas por IP por hora.
     */
    public record Turnos(int cancelacionHoras, int diasAnticipacion, int reservasPorIpHora) {}

    /** Dueño que se crea si la tabla barbero está vacía. */
    public record Admin(String email, String password, String nombre, String apellido) {}

    public record Mail(String remitente, boolean habilitado) {}
}
