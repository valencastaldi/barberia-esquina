package com.barberiaesquina.turnos;

import com.barberiaesquina.turnos.modelo.Bloqueo;
import com.barberiaesquina.turnos.modelo.EstadoTurno;
import com.barberiaesquina.turnos.repositorio.BloqueoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-27: cálculo de horarios disponibles. Recordatorio: "ahora" es martes 22/09 a las 11:00. */
class DisponibilidadTest extends PruebaDeIntegracion {

    @Autowired BloqueoRepositorio bloqueos;

    @Test
    void hoyNoOfreceHorariosQueYaPasaron() throws Exception {
        mvc.perform(get("/api/v1/disponibilidad")
                        .param("servicio", corte.getId().toString())
                        .param("fecha", HOY.toString())
                        .param("barbero", agustin.getId().toString()))
                .andExpect(status().isOk())
                // De 10:00 a 19:30 cada 30 min: 20 horarios; los ocupados también vienen, marcados.
                .andExpect(jsonPath("$.slots", hasSize(20)))
                .andExpect(jsonPath("$.slots[0].hora").value("10:00"))
                .andExpect(jsonPath("$.slots[0].libre").value(false))
                .andExpect(jsonPath("$.slots[2].hora").value("11:00"))
                .andExpect(jsonPath("$.slots[2].libre").value(false))
                .andExpect(jsonPath("$.slots[3].hora").value("11:30"))
                .andExpect(jsonPath("$.slots[3].libre").value(true))
                .andExpect(jsonPath("$.slots[19].hora").value("19:30"));
    }

    @Test
    void unServicioLargoNoPuedeEmpezarSiPisaUnTurno() throws Exception {
        turnoGuardado(agustin, corte, HOY.plusDays(7), "14:00", EstadoTurno.PENDIENTE);

        // Corte + Barba dura 60 min: 13:30 y 14:00 chocan con el turno de 14:00 a 14:30.
        mvc.perform(get("/api/v1/disponibilidad")
                        .param("servicio", corteYBarba.getId().toString())
                        .param("fecha", HOY.plusDays(7).toString())
                        .param("barbero", agustin.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[?(@.hora == '13:00')].libre").value(true))
                .andExpect(jsonPath("$.slots[?(@.hora == '13:30')].libre").value(false))
                .andExpect(jsonPath("$.slots[?(@.hora == '14:00')].libre").value(false))
                .andExpect(jsonPath("$.slots[?(@.hora == '14:30')].libre").value(true))
                // El último inicio posible es 19:00, porque termina justo a las 20:00.
                .andExpect(jsonPath("$.slots[-1].hora").value("19:00"));
    }

    @Test
    void losCanceladosNoOcupanLugar() throws Exception {
        turnoGuardado(agustin, corte, HOY.plusDays(7), "14:00", EstadoTurno.CANCELADO);

        mvc.perform(get("/api/v1/disponibilidad")
                        .param("servicio", corte.getId().toString())
                        .param("fecha", HOY.plusDays(7).toString())
                        .param("barbero", agustin.getId().toString()))
                .andExpect(jsonPath("$.slots[?(@.hora == '14:00')].libre").value(true));
    }

    @Test
    void sinPeluqueroElegidoAlcanzaConQueUnoTengaLugar() throws Exception {
        turnoGuardado(agustin, corte, HOY.plusDays(7), "15:00", EstadoTurno.PENDIENTE);

        mvc.perform(get("/api/v1/disponibilidad")
                        .param("servicio", corte.getId().toString())
                        .param("fecha", HOY.plusDays(7).toString()))
                .andExpect(jsonPath("$.slots[?(@.hora == '15:00')].libre").value(true))
                .andExpect(jsonPath("$.slots[?(@.hora == '15:00')].barberos[*]", contains(santiago.getId().intValue())))
                .andExpect(jsonPath("$.slots[?(@.hora == '16:00')].barberos[*]",
                        containsInAnyOrder(agustin.getId().intValue(), santiago.getId().intValue())));
    }

    @Test
    void losLibresVienenOrdenadosPorQuienTieneMenosTrabajoEseDia() throws Exception {
        turnoGuardado(agustin, corte, HOY.plusDays(7), "10:00", EstadoTurno.PENDIENTE);
        turnoGuardado(agustin, corte, HOY.plusDays(7), "11:00", EstadoTurno.PENDIENTE);

        // Santiago no tiene turnos ese día: aparece primero y el front lo sugiere.
        mvc.perform(get("/api/v1/disponibilidad")
                        .param("servicio", corte.getId().toString())
                        .param("fecha", HOY.plusDays(7).toString()))
                .andExpect(jsonPath("$.slots[?(@.hora == '17:00')].barberos[*]",
                        contains(santiago.getId().intValue(), agustin.getId().intValue())));
    }

    @Test
    void unaFranjaBloqueadaNoSeOfrece() throws Exception {
        Bloqueo b = new Bloqueo();
        b.setBarbero(agustin);
        b.setFecha(HOY.plusDays(7));
        b.setHoraInicio(LocalTime.of(13, 0));
        b.setHoraFin(LocalTime.of(14, 0));
        bloqueos.save(b);

        mvc.perform(get("/api/v1/disponibilidad")
                        .param("servicio", corte.getId().toString())
                        .param("fecha", HOY.plusDays(7).toString())
                        .param("barbero", agustin.getId().toString()))
                .andExpect(jsonPath("$.slots[?(@.hora == '12:30')].libre").value(true))
                .andExpect(jsonPath("$.slots[?(@.hora == '13:00')].libre").value(false))
                .andExpect(jsonPath("$.slots[?(@.hora == '13:30')].libre").value(false))
                .andExpect(jsonPath("$.slots[?(@.hora == '14:00')].libre").value(true));
    }

    @Test
    void unDiaQueNadieAtiendeNoTieneHorarios() throws Exception {
        mvc.perform(get("/api/v1/disponibilidad")
                        .param("servicio", corte.getId().toString())
                        .param("fecha", HOY.plusDays(5).toString()))   // domingo
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", empty()));
    }

    @Test
    void noSeOfrecenDiasPasadosNiDemasiadoLejanos() throws Exception {
        mvc.perform(get("/api/v1/disponibilidad")
                        .param("servicio", corte.getId().toString())
                        .param("fecha", HOY.minusDays(7).toString()))
                .andExpect(jsonPath("$.slots", empty()));
        mvc.perform(get("/api/v1/disponibilidad")
                        .param("servicio", corte.getId().toString())
                        .param("fecha", HOY.plusDays(35).toString()))
                .andExpect(jsonPath("$.slots", empty()));
    }

    @Test
    void elSelectorDeDiasCuentaLosLibres() throws Exception {
        mvc.perform(get("/api/v1/disponibilidad/dias")
                        .param("servicio", corte.getId().toString())
                        .param("barbero", agustin.getId().toString())
                        .param("cantidad", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].fecha").value(HOY.toString()))
                .andExpect(jsonPath("$[0].atiende").value(true))
                .andExpect(jsonPath("$[0].libres").value(17))    // 11:30 a 19:30
                .andExpect(jsonPath("$[1].atiende").value(false)); // miércoles: no hay horario cargado
    }

    @Test
    void unPeluqueroQueNoHaceElServicioDaError() throws Exception {
        var nino = servicio("Corte Niño", 30, 7500);
        mvc.perform(get("/api/v1/disponibilidad")
                        .param("servicio", nino.getId().toString())
                        .param("fecha", HOY.toString())
                        .param("barbero", agustin.getId().toString()))
                .andExpect(status().isUnprocessableContent());
    }
}
