package com.barberiaesquina.turnos.seguridad;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public boolean esDueno() {
        Jwt jwt = jwt();
        if (jwt == null) return false;
        List<String> roles = jwt.getClaimAsStringList(SeguridadConfig.CLAIM_ROLES);
        return roles != null && roles.contains("DUENO");
    }

    private Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Jwt jwt ? jwt : null;
    }
}
