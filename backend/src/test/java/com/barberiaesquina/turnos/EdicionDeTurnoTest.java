package com.barberiaesquina.turnos;

import com.barberiaesquina.turnos.modelo.Encuesta;
import com.barberiaesquina.turnos.modelo.EstadoTurno;
import com.barberiaesquina.turnos.modelo.MedioPago;
import com.barberiaesquina.turnos.modelo.Turno;
import com.barberiaesquina.turnos.repositorio.EncuestaRepositorio;
import com.barberiaesquina.turnos.repositorio.PagoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** [Extensión] Corregir o borrar un turno completado, y que Pagos lo refleje. */
class EdicionDeTurnoTest extends PruebaDeIntegracion {

    @Autowired PagoRepositorio pagos;
    @Autowired EncuestaRepositorio encuestas;

    private Turno cobrado(String medio, int monto) throws Exception {
        Turno t = turnoGuardado(santiago, corte, HOY, "10:00", EstadoTurno.COMPLETADO);
        mvc.perform(json(conToken(post("/api/v1/pagos"), login("agustin@test.com")), """
                        {"idTurno": %d, "monto": %d, "medio": "%s"}""".formatted(t.getId(), monto, medio)))
                .andExpect(status().isCreated());
        return t;
    }

    @Test
    void elBarberoCorrigeSuTurnoCobradoYPagosLoRefleja() throws Exception {
        Turno t = cobrado("efectivo", 9000);

        mvc.perform(json(conToken(put("/api/v1/turnos/" + t.getId()), login("santiago@test.com")), """
                        {"idServicio": %d, "precio": 15000, "medio": "transferencia"}""".formatted(corteYBarba.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio.nombre").value(corteYBarba.getNombre()))
                .andExpect(jsonPath("$.precio").value(15000))
                .andExpect(jsonPath("$.pago.monto").value(15000))
                .andExpect(jsonPath("$.pago.medio").value("transferencia"));

        mvc.perform(conToken(get("/api/v1/pagos"), login("agustin@test.com")))
                .andExpect(jsonPath("$.resumen.cobrado").value(15000))
                .andExpect(jsonPath("$.porMedio[?(@.medio == 'transferencia')].porcentaje").value(100));
    }

    @Test
    void soloElDuenoPasaUnTurnoAOtroPeluquero() throws Exception {
        Turno t = cobrado("efectivo", 9000);
        String cuerpo = """
                {"idServicio": %d, "idBarbero": %d, "precio": 9000, "medio": "efectivo"}"""
                .formatted(corte.getId(), agustin.getId());

        mvc.perform(json(conToken(put("/api/v1/turnos/" + t.getId()), login("santiago@test.com")), cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(json(conToken(put("/api/v1/turnos/" + t.getId()), login("agustin@test.com")), cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barbero.id").value(agustin.getId()));
    }

    @Test
    void borrarUnTurnoCobradoLoSacaDePagos() throws Exception {
        Turno t = cobrado("mercadopago", 9000);
        Encuesta e = new Encuesta();
        e.setTurno(t);
        e.setCalificacion(5);
        e.setFechaRespuesta(AHORA);
        encuestas.save(e);
        String dueno = login("agustin@test.com");

        mvc.perform(conToken(delete("/api/v1/turnos/" + t.getId()), dueno))
                .andExpect(status().isNoContent());

        assertThat(turnos.findById(t.getId())).isEmpty();
        assertThat(pagos.count()).isZero();
        assertThat(encuestas.count()).isZero();
        mvc.perform(conToken(get("/api/v1/pagos"), dueno))
                .andExpect(jsonPath("$.resumen.cobrado").value(0))
                .andExpect(jsonPath("$.resumen.cobros").value(0))
                .andExpect(jsonPath("$.movimientos").isEmpty());
    }

    @Test
    void unBarberoNoBorraTurnosAjenos() throws Exception {
        Turno ajeno = turnoGuardado(agustin, corte, HOY, "10:00", EstadoTurno.COMPLETADO);
        mvc.perform(conToken(delete("/api/v1/turnos/" + ajeno.getId()), login("santiago@test.com")))
                .andExpect(status().isForbidden());
        assertThat(turnos.findById(ajeno.getId())).isPresent();
    }

    @Test
    void soloSeCorrigenOBorranTurnosCompletados() throws Exception {
        Turno pendiente = turnoGuardado(agustin, corte, HOY, "16:00", EstadoTurno.PENDIENTE);
        String dueno = login("agustin@test.com");

        mvc.perform(conToken(delete("/api/v1/turnos/" + pendiente.getId()), dueno))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(json(conToken(put("/api/v1/turnos/" + pendiente.getId()), dueno), """
                        {"idServicio": %d, "precio": 9000}""".formatted(corte.getId())))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void unTurnoCobradoNoQuedaSinMedioDePago() throws Exception {
        Turno t = cobrado("efectivo", 9000);
        mvc.perform(json(conToken(put("/api/v1/turnos/" + t.getId()), login("agustin@test.com")), """
                        {"idServicio": %d, "precio": 9000}""".formatted(corte.getId())))
                .andExpect(status().isUnprocessableContent());
        assertThat(pagos.findByTurnoId(t.getId()).orElseThrow().getMedio()).isEqualTo(MedioPago.EFECTIVO);
    }
}
