package com.barberiaesquina.turnos;

import com.barberiaesquina.turnos.modelo.Barbero;
import com.barberiaesquina.turnos.modelo.Cliente;
import com.barberiaesquina.turnos.modelo.EstadoTurno;
import com.barberiaesquina.turnos.modelo.Rol;
import com.barberiaesquina.turnos.modelo.Turno;
import com.barberiaesquina.turnos.seguridad.LimpiezaDeTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeguridadTest extends PruebaDeIntegracion {

    @Autowired LimpiezaDeTokens limpiezaDeTokens;

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
    void unBarberoNoVeLaInformacionDelNegocio() throws Exception {
        String barbero = login("santiago@test.com");
        mvc.perform(conToken(get("/api/v1/clientes"), barbero)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/v1/dashboard/resumen"), barbero)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/v1/dashboard/barberos"), barbero)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/v1/barberos/equipo"), barbero)).andExpect(status().isForbidden());
        mvc.perform(conToken(get("/api/v1/pagos"), barbero)).andExpect(status().isForbidden());

        String dueno = login("agustin@test.com");
        mvc.perform(conToken(get("/api/v1/clientes"), dueno)).andExpect(status().isOk());
        mvc.perform(conToken(get("/api/v1/dashboard/resumen"), dueno)).andExpect(status().isOk());
    }

    @Test
    void enLaAgendaUnBarberoNoVeContactoNiCobrosDeTurnosAjenos() throws Exception {
        turnoGuardado(agustin, corte, HOY, "10:00", EstadoTurno.PENDIENTE);
        turnoGuardado(santiago, corte, HOY, "12:00", EstadoTurno.PENDIENTE);

        mvc.perform(conToken(get("/api/v1/turnos"), login("santiago@test.com")))
                .andExpect(status().isOk())
                // El de Agustín: se ve quién y qué, pero no el teléfono
                .andExpect(jsonPath("$[0].barbero.nombre").value("Agustín"))
                .andExpect(jsonPath("$[0].cliente.nombre").value("Mateo"))
                .andExpect(jsonPath("$[0].cliente.telefono").doesNotExist())
                .andExpect(jsonPath("$[0].cliente.email").doesNotExist())
                // El propio: completo
                .andExpect(jsonPath("$[1].cliente.telefono").value("351 415-2233"));

        mvc.perform(conToken(get("/api/v1/turnos"), login("agustin@test.com")))
                .andExpect(jsonPath("$[0].cliente.telefono").value("351 415-2233"));
    }

    @Test
    void unBarberoNoCambiaHorariosNiBloqueaFranjas() throws Exception {
        String barbero = login("santiago@test.com");
        mvc.perform(conToken(get("/api/v1/horarios").param("barbero", santiago.getId().toString()), barbero))
                .andExpect(status().isOk());
        mvc.perform(json(conToken(put("/api/v1/horarios"), barbero), """
                        {"dias": [{"diaSemana": 1, "horaInicio": "10:00", "horaFin": "20:00", "duracionSlotMin": 30, "activo": true}]}"""))
                .andExpect(status().isForbidden());
        mvc.perform(json(conToken(post("/api/v1/bloqueos"), barbero), """
                        {"fecha": "%s", "horaInicio": "13:00", "horaFin": "14:00"}""".formatted(HOY.plusDays(7))))
                .andExpect(status().isForbidden());

        mvc.perform(json(conToken(post("/api/v1/bloqueos"), login("agustin@test.com")), """
                        {"idBarbero": %d, "fecha": "%s", "horaInicio": "13:00", "horaFin": "14:00"}""".formatted(santiago.getId(), HOY.plusDays(7))))
                .andExpect(status().isCreated());
    }

    @Test
    void todoElEquipoVeLasOpiniones() throws Exception {
        Turno t = turnoGuardado(agustin, corte, HOY, "10:00", EstadoTurno.COMPLETADO);
        jdbc.update("insert into encuesta (id_turno, calificacion, comentario, fecha_respuesta) values (?, 5, 'Excelente', ?)",
                t.getId(), AHORA);

        mvc.perform(conToken(get("/api/v1/opiniones"), login("santiago@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.opiniones[0].barbero.nombre").value("Agustín"))
                .andExpect(jsonPath("$.opiniones[0].cliente").value("Mateo G."))
                .andExpect(jsonPath("$.opiniones[0].comentario").value("Excelente"))
                .andExpect(jsonPath("$.porBarbero[?(@.nombre == 'Agustín')].promedio").value(5.0))
                .andExpect(jsonPath("$.porBarbero[?(@.nombre == 'Santiago')].cantidad").value(0));
        mvc.perform(get("/api/v1/opiniones")).andExpect(status().isUnauthorized());
    }

    @Test
    void unBarberoSoloCobraSusTurnos() throws Exception {
        Turno deAgustin = turnoGuardado(agustin, corte, HOY, "10:00", EstadoTurno.COMPLETADO);
        Turno deSantiago = turnoGuardado(santiago, corte, HOY, "10:30", EstadoTurno.COMPLETADO);
        String barbero = login("santiago@test.com");

        mvc.perform(json(conToken(post("/api/v1/pagos"), barbero), """
                        {"idTurno": %d, "monto": 9000, "medio": "efectivo"}""".formatted(deAgustin.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(json(conToken(post("/api/v1/pagos"), barbero), """
                        {"idTurno": %d, "monto": 9000, "medio": "efectivo"}""".formatted(deSantiago.getId())))
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

    // ---------- La sesión se controla contra la base, no solo contra el token ----------

    @Test
    void unDuenoPasadoABarberoPierdeLosPermisosAlInstante() throws Exception {
        Barbero otroDueno = barbero("Nicolás", "nicolas@test.com", Rol.DUENO, 0);
        String token = login("nicolas@test.com");
        mvc.perform(conToken(get("/api/v1/clientes"), token)).andExpect(status().isOk());

        otroDueno.setRol(Rol.BARBERO);
        barberos.save(otroDueno);

        mvc.perform(conToken(get("/api/v1/clientes"), token)).andExpect(status().isForbidden());
    }

    @Test
    void elTokenDeUnPeluqueroDadoDeBajaDejaDeServir() throws Exception {
        String token = login("santiago@test.com");
        santiago.setActivo(false);
        barberos.save(santiago);

        mvc.perform(conToken(get("/api/v1/turnos"), token)).andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarLaContrasenaCierraLasSesionesAnteriores() throws Exception {
        String token = login("santiago@test.com");
        santiago.setPasswordHash(passwordEncoder.encode("otra-clave-nueva"));
        barberos.save(santiago);

        mvc.perform(conToken(get("/api/v1/auth/me"), token)).andExpect(status().isUnauthorized());
    }

    @Test
    void cerrarSesionAnulaEseTokenPeroNoLasOtrasSesiones() throws Exception {
        String compu = login("santiago@test.com");
        String celular = login("santiago@test.com");

        mvc.perform(conToken(post("/api/v1/auth/logout"), compu)).andExpect(status().isNoContent());

        mvc.perform(conToken(get("/api/v1/auth/me"), compu)).andExpect(status().isUnauthorized());
        mvc.perform(conToken(get("/api/v1/auth/me"), celular)).andExpect(status().isOk());
    }

    @Test
    void laLimpiezaBorraSoloLosTokensRevocadosQueYaVencieron() {
        jdbc.update("insert into token_revocado (jti, vence) values ('viejo', ?), ('vigente', ?)",
                AHORA.minusHours(1), AHORA.plusHours(1));

        limpiezaDeTokens.borrarVencidos();

        assertThat(jdbc.queryForList("select jti from token_revocado", String.class)).containsExactly("vigente");
    }

    @Test
    void despuesDeDiezLoginsFallidosLaIpEspera() throws Exception {
        String mal = """
                {"email": "agustin@test.com", "password": "mal"}""";
        for (int i = 0; i < 10; i++) {
            mvc.perform(json(post("/api/v1/auth/login"), mal)).andExpect(status().isUnauthorized());
        }
        mvc.perform(json(post("/api/v1/auth/login"), mal)).andExpect(status().isTooManyRequests());
        // Ni siquiera con la contraseña correcta: si no, el límite no frena nada.
        mvc.perform(json(post("/api/v1/auth/login"), """
                        {"email": "agustin@test.com", "password": "%s"}""".formatted(PASSWORD)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void unaReservaNoCambiaLosDatosDeUnClienteQueYaExiste() throws Exception {
        turnoGuardado(agustin, corte, HOY.minusDays(3), "10:00", EstadoTurno.COMPLETADO);   // cliente@test.com

        mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), agustin.getId(), HOY.plusDays(7), "10:00",
                        "cliente@test.com")))
                .andExpect(status().isCreated());

        Cliente c = clientes.findByEmailIgnoreCase("cliente@test.com").orElseThrow();
        assertThat(c.getNombre()).isEqualTo("Mateo");
        assertThat(c.getTelefono()).isEqualTo("351 415-2233");
    }

    @Test
    void losReportesDelPanelNoAceptanRangosDeMasDeUnAno() throws Exception {
        String dueno = login("agustin@test.com");
        mvc.perform(conToken(get("/api/v1/pagos"), dueno).param("desde", "2020-01-01").param("hasta", "2026-01-01"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(conToken(get("/api/v1/bloqueos"), dueno).param("desde", "2020-01-01").param("hasta", "2026-01-01"))
                .andExpect(status().isUnprocessableContent());
    }
}
