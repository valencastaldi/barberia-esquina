package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.seguridad.SesionActual;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Lo público que casi no cambia (servicios, peluqueros, horario, reseñas): el navegador del
 * cliente puede reusarlo 60 segundos sin volver a pedirlo. Con sesión (el panel) no se guarda
 * nunca: ahí se edita, y el cambio se tiene que ver en el momento. "Vary: Authorization" evita
 * que la respuesta guardada sin sesión se use para un pedido con sesión.
 */
@Component
public class CachePublica {

    private static final CacheControl UN_MINUTO = CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic();

    private final SesionActual sesion;

    public CachePublica(SesionActual sesion) {
        this.sesion = sesion;
    }

    public <T> ResponseEntity<T> ok(T cuerpo) {
        return ResponseEntity.ok()
                .cacheControl(sesion.estaAutenticado() ? CacheControl.noStore() : UN_MINUTO)
                .varyBy(HttpHeaders.AUTHORIZATION)
                .body(cuerpo);
    }
}
