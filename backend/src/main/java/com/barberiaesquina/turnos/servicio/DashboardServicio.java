package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.modelo.Barbero;
import com.barberiaesquina.turnos.modelo.Encuesta;
import com.barberiaesquina.turnos.modelo.EstadoTurno;
import com.barberiaesquina.turnos.modelo.Turno;
import com.barberiaesquina.turnos.repositorio.BarberoRepositorio;
import com.barberiaesquina.turnos.repositorio.EncuestaRepositorio;
import com.barberiaesquina.turnos.repositorio.TurnoRepositorio;
import com.barberiaesquina.turnos.web.dto.DashboardDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Métricas del panel (RF-18, RF-19, RF-20, RF-21). */
@Service
@Transactional(readOnly = true)
public class DashboardServicio {

    private final TurnoRepositorio turnos;
    private final EncuestaRepositorio encuestas;
    private final BarberoRepositorio barberos;

    public DashboardServicio(TurnoRepositorio turnos, EncuestaRepositorio encuestas, BarberoRepositorio barberos) {
        this.turnos = turnos;
        this.encuestas = encuestas;
        this.barberos = barberos;
    }

    public Resumen resumen(LocalDate desde, LocalDate hasta, Long idBarbero) {
        List<Turno> lista = delPeriodo(desde, hasta, idBarbero);
        Map<EstadoTurno, Long> porEstado = lista.stream()
                .collect(Collectors.groupingBy(Turno::getEstado, () -> new EnumMap<>(EstadoTurno.class), Collectors.counting()));
        long completados = porEstado.getOrDefault(EstadoTurno.COMPLETADO, 0L);
        long ausentes = porEstado.getOrDefault(EstadoTurno.AUSENTE, 0L);
        double ausentismo = completados + ausentes == 0 ? 0 : (double) ausentes / (completados + ausentes);

        List<Encuesta> respuestas = encuestasDe(lista);
        Map<Integer, Long> distribucion = new LinkedHashMap<>();
        for (int estrellas = 5; estrellas >= 1; estrellas--) distribucion.put(estrellas, 0L);
        respuestas.forEach(e -> distribucion.merge(e.getCalificacion(), 1L, Long::sum));

        List<Comentario> comentarios = respuestas.stream()
                .filter(e -> e.getComentario() != null)
                .sorted(Comparator.comparing(Encuesta::getFechaRespuesta).reversed())
                .limit(5)
                .map(e -> new Comentario(nombreCorto(e.getTurno()), e.getCalificacion(), e.getComentario(),
                        e.getFechaRespuesta()))
                .toList();

        return new Resumen(lista.size(), porEstado.getOrDefault(EstadoTurno.PENDIENTE, 0L), completados, ausentes,
                porEstado.getOrDefault(EstadoTurno.CANCELADO, 0L), ausentismo, facturacion(lista),
                promedio(respuestas), respuestas.size(), distribucion, comentarios);
    }

    /** Turnos por día, con los días sin turnos en cero para que el gráfico no tenga huecos. */
    public List<PuntoEvolucion> evolucion(LocalDate desde, LocalDate hasta, Long idBarbero) {
        Map<LocalDate, List<Turno>> porDia = delPeriodo(desde, hasta, idBarbero).stream()
                .filter(t -> t.getEstado() != EstadoTurno.CANCELADO)
                .collect(Collectors.groupingBy(Turno::getFecha));
        List<PuntoEvolucion> puntos = new ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            List<Turno> delDia = porDia.getOrDefault(d, List.of());
            long completados = delDia.stream().filter(t -> t.getEstado() == EstadoTurno.COMPLETADO).count();
            puntos.add(new PuntoEvolucion(d, delDia.size(), completados));
        }
        return puntos;
    }

    /** [Extensión] Cómo le fue a cada peluquero en el período. */
    public List<RendimientoBarbero> porBarbero(LocalDate desde, LocalDate hasta) {
        List<Turno> lista = delPeriodo(desde, hasta, null);
        Map<Long, List<Encuesta>> encuestasPorBarbero = encuestasDe(lista).stream()
                .collect(Collectors.groupingBy(e -> e.getTurno().getBarbero().getId()));
        List<RendimientoBarbero> filas = new ArrayList<>();
        for (Barbero b : barberos.findAllByOrderByIdAsc()) {
            List<Turno> suyos = lista.stream().filter(t -> t.getBarbero().getId().equals(b.getId())).toList();
            if (suyos.isEmpty() && !b.isActivo()) continue;
            filas.add(new RendimientoBarbero(b.getId(), b.nombreCompleto(),
                    suyos.stream().filter(t -> t.getEstado() == EstadoTurno.COMPLETADO).count(),
                    suyos.stream().filter(t -> t.getEstado() == EstadoTurno.AUSENTE).count(),
                    facturacion(suyos),
                    promedio(encuestasPorBarbero.getOrDefault(b.getId(), List.of()))));
        }
        return filas;
    }

    /** Reseñas públicas para la home: encuestas de los últimos 90 días y atendidos en el mes en curso. */
    public Resenas resenas(LocalDate hoy) {
        List<Encuesta> respuestas = encuestasDe(delPeriodo(hoy.minusDays(89), hoy, null));
        long atendidosMes = delPeriodo(hoy.withDayOfMonth(1), hoy, null).stream()
                .filter(t -> t.getEstado() == EstadoTurno.COMPLETADO).count();
        List<Comentario> destacados = respuestas.stream()
                .filter(e -> e.getComentario() != null && e.getCalificacion() >= 4)
                .sorted(Comparator.comparing(Encuesta::getFechaRespuesta).reversed())
                .limit(3)
                .map(e -> new Comentario(nombreCorto(e.getTurno()), e.getCalificacion(), e.getComentario(),
                        e.getFechaRespuesta()))
                .toList();
        return new Resenas(promedio(respuestas), respuestas.size(), atendidosMes, destacados);
    }

    // ------------------------------------------------------------------

    private List<Turno> delPeriodo(LocalDate desde, LocalDate hasta, Long idBarbero) {
        Calendario.validarRango(desde, hasta);
        return turnos.entre(desde, hasta, idBarbero);
    }

    private List<Encuesta> encuestasDe(List<Turno> lista) {
        List<Long> ids = lista.stream().filter(t -> t.getEstado() == EstadoTurno.COMPLETADO).map(Turno::getId).toList();
        return ids.isEmpty() ? List.of() : encuestas.findByTurnoIdIn(ids);
    }

    private static BigDecimal facturacion(List<Turno> lista) {
        return lista.stream().filter(t -> t.getEstado() == EstadoTurno.COMPLETADO)
                .map(Turno::getPrecio).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Double promedio(List<Encuesta> respuestas) {
        return respuestas.stream().mapToInt(Encuesta::getCalificacion).average().stream().boxed().findFirst().orElse(null);
    }

    /** "Mateo G.": en el panel alcanza con el nombre y la inicial del apellido. */
    private static String nombreCorto(Turno t) {
        String apellido = t.getCliente().getApellido();
        return t.getCliente().getNombre() + (apellido == null || apellido.isBlank() ? "" : " " + apellido.charAt(0) + ".");
    }
}
