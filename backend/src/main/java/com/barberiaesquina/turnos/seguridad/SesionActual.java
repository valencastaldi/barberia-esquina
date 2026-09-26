package com.barberiaesquina.turnos.seguridad;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Quién está haciendo el pedido, leído del JWT. */
@Component
public class SesionActual {

    public boolean estaAutenticado() {
        return jwt() != null;
    }

    public Long idBarbero() {
        Jwt jwt = jwt();
        return jwt == null ? null : Long.valueOf(jwt.getSubject());
    }

    /** Según el rol actual en la base (ver ConversorDeSesion), no el que tenía al emitirse el token. */
    public boolean esDueno() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return jwt() != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DUENO".equals(a.getAuthority()));
    }

    /** El JWT del pedido (para revocarlo al cerrar sesión), o null si no hay sesión. */
    public Jwt token() {
        return jwt();
    }

    private Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Jwt jwt ? jwt : null;
    }
}
