package com.barberiaesquina.turnos;

import com.barberiaesquina.turnos.modelo.EstadoTurno;
import com.barberiaesquina.turnos.modelo.Turno;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El ciclo de vida completo de un turno, de la reserva al cobro. */
class FlujoDeTurnosTest extends PruebaDeIntegracion {

    private static final LocalDate DIA = HOY.plusDays(7);   // martes siguiente

    @Test
    void reservarOcupaElHorarioYNoSePuedeReservarDosVeces() throws Exception {
        mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), agustin.getId(), DIA, "15:00", "lucas@test.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horaInicio").value("15:00"))
                .andExpect(jsonPath("$.horaFin").value("15:30"))
                .andExpect(jsonPath("$.barbero").value("Agustín Test"))
                .andExpect(jsonPath("$.precio").value(9000))
                .andExpect(jsonPath("$.tokenCancelacion", hasLength(43)));

        mvc.perform(get("/api/v1/disponibilidad").param("servicio", corte.getId().toString())
                        .param("fecha", DIA.toString()).param("barbero", agustin.getId().toString()))
                .andExpect(jsonPath("$.slots[?(@.hora == '15:00')].libre").value(false));

        mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), agustin.getId(), DIA, "15:00", "otro@test.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ese horario se acaba de ocupar. Elegí otro."));
    }

    @Test
    void reservasSimultaneasAlMismoHorarioSoloGuardanUna() throws Exception {
        int pedidos = 8;
        var listos = new java.util.concurrent.CountDownLatch(1);
        var hilos = java.util.concurrent.Executors.newFixedThreadPool(pedidos);
        var codigos = new java.util.concurrent.ConcurrentLinkedQueue<Integer>();
        for (int i = 0; i < pedidos; i++) {
            String email = "carrera" + i + "@test.com";
            hilos.submit(() -> {
                listos.await();
                codigos.add(mvc.perform(json(post("/api/v1/turnos"),
                                reserva(corteYBarba.getId(), agustin.getId(), DIA, "10:00", email)))
                        .andReturn().getResponse().getStatus());
                return null;
            });
        }
        listos.countDown();   // todos salen a la vez
        hilos.shutdown();
        assertThat(hilos.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        assertThat(codigos).hasSize(pedidos);
        assertThat(codigos.stream().filter(c -> c == 201)).hasSize(1);
        assertThat(codigos.stream().filter(c -> c == 409)).hasSize(pedidos - 1);
        assertThat(turnos.findAll().stream()
                .filter(t -> t.getBarbero().getId().equals(agustin.getId()) && t.getFecha().equals(DIA))).hasSize(1);
    }

    @Test
    void sinPeluqueroElegidoSeAsignaUnoLibre() throws Exception {
        String primero = mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), null, DIA, "16:00", "a@test.com")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String segundo = mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), null, DIA, "16:00", "b@test.com")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(primero, "$.barbero")).isNotEqualTo(JsonPath.read(segundo, "$.barbero"));

        mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), null, DIA, "16:00", "c@test.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void elClienteQueVuelveSeReconocePorSuEmail() throws Exception {
        mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), agustin.getId(), DIA, "10:00", "Lucas@Test.com")))
                .andExpect(status().isCreated());
        mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), agustin.getId(), DIA, "12:00", "lucas@test.com")))
                .andExpect(status().isCreated());

        assertThat(clientes.count()).isEqualTo(1);
    }

    @Test
    void noSeReservaEnElPasadoNiFueraDeHorario() throws Exception {
        mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), agustin.getId(), HOY, "10:30", "a@test.com")))
                .andExpect(status().isConflict());
        mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), agustin.getId(), DIA, "10:15", "a@test.com")))
                .andExpect(status().isConflict());   // fuera de la grilla de 30 min
        mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), agustin.getId(), DIA, "19:45", "a@test.com")))
                .andExpect(status().isConflict());
        mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), agustin.getId(), HOY.minusDays(1), "15:00", "a@test.com")))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void losDatosDelClienteSeValidan() throws Exception {
        String malo = """
                {"idServicio": %d, "fecha": "%s", "hora": "15:00",
                 "cliente": {"nombre": "", "apellido": "Paz", "email": "no-es-email", "telefono": "12"}}
                """.formatted(corte.getId(), DIA);
        mvc.perform(json(post("/api/v1/turnos"), malo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['cliente.nombre']").exists())
                .andExpect(jsonPath("$.errores['cliente.email']").exists())
                .andExpect(jsonPath("$.errores['cliente.telefono']").exists());
    }

    @Test
    void elClienteCancelaConElLinkYElHorarioSeLibera() throws Exception {
        String respuesta = mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), agustin.getId(), DIA, "15:00", "a@test.com")))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(respuesta, "$.tokenCancelacion");

        mvc.perform(get("/api/v1/turnos/cancelar/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelable").value(true))
                .andExpect(jsonPath("$.estado").value("pendiente"));

        mvc.perform(patch("/api/v1/turnos/cancelar/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("cancelado"))
                .andExpect(jsonPath("$.cancelable").value(false));

        // Token de un solo uso
        mvc.perform(patch("/api/v1/turnos/cancelar/" + token)).andExpect(status().isConflict());

        mvc.perform(json(post("/api/v1/turnos"), reserva(corte.getId(), agustin.getId(), DIA, "15:00", "b@test.com")))
                .andExpect(status().isCreated());
    }

    @Test
    void elLinkDeCancelacionVenceA48Horas() throws Exception {
        Turno t = turnoGuardado(agustin, corte, DIA, "15:00", EstadoTurno.PENDIENTE);
        t.setTokenVencimiento(AHORA.minusMinutes(1));
        turnos.save(t);

        mvc.perform(patch("/api/v1/turnos/cancelar/" + t.getTokenCancelacion()))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(patch("/api/v1/turnos/cancelar/token-inventado")).andExpect(status().isNotFound());
    }

    @Test
    void completarHabilitaLaEncuestaUnaSolaVez() throws Exception {
        Turno t = turnoGuardado(agustin, corte, HOY, "10:00", EstadoTurno.PENDIENTE);
        String dueno = login("agustin@test.com");

        mvc.perform(json(conToken(patch("/api/v1/turnos/" + t.getId() + "/estado"), dueno), """
                        {"estado": "completado"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("completado"));

        String tokenEncuesta = turnos.findById(t.getId()).orElseThrow().getTokenEncuesta();
        assertThat(tokenEncuesta).isNotBlank();

        mvc.perform(get("/api/v1/encuestas/" + t.getId()).param("token", "otro"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/encuestas/" + t.getId()).param("token", tokenEncuesta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respondida").value(false))
                .andExpect(jsonPath("$.servicio").value("Corte"));

        String respuesta = """
                {"calificacion": 5, "comentario": "Impecable"}""";
        mvc.perform(json(post("/api/v1/encuestas/" + t.getId()).param("token", tokenEncuesta), respuesta))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.respondida").value(true))
                .andExpect(jsonPath("$.calificacion").value(5));
        mvc.perform(json(post("/api/v1/encuestas/" + t.getId()).param("token", tokenEncuesta), respuesta))
                .andExpect(status().isConflict());

        // Un turno cerrado no cambia más de estado
        mvc.perform(json(conToken(patch("/api/v1/turnos/" + t.getId() + "/estado"), dueno), """
                        {"estado": "ausente"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void noSeCompletaUnTurnoQueTodaviaNoEmpezo() throws Exception {
        Turno t = turnoGuardado(agustin, corte, HOY, "15:00", EstadoTurno.PENDIENTE);
        mvc.perform(json(conToken(patch("/api/v1/turnos/" + t.getId() + "/estado"), login("agustin@test.com")), """
                        {"estado": "completado"}"""))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void cobrarUnTurnoYVerloEnElReporte() throws Exception {
        Turno cobrado = turnoGuardado(santiago, corteYBarba, HOY, "10:00", EstadoTurno.COMPLETADO);
        turnoGuardado(agustin, corte, HOY, "10:00", EstadoTurno.COMPLETADO);   // queda sin cobrar
        Turno pendiente = turnoGuardado(agustin, corte, HOY, "16:00", EstadoTurno.PENDIENTE);
        String dueno = login("agustin@test.com");

        mvc.perform(json(conToken(post("/api/v1/pagos"), dueno), """
                        {"idTurno": %d, "monto": 15000, "medio": "mercadopago"}""".formatted(cobrado.getId())))
                .andExpect(status().isCreated());
        mvc.perform(json(conToken(post("/api/v1/pagos"), dueno), """
                        {"idTurno": %d, "monto": 15000, "medio": "efectivo"}""".formatted(cobrado.getId())))
                .andExpect(status().isConflict());
        mvc.perform(json(conToken(post("/api/v1/pagos"), dueno), """
                        {"idTurno": %d, "monto": 9000, "medio": "efectivo"}""".formatted(pendiente.getId())))
                .andExpect(status().isUnprocessableContent());

        mvc.perform(conToken(get("/api/v1/pagos"), dueno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.cobrado").value(15000))
                .andExpect(jsonPath("$.resumen.cobros").value(1))
                .andExpect(jsonPath("$.resumen.paraLaCasa").value(7500))   // Santiago cobra 50 %
                .andExpect(jsonPath("$.resumen.sinCobrarMonto").value(9000))
                .andExpect(jsonPath("$.porMedio[?(@.medio == 'mercadopago')].porcentaje").value(100))
                .andExpect(jsonPath("$.liquidacion[?(@.nombre == 'Santiago')].leToca").value(7500))
                .andExpect(jsonPath("$.movimientos[0].pago").value(nullValue()));   // sin cobrar, primero
    }

    @Test
    void laAgendaListaLosTurnosDelDiaConSuCobro() throws Exception {
        turnoGuardado(agustin, corte, HOY, "10:00", EstadoTurno.COMPLETADO);
        turnoGuardado(santiago, corte, HOY, "12:00", EstadoTurno.PENDIENTE);
        turnoGuardado(santiago, corte, HOY.plusDays(1), "12:00", EstadoTurno.PENDIENTE);
        String dueno = login("agustin@test.com");

        mvc.perform(conToken(get("/api/v1/turnos"), dueno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].horaInicio").value("10:00"))
                .andExpect(jsonPath("$[0].cliente.nombre").value("Mateo"));

        mvc.perform(conToken(get("/api/v1/turnos").param("barbero", santiago.getId().toString())
                        .param("estado", "pendiente"), dueno))
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
