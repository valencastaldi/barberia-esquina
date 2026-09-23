package com.barberiaesquina.turnos;

import com.barberiaesquina.turnos.modelo.EstadoTurno;
import com.barberiaesquina.turnos.modelo.Turno;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeguridadTest extends PruebaDeIntegracion {

    @Test
    void elPanelPideLogin() throws Exception {
        mvc.perform(get("/api/v1/turnos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/dashboard/resumen")).andExpect(status().isUnauthorized());
        mvc.perform(conToken(get("/api/v1/turnos"), "token-trucho")).andExpect(status().isUnauthorized());
    }

    @Test
    void loQueUsaElClienteEsPublico() throws Exception {
        mvc.perform(get("/api/v1/servicios")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/barberos")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/horarios")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/resenas")).andExpect(status().isOk());
    }

    @Test
    void loginConCredencialesIncorrectas() throws Exception {
        mvc.perform(json(post("/api/v1/auth/login"), """
                        {"email": "agustin@test.com", "password": "mal"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email o contraseña incorrectos"));
        mvc.perform(json(post("/api/v1/auth/login"), """
                        {"email": "nadie@test.com", "password": "mal"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email o contraseña incorrectos"));
    }

    @Test
    void loginDevuelveUnTokenQueIdentificaAlUsuario() throws Exception {
        mvc.perform(conToken(get("/api/v1/auth/me"), login("santiago@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Santiago"))
                .andExpect(jsonPath("$.rol").value("barbero"));
    }

    @Test
    void unPeluqueroDadoDeBajaNoEntra() throws Exception {
        santiago.setActivo(false);
        barberos.save(santiago);
        mvc.perform(json(post("/api/v1/auth/login"), """
                        {"email": "santiago@test.com", "password": "%s"}""".formatted(PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void soloElDuenoAdministraElCatalogoYElEquipo() throws Exception {
        String barbero = login("santiago@test.com");
        String nuevoServicio = """
                {"nombre": "Color", "duracionMinutos": 90, "precio": 25000}""";

        mvc.perform(json(conToken(post("/api/v1/servicios"), barbero), nuevoServicio))
                .andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/v1/pagos"), barbero)).andExpect(status().isForbidden());

        mvc.perform(json(conToken(post("/api/v1/servicios"), login("agustin@test.com")), nuevoServicio))
                .andExpect(status().isCreated());
    }

    @Test
    void unPeluqueroSoloManejaSusPropiosTurnos() throws Exception {
        Turno deAgustin = turnoGuardado(agustin, corte, HOY, "10:00", EstadoTurno.PENDIENTE);
        Turno deSantiago = turnoGuardado(santiago, corte, HOY, "10:00", EstadoTurno.PENDIENTE);
        String barbero = login("santiago@test.com");

        mvc.perform(json(conToken(patch("/api/v1/turnos/" + deAgustin.getId() + "/estado"), barbero), """
                        {"estado": "ausente"}"""))
                .andExpect(status().isForbidden());
        mvc.perform(json(conToken(patch("/api/v1/turnos/" + deSantiago.getId() + "/estado"), barbero), """
                        {"estado": "ausente"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void siempreQuedaUnDuenoActivo() throws Exception {
        mvc.perform(json(conToken(patch("/api/v1/barberos/" + agustin.getId() + "/estado"), login("agustin@test.com")), """
                        {"activo": false}"""))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void masDeDiezReservasPorHoraDesdeLaMismaIpSeCortan() throws Exception {
        for (int i = 0; i < 10; i++) {
            String hora = String.format("%02d:00", 10 + i % 10);
            mvc.perform(json(post("/api/v1/turnos"),
                            reserva(corte.getId(), agustin.getId(), HOY.plusDays(7), hora, "c" + i + "@test.com")))
                    .andExpect(status().isCreated());
        }
        mvc.perform(json(post("/api/v1/turnos"),
                        reserva(corte.getId(), santiago.getId(), HOY.plusDays(7), "10:00", "otro@test.com")))
                .andExpect(status().isTooManyRequests());
    }
}
