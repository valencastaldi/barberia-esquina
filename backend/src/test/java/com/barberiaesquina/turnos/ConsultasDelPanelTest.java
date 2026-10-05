package com.barberiaesquina.turnos;

import com.barberiaesquina.turnos.modelo.*;
import com.barberiaesquina.turnos.repositorio.EncuestaRepositorio;
import com.barberiaesquina.turnos.servicio.Tokens;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las pantallas Clientes y Dashboard del panel se arman con un solo pedido cada una:
 * dan lo mismo que los pedidos separados de antes, con menos consultas a la base.
 */
class ConsultasDelPanelTest extends PruebaDeIntegracion {

    @Autowired EntityManagerFactory emf;
    @Autowired EncuestaRepositorio encuestas;

    private final ObjectMapper json = new ObjectMapper();
    private String token;

    /** 12 clientes con distinta cantidad de visitas, ausencias y encuestas en los últimos dos meses. */
    @BeforeEach
    void variosClientes() throws Exception {
        for (int i = 0; i < 12; i++) {
            Cliente c = new Cliente();
            c.setNombre("Cliente" + i);
            c.setApellido("Apellido" + i);
            c.setEmail("c" + i + "@test.com");
            c.setTelefono("351 400-00" + (10 + i));
            c.setFechaAlta(AHORA.minusDays(80));
            clientes.save(c);
            for (int j = 0; j <= i % 6; j++) {
                EstadoTurno estado = j == 3 ? EstadoTurno.AUSENTE : EstadoTurno.COMPLETADO;
                Turno t = turno(c, j % 2 == 0 ? agustin : santiago, 5 + i * 3 + j * 7, estado);
                if (estado == EstadoTurno.COMPLETADO && j % 2 == 0) {
                    Encuesta e = new Encuesta();
                    e.setTurno(t);
                    e.setCalificacion(4 + i % 2);
                    e.setComentario("Muy bien " + i);
                    e.setFechaRespuesta(t.getFecha().atTime(20, 0));
                    encuestas.save(e);
                }
            }
        }
        token = login("agustin@test.com");
    }

    @Test
    void clientesTraeLosTotalesDeCadaFiltroEnLaMismaRespuesta() throws Exception {
        JsonNode pagina = pedir("/api/v1/clientes?filtro=todos&pagina=0&tamano=25");
        for (String filtro : new String[]{"todos", "frecuentes", "nuevos", "perdidos"}) {
            assertThat(pagina.at("/totales/" + filtro).asLong())
                    .as(filtro)
                    .isEqualTo(pedir("/api/v1/clientes?filtro=" + filtro + "&tamano=1").get("total").asLong());
        }
        // Buscar o filtrar cambia la lista, no las tarjetas.
        JsonNode buscando = pedir("/api/v1/clientes?q=Cliente1&filtro=frecuentes");
        assertThat(buscando.get("total").asLong()).isLessThan(pagina.get("total").asLong());
        assertThat(buscando.get("totales")).isEqualTo(pagina.get("totales"));
    }

    @Test
    void dashboardDaEnUnPedidoLoMismoQueLosTresSeparados() throws Exception {
        String r = "?desde=" + HOY.minusDays(29) + "&hasta=" + HOY;
        JsonNode completo = pedir("/api/v1/dashboard" + r);
        assertThat(completo.get("resumen")).isEqualTo(pedir("/api/v1/dashboard/resumen" + r));
        assertThat(completo.get("evolucion")).isEqualTo(pedir("/api/v1/dashboard/evolucion" + r));
        assertThat(completo.get("barberos")).isEqualTo(pedir("/api/v1/dashboard/barberos" + r));
        assertThat(completo.at("/resumen/encuestas").asLong()).isPositive();
    }

    @Test
    void cadaPantallaCuestaPocasConsultas() throws Exception {
        // Antes: Clientes hacía 5 pedidos (30 consultas) y el Dashboard 3 (12 consultas).
        assertThat(consultas("/api/v1/clientes?filtro=todos&pagina=0&tamano=25")).isLessThanOrEqualTo(6);
        assertThat(consultas("/api/v1/dashboard?desde=" + HOY.minusDays(29) + "&hasta=" + HOY)).isLessThanOrEqualTo(5);
    }

    // ------------------------------------------------------------------

    private Turno turno(Cliente c, Barbero b, int diasAtras, EstadoTurno estado) {
        Turno t = new Turno();
        t.setCliente(c);
        t.setServicio(corte);
        t.setBarbero(b);
        t.setFecha(HOY.minusDays(diasAtras));
        t.setHoraInicio(LocalTime.of(10, 0));
        t.setHoraFin(LocalTime.of(10, 30));
        t.setPrecio(corte.getPrecio());
        t.setEstado(estado);
        t.setTokenCancelacion(Tokens.nuevo());
        t.setTokenVencimiento(AHORA);
        t.setFechaCreacion(AHORA.minusDays(diasAtras + 1));
        return turnos.save(t);
    }

    private JsonNode pedir(String ruta) throws Exception {
        return json.readTree(mvc.perform(conToken(get(ruta), token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    /** Sentencias SQL que ejecuta un pedido, incluidas las de la sesión. */
    private long consultas(String ruta) throws Exception {
        Statistics st = emf.unwrap(SessionFactory.class).getStatistics();
        st.setStatisticsEnabled(true);
        try {
            st.clear();
            mvc.perform(conToken(get(ruta), token)).andExpect(status().isOk());
            return st.getPrepareStatementCount();
        } finally {
            st.setStatisticsEnabled(false);
        }
    }
}
