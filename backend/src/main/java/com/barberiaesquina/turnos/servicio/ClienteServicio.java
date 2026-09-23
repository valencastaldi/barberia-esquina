package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.modelo.Cliente;
import com.barberiaesquina.turnos.modelo.EstadoTurno;
import com.barberiaesquina.turnos.repositorio.ClienteRepositorio;
import com.barberiaesquina.turnos.repositorio.TurnoRepositorio;
import com.barberiaesquina.turnos.servicio.excepcion.NoEncontradoException;
import com.barberiaesquina.turnos.servicio.excepcion.ReglaNegocioException;
import com.barberiaesquina.turnos.web.dto.ClienteDtos.Ficha;
import com.barberiaesquina.turnos.web.dto.ClienteDtos.Pagina;
import com.barberiaesquina.turnos.web.dto.ClienteDtos.Resumen;
import com.barberiaesquina.turnos.web.dto.TurnoDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

/** [Extensión] Clientes: solo lectura sobre la tabla Cliente que ya define la documentación. */
@Service
@Transactional(readOnly = true)
public class ClienteServicio {

    /** Filtros de la pantalla de Clientes. */
    public enum Filtro { TODOS, FRECUENTES, NUEVOS, AUSENCIAS, PERDIDOS }

    private final ClienteRepositorio clientes;
    private final TurnoRepositorio turnos;
    private final TurnoServicio turnoServicio;
    private final Calendario calendario;

    public ClienteServicio(ClienteRepositorio clientes, TurnoRepositorio turnos, TurnoServicio turnoServicio,
                           Calendario calendario) {
        this.clientes = clientes;
        this.turnos = turnos;
        this.turnoServicio = turnoServicio;
        this.calendario = calendario;
    }

    public Pagina listar(String busqueda, Filtro filtro, int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > 100) throw new ReglaNegocioException("Paginación inválida");

        Map<Long, Acumulado> acumulados = acumular();
        Set<Long> conProximoTurno = new HashSet<>(turnos.clientesConTurnoDesde(EstadoTurno.PENDIENTE, calendario.hoy()));

        List<Resumen> todos = clientes.findAll().stream()
                .map(c -> acumulados.getOrDefault(c.getId(), new Acumulado()).resumen(c))
                .filter(coincide(busqueda))
                .filter(filtro(filtro == null ? Filtro.TODOS : filtro, conProximoTurno))
                .sorted(Comparator.comparing(Resumen::ultimaVisita, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Resumen::visitas, Comparator.reverseOrder()))
                .toList();

