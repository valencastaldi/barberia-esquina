package com.barberiaesquina.turnos;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** [Extensión] Turnos cargados desde el panel: por teléfono o de alguien que vino sin reservar. */
class TurnoDesdePanelTest extends PruebaDeIntegracion {

    private static final LocalDate DIA = HOY.plusDays(7);   // martes siguiente

    private static String turno(Long idServicio, Long idBarbero, String hora, String email, String telefono) {
        return """
                {"idServicio": %d, "idBarbero": %s, "fecha": "%s", "hora": "%s",
                 "cliente": {"nombre": "Lucas", "apellido": "Ferreyra", "email": %s, "telefono": "%s"}}
                """.formatted(idServicio, idBarbero == null ? "null" : idBarbero.toString(), DIA, hora,
                email == null ? "null" : "\"" + email + "\"", telefono);
    }

    @Test
    void sinSesionNoSePuede() throws Exception {
        mvc.perform(json(post("/api/v1/turnos/panel"), turno(corte.getId(), agustin.getId(), "15:00", null, "351 711-0043")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elDuenoCargaUnTurnoSinEmailParaOtroPeluquero() throws Exception {
        String token = login("agustin@test.com");
        mvc.perform(conToken(json(post("/api/v1/turnos/panel"),
                        turno(corte.getId(), santiago.getId(), "15:00", null, "351 711-0043")), token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("pendiente"))
                .andExpect(jsonPath("$.horaFin").value("15:30"))
                .andExpect(jsonPath("$.barbero.id").value(santiago.getId()))
                .andExpect(jsonPath("$.cliente.email").doesNotExist());

        assertThat(clientes.findAll()).singleElement().satisfies(c -> assertThat(c.getEmail()).isNull());
    }

    @Test
    void elClienteSinEmailSeReconocePorElTelefono() throws Exception {
        String token = login("agustin@test.com");
        mvc.perform(conToken(json(post("/api/v1/turnos/panel"),
                        turno(corte.getId(), agustin.getId(), "15:00", null, "351 711-0043")), token))
                .andExpect(status().isCreated());
        mvc.perform(conToken(json(post("/api/v1/turnos/panel"),
                        turno(corte.getId(), agustin.getId(), "16:00", "", "351 711-0043")), token))
                .andExpect(status().isCreated());
        mvc.perform(conToken(json(post("/api/v1/turnos/panel"),
                        turno(corte.getId(), agustin.getId(), "17:00", null, "351 999-0000")), token))
                .andExpect(status().isCreated());

        assertThat(clientes.findAll()).hasSize(2);
    }

    @Test
    void conEmailSeUsaElClienteQueYaExiste() throws Exception {
        mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), agustin.getId(), DIA, "10:00", "lucas@test.com")))
                .andExpect(status().isCreated());

        String token = login("agustin@test.com");
        mvc.perform(conToken(json(post("/api/v1/turnos/panel"),
                        turno(corte.getId(), agustin.getId(), "15:00", "LUCAS@test.com", "351 711-0043")), token))
                .andExpect(status().isCreated());

        assertThat(clientes.findAll()).hasSize(1);
    }

    @Test
    void unBarberoSoloSeCargaTurnosASiMismo() throws Exception {
        String token = login("santiago@test.com");
        mvc.perform(conToken(json(post("/api/v1/turnos/panel"),
                        turno(corte.getId(), agustin.getId(), "15:00", null, "351 711-0043")), token))
                .andExpect(status().isForbidden());

        // Sin elegir peluquero, queda para él.
        mvc.perform(conToken(json(post("/api/v1/turnos/panel"),
                        turno(corte.getId(), null, "15:00", null, "351 711-0043")), token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barbero.id").value(santiago.getId()));
    }

    @Test
    void respetaLaDisponibilidad() throws Exception {
        turnoGuardado(agustin, corte, DIA, "15:00", com.barberiaesquina.turnos.modelo.EstadoTurno.PENDIENTE);
        String token = login("agustin@test.com");
        mvc.perform(conToken(json(post("/api/v1/turnos/panel"),
                        turno(corte.getId(), agustin.getId(), "15:00", null, "351 711-0043")), token))
                .andExpect(status().isConflict());
    }

    @Test
    void elEmailSiVieneTieneQueSerValido() throws Exception {
        String token = login("agustin@test.com");
        mvc.perform(conToken(json(post("/api/v1/turnos/panel"),
                        turno(corte.getId(), agustin.getId(), "15:00", "no-es-un-email", "351 711-0043")), token))
                .andExpect(status().isBadRequest());
    }
}
