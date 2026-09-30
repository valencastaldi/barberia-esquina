package com.barberiaesquina.turnos;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Qué puede guardar el navegador sin volver a pedirlo (header Cache-Control). */
class CacheHttpTest extends PruebaDeIntegracion {

    private static final List<String> PUBLICO = List.of(
            "/api/v1/servicios", "/api/v1/barberos", "/api/v1/horarios", "/api/v1/resenas");

    @Test
    void loPublicoQueCasiNoCambiaSeGuardaUnMinuto() throws Exception {
        for (String ruta : PUBLICO) {
            mvc.perform(get(ruta))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "max-age=60, public"))
                    .andExpect(header().stringValues("Vary", hasItem(containsString("Authorization"))));
        }
    }

    @Test
    void conSesionNoSeGuardaNada() throws Exception {
        // El panel usa las mismas rutas para editar: después de guardar tiene que ver el cambio.
        String token = login("agustin@test.com");
        for (String ruta : PUBLICO) {
            mvc.perform(conToken(get(ruta), token))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", containsString("no-store")));
        }
    }

    @Test
    void laDisponibilidadNoSeGuarda() throws Exception {
        // Cambia con cada reserva.
        mvc.perform(get("/api/v1/disponibilidad/dias").param("servicio", corte.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }
}