        int desde = Math.min(pagina * tamano, todos.size());
        int hasta = Math.min(desde + tamano, todos.size());
        return new Pagina(todos.subList(desde, hasta), todos.size(), pagina, tamano);
    }

    public Ficha ficha(Long idCliente) {
        Cliente c = clientes.findById(idCliente).orElseThrow(() -> NoEncontradoException.de("Cliente", idCliente));
        List<TurnoDtos.Detalle> historial = turnoServicio.detalles(turnos.deCliente(idCliente));

        Acumulado acc = new Acumulado();
        Map<Long, TurnoDtos.Ref> barberosVistos = new HashMap<>();
        for (TurnoDtos.Detalle t : historial) {
            acc.sumar(t.estado(), 1, t.precio(), t.fecha(), t.fecha());
            if (t.estado() == EstadoTurno.COMPLETADO) {
                acc.atenciones.merge(t.barbero().id(), 1L, Long::sum);
                barberosVistos.put(t.barbero().id(), t.barbero());
            }
        }
        acc.nombresBarberos.putAll(barberosVistos);

        LocalDate hoy = calendario.hoy();
        TurnoDtos.Detalle proximo = historial.stream()
                .filter(t -> t.estado() == EstadoTurno.PENDIENTE && !t.fecha().isBefore(hoy))
                .min(Comparator.comparing(TurnoDtos.Detalle::fecha).thenComparing(TurnoDtos.Detalle::horaInicio))
                .orElse(null);
        Double satisfaccion = historial.stream()
                .map(TurnoDtos.Detalle::calificacion).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).average().stream().boxed().findFirst().orElse(null);

        return new Ficha(acc.resumen(c), satisfaccion, proximo, historial);
    }

    // ------------------------------------------------------------------

    private Map<Long, Acumulado> acumular() {
        Map<Long, Acumulado> porCliente = new HashMap<>();
        for (Object[] fila : turnos.agregadoPorClienteYEstado()) {
            porCliente.computeIfAbsent((Long) fila[0], id -> new Acumulado())
                    .sumar((EstadoTurno) fila[1], (Long) fila[2], (BigDecimal) fila[3],
                            (LocalDate) fila[4], (LocalDate) fila[5]);
        }
        for (Object[] fila : turnos.atencionesPorClienteYBarbero(EstadoTurno.COMPLETADO)) {
            Acumulado acc = porCliente.get((Long) fila[0]);
            if (acc == null) continue;
            acc.atenciones.put((Long) fila[1], (Long) fila[3]);
            acc.nombresBarberos.put((Long) fila[1], new TurnoDtos.Ref((Long) fila[1], (String) fila[2]));
        }
        return porCliente;
    }

    private Predicate<Resumen> coincide(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) return r -> true;
        String q = busqueda.trim().toLowerCase(Locale.ROOT);
        String digitos = q.replaceAll("\\D", "");
        return r -> (r.nombre() + " " + r.apellido()).toLowerCase(Locale.ROOT).contains(q)
                || r.email().toLowerCase(Locale.ROOT).contains(q)
                || (!digitos.isEmpty() && r.telefono().replaceAll("\\D", "").contains(digitos));
    }

    private Predicate<Resumen> filtro(Filtro filtro, Set<Long> conProximoTurno) {
        LocalDate hoy = calendario.hoy();
        return switch (filtro) {
            case TODOS -> r -> true;
            case FRECUENTES -> r -> r.visitas() >= 4;
            case NUEVOS -> r -> r.primeraVisita() != null && !r.primeraVisita().isBefore(hoy.minusDays(30));
            case AUSENCIAS -> r -> r.ausencias() > 0;
            // Clientes que venían seguido, no vuelven hace más de 3 semanas y no tienen turno sacado.
            case PERDIDOS -> r -> r.visitas() >= 3 && r.ultimaVisita() != null
                    && r.ultimaVisita().isBefore(hoy.minusDays(21)) && !conProximoTurno.contains(r.id());
        };
    }

    /** Suma lo de un cliente a partir de sus turnos (o de los agregados de la consulta). */
    private static final class Acumulado {
        long visitas;
        long ausencias;
        BigDecimal gastado = BigDecimal.ZERO;
        LocalDate ultimaVisita;
        LocalDate primeraFecha;
        final Map<Long, Long> atenciones = new HashMap<>();
        final Map<Long, TurnoDtos.Ref> nombresBarberos = new HashMap<>();

        void sumar(EstadoTurno estado, long cantidad, BigDecimal monto, LocalDate ultima, LocalDate primera) {
            if (estado == EstadoTurno.COMPLETADO) {
                visitas += cantidad;
                gastado = gastado.add(monto);
                if (ultimaVisita == null || ultima.isAfter(ultimaVisita)) ultimaVisita = ultima;
            }
            if (estado == EstadoTurno.AUSENTE) ausencias += cantidad;
            if (primeraFecha == null || primera.isBefore(primeraFecha)) primeraFecha = primera;
        }

        Resumen resumen(Cliente c) {
            TurnoDtos.Ref habitual = atenciones.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> nombresBarberos.get(e.getKey()))
                    .orElse(null);
            return new Resumen(c.getId(), c.getNombre(), c.getApellido(), c.getEmail(), c.getTelefono(),
                    visitas, ausencias, gastado, ultimaVisita, primeraFecha, habitual);
        }
    }
}
