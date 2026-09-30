package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.modelo.*;
import com.barberiaesquina.turnos.repositorio.*;
import com.barberiaesquina.turnos.servicio.excepcion.NoEncontradoException;
import com.barberiaesquina.turnos.servicio.excepcion.ReglaNegocioException;
import com.barberiaesquina.turnos.web.dto.DisponibilidadDtos.Dia;
import com.barberiaesquina.turnos.web.dto.DisponibilidadDtos.ResumenDia;
import com.barberiaesquina.turnos.web.dto.DisponibilidadDtos.Slot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CalcularSlotsDisponibles (RF-27). Un horario está libre para un peluquero si:
 *  - cae dentro de su horario de atención de ese día y sobre la grilla de slots,
 *  - el servicio entero termina antes del cierre,
 *  - no se superpone con un turno no cancelado ni con una franja bloqueada,
 *  - no pasó todavía (si es hoy).
 * Sin peluquero elegido, un horario está libre si al menos uno lo tiene libre.
 */
@Service
@Transactional(readOnly = true)
public class DisponibilidadServicio {

    private final ServicioRepositorio servicios;
    private final BarberoRepositorio barberos;
    private final HorarioRepositorio horarios;
    private final TurnoRepositorio turnos;
    private final BloqueoRepositorio bloqueos;
    private final Calendario calendario;

    public DisponibilidadServicio(ServicioRepositorio servicios, BarberoRepositorio barberos,
                                  HorarioRepositorio horarios, TurnoRepositorio turnos,
                                  BloqueoRepositorio bloqueos, Calendario calendario) {
        this.servicios = servicios;
        this.barberos = barberos;
        this.horarios = horarios;
        this.turnos = turnos;
        this.bloqueos = bloqueos;
        this.calendario = calendario;
    }

    public Dia delDia(LocalDate fecha, Long idServicio, Long idBarbero) {
        Servicio servicio = servicioReservable(idServicio);
        List<Slot> slots = fueraDeRango(fecha) ? List.of()
                : calcularSlots(agendas(candidatos(servicio, idBarbero), fecha, fecha, servicio.getDuracionMinutos()).delDia(fecha));
        return new Dia(fecha, servicio.getId(), servicio.getDuracionMinutos(), slots);
    }

