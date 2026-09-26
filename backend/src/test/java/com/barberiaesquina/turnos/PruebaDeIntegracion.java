package com.barberiaesquina.turnos;

import com.barberiaesquina.turnos.modelo.*;
import com.barberiaesquina.turnos.repositorio.*;
import com.barberiaesquina.turnos.seguridad.LimiteDeLoginFiltro;
import com.barberiaesquina.turnos.seguridad.LimiteDeReservasFiltro;
import com.barberiaesquina.turnos.servicio.Tokens;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.*;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de los tests de integración: levanta la API completa sobre H2 con un
 * reloj fijo en el martes 22/09/2026 a las 11:00 de Córdoba.
 * Antes de cada test la base queda vacía y se carga una barbería mínima:
 *  - Corte (30 min, $9000) y Corte + Barba (60 min, $15000)
 *  - Agustín (dueño) y Santiago (barbero), los dos atienden martes de 10 a 20
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PruebaDeIntegracion.RelojFijo.class)
public abstract class PruebaDeIntegracion {

    protected static final ZoneId CORDOBA = ZoneId.of("America/Argentina/Cordoba");
    protected static final LocalDate HOY = LocalDate.of(2026, 9, 22);        // martes
    protected static final LocalDateTime AHORA = HOY.atTime(11, 0);
    protected static final String PASSWORD = "clave-de-prueba";

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(AHORA.atZone(CORDOBA).toInstant(), CORDOBA);
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected LimiteDeReservasFiltro limiteDeReservas;
    @Autowired protected LimiteDeLoginFiltro limiteDeLogin;
    @Autowired protected BarberoRepositorio barberos;
    @Autowired protected ServicioRepositorio servicios;
    @Autowired protected HorarioRepositorio horarios;
    @Autowired protected ClienteRepositorio clientes;
    @Autowired protected TurnoRepositorio turnos;

    protected Servicio corte;
    protected Servicio corteYBarba;
    protected Barbero agustin;
    protected Barbero santiago;

    @BeforeEach
    void baseLimpia() {
        for (String tabla : new String[]{"token_revocado", "pago", "encuesta", "bloqueo", "turno", "cliente", "horario_atencion",
                "barbero_servicio", "servicio", "barbero"}) {
            jdbc.update("delete from " + tabla);
        }
        limiteDeReservas.reiniciar();
        limiteDeLogin.reiniciar();

        corte = servicio("Corte", 30, 9000);
        corteYBarba = servicio("Corte + Barba", 60, 15000);
        agustin = barbero("Agustín", "agustin@test.com", Rol.DUENO, 0);
        santiago = barbero("Santiago", "santiago@test.com", Rol.BARBERO, 50);
        horario(agustin, 2, "10:00", "20:00");
        horario(santiago, 2, "10:00", "20:00");
    }

    // ---------- Datos ----------

    protected Servicio servicio(String nombre, int minutos, int precio) {
        Servicio s = new Servicio();
        s.setNombre(nombre);
        s.setDuracionMinutos(minutos);
        s.setPrecio(BigDecimal.valueOf(precio));
        return servicios.save(s);
    }

    protected Barbero barbero(String nombre, String email, Rol rol, int comision) {
        Barbero b = new Barbero();
        b.setNombre(nombre);
        b.setApellido("Test");
        b.setEmail(email);
        b.setPasswordHash(passwordEncoder.encode(PASSWORD));
        b.setRol(rol);
        b.setComisionPct(comision);
        b.setFechaAlta(AHORA);
        b.setServicios(new HashSet<>(Set.of(corte, corteYBarba)));
        return barberos.save(b);
    }

    protected void horario(Barbero b, int dia, String inicio, String fin) {
        HorarioAtencion h = new HorarioAtencion();
        h.setBarbero(b);
        h.setDiaSemana(dia);
        h.setHoraInicio(LocalTime.parse(inicio));
        h.setHoraFin(LocalTime.parse(fin));
        h.setDuracionSlotMin(30);
        horarios.save(h);
    }

    /** Guarda un turno directo en la base (sin pasar por la API). */
    protected Turno turnoGuardado(Barbero b, Servicio s, LocalDate fecha, String hora, EstadoTurno estado) {
        Cliente c = clientes.findByEmailIgnoreCase("cliente@test.com").orElseGet(() -> {
            Cliente nuevo = new Cliente();
            nuevo.setNombre("Mateo");
            nuevo.setApellido("Giménez");
            nuevo.setEmail("cliente@test.com");
            nuevo.setTelefono("351 415-2233");
            nuevo.setFechaAlta(AHORA);
            return clientes.save(nuevo);
        });
        Turno t = new Turno();
        t.setCliente(c);
        t.setServicio(s);
        t.setBarbero(b);
        t.setFecha(fecha);
        t.setHoraInicio(LocalTime.parse(hora));
        t.setHoraFin(LocalTime.parse(hora).plusMinutes(s.getDuracionMinutos()));
        t.setPrecio(s.getPrecio());
        t.setEstado(estado);
        t.setTokenCancelacion(Tokens.nuevo());
        t.setTokenVencimiento(AHORA.plusHours(48));
        t.setFechaCreacion(AHORA.minusDays(1));
        return turnos.save(t);
    }

    // ---------- API ----------

    protected String login(String email) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    protected static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder pedido, String token) {
        return pedido.header("Authorization", "Bearer " + token);
    }

    protected static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder pedido, String cuerpo) {
        return pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    protected static String reserva(Long idServicio, Long idBarbero, LocalDate fecha, String hora, String email) {
        return """
                {"idServicio": %d, "idBarbero": %s, "fecha": "%s", "hora": "%s",
                 "cliente": {"nombre": "Lucas", "apellido": "Ferreyra", "email": "%s", "telefono": "351 711-0043"}}
                """.formatted(idServicio, idBarbero == null ? "null" : idBarbero.toString(), fecha, hora, email);
    }
}
