package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.modelo.Barbero;
import com.barberiaesquina.turnos.modelo.Encuesta;
import com.barberiaesquina.turnos.modelo.Turno;
import com.barberiaesquina.turnos.repositorio.BarberoRepositorio;
import com.barberiaesquina.turnos.repositorio.EncuestaRepositorio;
import com.barberiaesquina.turnos.web.dto.OpinionDtos.Opinion;
import com.barberiaesquina.turnos.web.dto.OpinionDtos.PorBarbero;
import com.barberiaesquina.turnos.web.dto.OpinionDtos.Reporte;
import com.barberiaesquina.turnos.web.dto.TurnoDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Opiniones de los clientes (encuestas respondidas), visibles para todo el equipo. */
@Service
@Transactional(readOnly = true)
public class OpinionServicio {

    private final EncuestaRepositorio encuestas;
    private final BarberoRepositorio barberos;

    public OpinionServicio(EncuestaRepositorio encuestas, BarberoRepositorio barberos) {
        this.encuestas = encuestas;
        this.barberos = barberos;
    }

    public Reporte reporte(LocalDate desde, LocalDate hasta, Long idBarbero) {
        Calendario.validarRango(desde, hasta);
        List<Encuesta> todas = encuestas.delPeriodo(desde, hasta);

        // Resumen de cada peluquero, siempre con todo el equipo (para compararse).
        List<PorBarbero> porBarbero = new ArrayList<>();
        for (Barbero b : barberos.findAllByOrderByIdAsc()) {
            List<Encuesta> suyas = todas.stream().filter(e -> e.getTurno().getBarbero().getId().equals(b.getId())).toList();
            if (suyas.isEmpty() && !b.isActivo()) continue;
            porBarbero.add(new PorBarbero(b.getId(), b.getNombre(), promedio(suyas), suyas.size(), distribucion(suyas)));
        }

        List<Encuesta> filtradas = idBarbero == null ? todas
                : todas.stream().filter(e -> e.getTurno().getBarbero().getId().equals(idBarbero)).toList();
        List<Opinion> opiniones = filtradas.stream().map(OpinionServicio::opinion).toList();
        return new Reporte(promedio(filtradas), filtradas.size(), porBarbero, opiniones);
    }

    private static Opinion opinion(Encuesta e) {
        Turno t = e.getTurno();
        String apellido = t.getCliente().getApellido();
        String cliente = t.getCliente().getNombre() + (apellido == null || apellido.isBlank() ? "" : " " + apellido.charAt(0) + ".");
        return new Opinion(t.getId(), t.getFecha(), t.getServicio().getNombre(),
                new TurnoDtos.Ref(t.getBarbero().getId(), t.getBarbero().getNombre()), cliente,
                e.getCalificacion(), e.getComentario(), e.getFechaRespuesta());
    }

    private static Double promedio(List<Encuesta> lista) {
        return lista.stream().mapToInt(Encuesta::getCalificacion).average().stream().boxed().findFirst().orElse(null);
    }

    private static Map<Integer, Long> distribucion(List<Encuesta> lista) {
        Map<Integer, Long> d = new LinkedHashMap<>();
        for (int estrellas = 5; estrellas >= 1; estrellas--) d.put(estrellas, 0L);
        lista.forEach(e -> d.merge(e.getCalificacion(), 1L, Long::sum));
        return d;
    }
}