    /**
     * Para el selector de días del paso 2: cuántos horarios libres tiene cada día. El primero
     * con lugar trae también sus horarios, que ya están calculados: es el día que la pantalla
     * muestra al entrar y así no necesita un segundo pedido.
     */
    public List<ResumenDia> proximosDias(Long idServicio, Long idBarbero, int cantidad) {
        Servicio servicio = servicioReservable(idServicio);
        LocalDate desde = calendario.hoy();
        LocalDate hasta = desde.plusDays(cantidad - 1L);
        if (hasta.isAfter(calendario.ultimoDiaReservable())) hasta = calendario.ultimoDiaReservable();
        Agendas agendas = agendas(candidatos(servicio, idBarbero), desde, hasta, servicio.getDuracionMinutos());

        List<ResumenDia> dias = new ArrayList<>();
        boolean conHorarios = false;
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            List<Slot> slots = calcularSlots(agendas.delDia(fecha));
            int libres = (int) slots.stream().filter(Slot::libre).count();
            boolean primeroConLugar = libres > 0 && !conHorarios;
            dias.add(new ResumenDia(fecha, !slots.isEmpty(), libres, primeroConLugar ? slots : null));
            conHorarios |= primeroConLugar;
        }
        return dias;
    }

    /**
     * ¿El peluquero puede atender este servicio en ese horario? Se vuelve a
     * preguntar justo antes de guardar la reserva (RNF: validación en tiempo real).
     */
    public boolean estaLibre(Barbero barbero, Servicio servicio, LocalDate fecha, LocalTime inicio) {
        if (fueraDeRango(fecha)) return false;
        return agendaDelDia(barbero, fecha, servicio.getDuracionMinutos())
                .map(agenda -> agenda.libre(inicio))
                .orElse(false);
    }

    /** Peluqueros activos que hacen el servicio (o solo el elegido, si hace el servicio). */
    public List<Barbero> candidatos(Servicio servicio, Long idBarbero) {
        List<Barbero> activos = barberos.findByActivoTrueOrderByIdAsc().stream()
                .filter(b -> b.haceServicio(servicio.getId()))
                .toList();
        if (idBarbero == null) return activos;
        return activos.stream().filter(b -> b.getId().equals(idBarbero)).findFirst()
                .map(List::of)
                .orElseThrow(() -> new ReglaNegocioException("Ese peluquero no hace " + servicio.getNombre()));
    }

    public Servicio servicioReservable(Long idServicio) {
        Servicio servicio = servicios.findById(idServicio)
                .orElseThrow(() -> NoEncontradoException.de("Servicio", idServicio));
        if (!servicio.isActivo()) throw new ReglaNegocioException("El servicio no está disponible");
        return servicio;
    }

    public boolean fueraDeRango(LocalDate fecha) {
        return fecha.isBefore(calendario.hoy()) || fecha.isAfter(calendario.ultimoDiaReservable());
    }

    // ---------------------------------------------------------------

    /** Los horarios de un día, cada uno con los peluqueros que lo tienen libre. */
    private static List<Slot> calcularSlots(Map<Barbero, Agenda> agendas) {
        // Primero el que tiene menos turnos ese día: la lista de libres de cada horario sale
        // en ese orden y el front sugiere al primero, así el trabajo se reparte.
        List<Map.Entry<Barbero, Agenda>> porCarga = agendas.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Barbero, Agenda> e) -> e.getValue().ocupados().size())
                        .thenComparing(e -> e.getKey().getId()))
                .toList();

        // hora -> peluqueros libres a esa hora (TreeMap: queda ordenado)
        Map<LocalTime, List<Long>> porHora = new TreeMap<>();
        for (var entrada : porCarga) {
            Agenda agenda = entrada.getValue();
            for (LocalTime hora : agenda.inicios()) {
                List<Long> libres = porHora.computeIfAbsent(hora, h -> new ArrayList<>());
                if (agenda.libre(hora)) libres.add(entrada.getKey().getId());
            }
        }
        return porHora.entrySet().stream()
                .map(e -> new Slot(e.getKey(), !e.getValue().isEmpty(), e.getValue()))
                .toList();
    }

    /**
     * Horarios, turnos y bloqueos de los candidatos en todo el rango: tres consultas en total,
     * no tres por peluquero y por día. Después cada día se arma en memoria.
     */
    private Agendas agendas(List<Barbero> candidatos, LocalDate desde, LocalDate hasta, int duracion) {
        List<Long> ids = candidatos.stream().map(Barbero::getId).toList();
        if (ids.isEmpty()) return new Agendas(List.of(), duracion, Map.of(), Map.of(), Map.of(), calendario.ahora());
        Map<Long, Map<Integer, HorarioAtencion>> horarioPorDia = horarios.findByBarberoIdIn(ids).stream()
                .filter(HorarioAtencion::isActivo)
                .collect(Collectors.groupingBy(h -> h.getBarbero().getId(),
                        Collectors.toMap(HorarioAtencion::getDiaSemana, h -> h)));
        Map<Long, Map<LocalDate, List<Turno>>> turnosPorDia = turnos.delRango(ids, desde, hasta, EstadoTurno.CANCELADO).stream()
                .collect(Collectors.groupingBy(t -> t.getBarbero().getId(), Collectors.groupingBy(Turno::getFecha)));
        Map<Long, Map<LocalDate, List<Bloqueo>>> bloqueosPorDia = bloqueos.findByBarberoIdInAndFechaBetween(ids, desde, hasta).stream()
                .collect(Collectors.groupingBy(b -> b.getBarbero().getId(), Collectors.groupingBy(Bloqueo::getFecha)));
        return new Agendas(candidatos, duracion, horarioPorDia, turnosPorDia, bloqueosPorDia, calendario.ahora());
    }

    /**
     * La agenda de un peluquero en un día, leída de la base en ese momento. La usa la reserva
     * después de bloquear al peluquero: tiene que ver el turno que otro cliente acaba de guardar.
     */
    private Optional<Agenda> agendaDelDia(Barbero barbero, LocalDate fecha, int duracion) {
        return horarios.findByBarberoIdAndDiaSemana(barbero.getId(), Calendario.diaSemana(fecha))
                .filter(HorarioAtencion::isActivo)
                .map(h -> new Agenda(h, duracion, fecha,
                        turnos.delDia(barbero.getId(), fecha, EstadoTurno.CANCELADO),
                        bloqueos.findByBarberoIdAndFecha(barbero.getId(), fecha),
                        calendario.ahora()));
    }

    /** Lo cargado para un rango de días: alcanza para armar la agenda de cualquiera de esos días. */
    private record Agendas(List<Barbero> candidatos, int duracion,
                           Map<Long, Map<Integer, HorarioAtencion>> horarioPorDia,
                           Map<Long, Map<LocalDate, List<Turno>>> turnosPorDia,
                           Map<Long, Map<LocalDate, List<Bloqueo>>> bloqueosPorDia,
                           LocalDateTime ahora) {

        /** La agenda de cada candidato que atiende ese día. */
        Map<Barbero, Agenda> delDia(LocalDate fecha) {
            Map<Barbero, Agenda> agendas = new LinkedHashMap<>();
            for (Barbero b : candidatos) {
                HorarioAtencion horario = horarioPorDia.getOrDefault(b.getId(), Map.of()).get(Calendario.diaSemana(fecha));
                if (horario == null) continue;
                agendas.put(b, new Agenda(horario, duracion, fecha,
                        turnosPorDia.getOrDefault(b.getId(), Map.of()).getOrDefault(fecha, List.of()),
                        bloqueosPorDia.getOrDefault(b.getId(), Map.of()).getOrDefault(fecha, List.of()),
                        ahora));
            }
            return agendas;
        }
    }

    /** La agenda de un peluquero en un día, lista para preguntarle si una hora está libre. */
    private record Agenda(HorarioAtencion horario, int duracion, LocalDate fecha,
                          List<Turno> ocupados, List<Bloqueo> bloqueados, LocalDateTime ahora) {

        /** Todas las horas de inicio posibles para un servicio de esta duración. */
        List<LocalTime> inicios() {
            List<LocalTime> lista = new ArrayList<>();
            LocalTime cursor = horario.getHoraInicio();
            while (true) {
                LocalTime fin = cursor.plusMinutes(duracion);
                // LocalTime da la vuelta a las 00:00: si fin < cursor, se pasó de medianoche.
                if (fin.isBefore(cursor) || fin.isAfter(horario.getHoraFin())) break;
                lista.add(cursor);
                LocalTime siguiente = cursor.plusMinutes(horario.getDuracionSlotMin());
                if (!siguiente.isAfter(cursor)) break;
                cursor = siguiente;
            }
            return lista;
        }

        boolean libre(LocalTime inicio) {
            LocalTime fin = inicio.plusMinutes(duracion);
            boolean enGrilla = !inicio.isBefore(horario.getHoraInicio())
                    && fin.isAfter(inicio)
                    && !fin.isAfter(horario.getHoraFin())
                    && ChronoUnit.MINUTES.between(horario.getHoraInicio(), inicio) % horario.getDuracionSlotMin() == 0;
            if (!enGrilla) return false;
            if (!fecha.atTime(inicio).isAfter(ahora)) return false;
            return ocupados.stream().noneMatch(t -> t.ocupa(inicio, fin))
                    && bloqueados.stream().noneMatch(b -> b.ocupa(inicio, fin));
        }
    }
}
