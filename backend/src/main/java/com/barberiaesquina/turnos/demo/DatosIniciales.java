package com.barberiaesquina.turnos.demo;

import com.barberiaesquina.turnos.config.AppProperties;
import com.barberiaesquina.turnos.modelo.*;
import com.barberiaesquina.turnos.repositorio.*;
import com.barberiaesquina.turnos.servicio.Calendario;
import com.barberiaesquina.turnos.servicio.Tokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Al arrancar:
 *  1. Si no hay ningún peluquero, crea al dueño con app.admin (siempre).
 *  2. Si app.datos-demo = true y no hay servicios, carga una barbería de ejemplo
 *     con 45 días de historial y la semana que viene, para desarrollar el front.
 */
@Component
public class DatosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final AppProperties props;
    private final PasswordEncoder passwordEncoder;
    private final Calendario calendario;
    private final BarberoRepositorio barberos;
    private final ServicioRepositorio servicios;
    private final HorarioRepositorio horarios;
    private final ClienteRepositorio clientes;
    private final TurnoRepositorio turnos;
    private final PagoRepositorio pagos;
    private final EncuestaRepositorio encuestas;

    public DatosIniciales(AppProperties props, PasswordEncoder passwordEncoder, Calendario calendario,
                          BarberoRepositorio barberos, ServicioRepositorio servicios, HorarioRepositorio horarios,
                          ClienteRepositorio clientes, TurnoRepositorio turnos, PagoRepositorio pagos,
                          EncuestaRepositorio encuestas) {
        this.props = props;
        this.passwordEncoder = passwordEncoder;
        this.calendario = calendario;
        this.barberos = barberos;
        this.servicios = servicios;
        this.horarios = horarios;
        this.clientes = clientes;
        this.turnos = turnos;
        this.pagos = pagos;
        this.encuestas = encuestas;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (barberos.count() == 0) {
            var a = props.admin();
            Barbero dueno = barbero(a.nombre(), a.apellido(), a.email(), Rol.DUENO, 0);
            dueno.setPasswordHash(passwordEncoder.encode(a.password()));
            barberos.save(dueno);
            log.info("Se creó el usuario dueño {}", a.email());
        }
        if (props.datosDemo() && servicios.count() == 0) {
            cargarDemo();
        }
    }

    // ------------------------------------------------------------------

    private void cargarDemo() {
        Random azar = new Random(1290);

        Servicio corto = servicio("Corte Corto", "Máquina y tijera, lavado incluido", 30, 9000, true);
        Servicio largo = servicio("Corte Largo", "Tijera, texturizado y peinado", 45, 11000, true);
        Servicio barba = servicio("Corte + Barba", "Corte completo, perfilado y toalla caliente", 60, 15000, true);
        Servicio perfilado = servicio("Perfilado de Barba", "Diseño, navaja y aceite", 30, 7000, true);
        Servicio nino = servicio("Corte Niño", "Hasta 12 años", 30, 7500, false);

        Barbero agustin = barberos.findAll().getFirst();
        agustin.getServicios().addAll(List.of(corto, largo, barba, perfilado, nino));
        Barbero santiago = barbero("Santiago", "Molina", "santiago@barberiaesquina.com", Rol.BARBERO, 50);
        santiago.getServicios().addAll(List.of(corto, largo, barba, perfilado));
        Barbero joaquin = barbero("Joaquín", "Vera", "joaquin@barberiaesquina.com", Rol.BARBERO, 45);
        joaquin.getServicios().addAll(List.of(corto, perfilado, nino));
        String hash = passwordEncoder.encode(props.admin().password());
        santiago.setPasswordHash(hash);
        joaquin.setPasswordHash(hash);
        barberos.saveAll(List.of(santiago, joaquin));

        // Lun-Mié 10-20 · Jue-Vie 10-21 · Sáb 9-18 · Dom cerrado (como la maqueta)
        semana(agustin, Set.of(1, 2, 3, 4, 5, 6));
        semana(santiago, Set.of(2, 3, 4, 5, 6));
        semana(joaquin, Set.of(1, 2, 4, 5, 6));

        List<Cliente> base = clientesDemo();
        List<Barbero> equipo = List.of(agustin, santiago, joaquin);
        LocalDateTime ahora = calendario.ahora();
        int creados = 0;

        for (int d = -45; d <= 7; d++) {
            LocalDate fecha = calendario.hoy().plusDays(d);
            for (Barbero b : equipo) {
                var horario = horarios.findByBarberoIdAndDiaSemana(b.getId(), Calendario.diaSemana(fecha));
                if (horario.isEmpty()) continue;
                List<Servicio> suyos = b.getServicios().stream().filter(Servicio::isActivo)
                        .sorted(Comparator.comparing(Servicio::getId)).toList();
                List<LocalTime[]> ocupado = new ArrayList<>();
                int cantidad = d > 0 ? 1 + azar.nextInt(4) : 3 + azar.nextInt(5);

                for (int intento = 0; intento < cantidad * 3 && ocupado.size() < cantidad; intento++) {
                    Servicio s = suyos.get(azar.nextInt(suyos.size()));
                    int bloques = (int) java.time.Duration.between(horario.get().getHoraInicio(), horario.get().getHoraFin()).toMinutes() / 30;
                    LocalTime inicio = horario.get().getHoraInicio().plusMinutes(30L * azar.nextInt(bloques));
                    LocalTime fin = inicio.plusMinutes(s.getDuracionMinutos());
                    if (fin.isAfter(horario.get().getHoraFin())) continue;
                    if (ocupado.stream().anyMatch(o -> o[0].isBefore(fin) && inicio.isBefore(o[1]))) continue;
                    ocupado.add(new LocalTime[]{inicio, fin});

                    Turno t = turno(base.get(elegirCliente(azar, base.size())), s, b, fecha, inicio, fin);
                    boolean yaPaso = fecha.atTime(inicio).isBefore(ahora);
                    t.setEstado(yaPaso ? estadoPasado(azar) : (azar.nextInt(20) == 0 ? EstadoTurno.CANCELADO : EstadoTurno.PENDIENTE));
                    turnos.save(t);
                    creados++;

                    if (t.getEstado() == EstadoTurno.COMPLETADO) {
                        t.setTokenEncuesta(Tokens.nuevo());
                        // Casi todo cobrado; algunos de hoy quedan pendientes de cobro.
                        if (d < 0 || azar.nextInt(3) > 0) pago(t, azar);
                        if (azar.nextInt(100) < 55) encuesta(t, azar);
                    }
                }
            }
        }
        log.info("Datos de demostración cargados: 5 servicios, 3 peluqueros, {} clientes, {} turnos", base.size(), creados);
    }

    private static final String[] COMENTARIOS = {
            "Excelente atención, como siempre.", "Muy buen corte, volveré.", "Puntuales y prolijos.",
            "Me encantó el perfilado.", "Buena onda y buen precio.", "Tuve que esperar un poco pero quedó bien.",
            "El mejor fade de Córdoba.", null, null, null};

    private static EstadoTurno estadoPasado(Random azar) {
        int r = azar.nextInt(100);
        return r < 82 ? EstadoTurno.COMPLETADO : r < 89 ? EstadoTurno.AUSENTE : EstadoTurno.CANCELADO;
    }

    /** Los primeros 20 clientes son habitués (vienen cada una o dos semanas); el resto, de vez en cuando. */
    private static int elegirCliente(Random azar, int total) {
        return azar.nextInt(100) < 25 ? azar.nextInt(20) : azar.nextInt(total);
    }

    private Servicio servicio(String nombre, String descripcion, int minutos, int precio, boolean activo) {
        Servicio s = new Servicio();
        s.setNombre(nombre);
        s.setDescripcion(descripcion);
        s.setDuracionMinutos(minutos);
        s.setPrecio(BigDecimal.valueOf(precio));
        s.setActivo(activo);
        return servicios.save(s);
    }

    private Barbero barbero(String nombre, String apellido, String email, Rol rol, int comision) {
        Barbero b = new Barbero();
        b.setNombre(nombre);
        b.setApellido(apellido);
        b.setEmail(email);
        b.setRol(rol);
        b.setComisionPct(comision);
        b.setFechaAlta(calendario.ahora());
        return b;
    }

    private void semana(Barbero b, Set<Integer> dias) {
        for (int dia : dias) {
            HorarioAtencion h = new HorarioAtencion();
            h.setBarbero(b);
            h.setDiaSemana(dia);
            h.setHoraInicio(LocalTime.of(dia == 6 ? 9 : 10, 0));
            h.setHoraFin(LocalTime.of(dia == 6 ? 18 : dia >= 4 ? 21 : 20, 0));
            h.setDuracionSlotMin(30);
            horarios.save(h);
        }
    }

    private List<Cliente> clientesDemo() {
        String[] nombres = {"Mateo Giménez", "Bruno Actis", "Nicolás Paz", "Lucas Ferreyra", "Tomás Quiroga",
                "Iván Ledesma", "Franco Britos", "Julián Sosa", "Ramiro Castro", "Ezequiel Moreno", "Facundo Oviedo",
                "Gonzalo Luna", "Valentín Herrera", "Lautaro Díaz", "Martín Cabrera", "Santino Rojas",
                "Benjamín Suárez", "Thiago Romero", "Emanuel Acosta", "Diego Villalba", "Leandro Funes",
                "Maximiliano Ortiz", "Alejo Aguirre", "Bautista Bustos", "Camilo Carranza", "Dante Domínguez",
                "Elías Escudero", "Felipe Figueroa", "Gael Godoy", "Hernán Heredia", "Ignacio Juárez",
                "Juan Cruz Lucero", "Lisandro Maldonado", "Marcos Navarro", "Nahuel Olmedo", "Octavio Pereyra",
                "Pablo Quinteros", "Renzo Ramírez", "Sebastián Sánchez", "Tobías Toledo"};
        // Más clientes ocasionales, combinando nombres y apellidos.
        String[] pila = {"Agustín", "Alan", "Bruno", "Ciro", "Enzo", "Fausto", "Gino", "Iker", "Joel", "León", "Máximo"};
        String[] apellidos = {"Arias", "Bravo", "Córdoba", "Farías", "Gómez", "Ibarra", "Luján", "Medina", "Ponce", "Ríos"};
        List<String> todos = new ArrayList<>(List.of(nombres));
        for (String n : pila) for (String a : apellidos) todos.add(n + " " + a);

        List<Cliente> lista = new ArrayList<>();
        for (int i = 0; i < todos.size(); i++) {
            String[] partes = todos.get(i).split(" ", 2);
            // "Juan Cruz Lucero": nombre compuesto
            String nombre = partes[0], apellido = partes[1];
            if (apellido.contains(" ")) {
                nombre = partes[0] + " " + apellido.substring(0, apellido.indexOf(' '));
                apellido = apellido.substring(apellido.indexOf(' ') + 1);
            }
            Cliente c = new Cliente();
            c.setNombre(nombre);
            c.setApellido(apellido);
            c.setEmail(java.text.Normalizer.normalize(nombre + "." + apellido, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").replace(" ", "").toLowerCase() + "@gmail.com");
            c.setTelefono("351 " + (200 + i * 17 % 800) + "-" + (1000 + i * 263 % 9000));
            c.setFechaAlta(calendario.ahora().minusDays(60));
            lista.add(clientes.save(c));
        }
        return lista;
    }

    private Turno turno(Cliente c, Servicio s, Barbero b, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        Turno t = new Turno();
        t.setCliente(c);
        t.setServicio(s);
        t.setBarbero(b);
        t.setFecha(fecha);
        t.setHoraInicio(inicio);
        t.setHoraFin(fin);
        t.setPrecio(s.getPrecio());
        t.setTokenCancelacion(Tokens.nuevo());
        LocalDateTime creado = fecha.atStartOfDay().minusDays(3).plusHours(12);
        t.setFechaCreacion(creado);
        t.setTokenVencimiento(creado.plusHours(props.turnos().cancelacionHoras()));
        return t;
    }

    private void pago(Turno t, Random azar) {
        Pago p = new Pago();
        p.setTurno(t);
        p.setMonto(t.getPrecio());
        int r = azar.nextInt(100);
        p.setMedio(r < 40 ? MedioPago.EFECTIVO : r < 75 ? MedioPago.TRANSFERENCIA : MedioPago.MERCADOPAGO);
        p.setFecha(t.getFecha().atTime(t.getHoraFin()));
        pagos.save(p);
    }

    private void encuesta(Turno t, Random azar) {
        Encuesta e = new Encuesta();
        e.setTurno(t);
        int r = azar.nextInt(100);
        e.setCalificacion(r < 58 ? 5 : r < 86 ? 4 : r < 95 ? 3 : r < 98 ? 2 : 1);
        e.setComentario(COMENTARIOS[azar.nextInt(COMENTARIOS.length)]);
        e.setFechaRespuesta(t.getFecha().atTime(t.getHoraFin()).plusHours(2));
        encuestas.save(e);
    }
}
