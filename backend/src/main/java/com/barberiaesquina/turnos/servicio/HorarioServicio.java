package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.modelo.Barbero;
import com.barberiaesquina.turnos.modelo.HorarioAtencion;
import com.barberiaesquina.turnos.repositorio.BarberoRepositorio;
import com.barberiaesquina.turnos.repositorio.HorarioRepositorio;
import com.barberiaesquina.turnos.seguridad.SesionActual;
import com.barberiaesquina.turnos.servicio.excepcion.NoEncontradoException;
import com.barberiaesquina.turnos.servicio.excepcion.ReglaNegocioException;
import com.barberiaesquina.turnos.web.dto.HorarioDtos.Dia;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/** Horario de atención (RF-24, RF-25, RF-26). Con la extensión, cada peluquero tiene el suyo. */
@Service
@Transactional
public class HorarioServicio {

    private static final int SLOT_POR_DEFECTO = 30;

    private final HorarioRepositorio horarios;
    private final BarberoRepositorio barberos;
    private final SesionActual sesion;

    public HorarioServicio(HorarioRepositorio horarios, BarberoRepositorio barberos, SesionActual sesion) {
        this.horarios = horarios;
        this.barberos = barberos;
        this.sesion = sesion;
    }

    /** Los 7 días del peluquero, incluidos los que no atiende (activo = false). */
    @Transactional(readOnly = true)
    public List<Dia> deBarbero(Long idBarbero) {
        buscarBarbero(idBarbero);
        Map<Integer, HorarioAtencion> porDia = horarios.findByBarberoIdOrderByDiaSemanaAsc(idBarbero).stream()
                .collect(Collectors.toMap(HorarioAtencion::getDiaSemana, h -> h));
        List<Dia> semana = new ArrayList<>();
        for (int d = 0; d <= 6; d++) {
            HorarioAtencion h = porDia.get(d);
            semana.add(h == null
                    ? new Dia(d, null, null, SLOT_POR_DEFECTO, false)
                    : new Dia(d, h.getHoraInicio(), h.getHoraFin(), h.getDuracionSlotMin(), h.isActivo()));
        }
        return semana;
    }

    /**
     * Horario de la barbería para el sitio del cliente ("abierto ahora"): un día
     * está abierto si atiende algún peluquero, desde el que abre antes hasta el que cierra último.
     */
    @Transactional(readOnly = true)
    public List<Dia> deLaBarberia() {
        Map<Integer, List<HorarioAtencion>> porDia = horarios.activosDeBarberosActivos().stream()
                .collect(Collectors.groupingBy(HorarioAtencion::getDiaSemana));
        List<Dia> semana = new ArrayList<>();
        for (int d = 0; d <= 6; d++) {
            List<HorarioAtencion> delDia = porDia.getOrDefault(d, List.of());
            if (delDia.isEmpty()) {
                semana.add(new Dia(d, null, null, SLOT_POR_DEFECTO, false));
                continue;
            }
            LocalTime abre = delDia.stream().map(HorarioAtencion::getHoraInicio).min(Comparator.naturalOrder()).orElseThrow();
            LocalTime cierra = delDia.stream().map(HorarioAtencion::getHoraFin).max(Comparator.naturalOrder()).orElseThrow();
            int slot = delDia.stream().mapToInt(HorarioAtencion::getDuracionSlotMin).min().orElse(SLOT_POR_DEFECTO);
            semana.add(new Dia(d, abre, cierra, slot, true));
        }
        return semana;
    }

    /**
     * Reemplaza la semana del peluquero. Cambiar el horario no toca los turnos
     * ya reservados: solo afecta lo que se ofrece de ahora en más.
     */
    public List<Dia> guardar(Long idBarbero, List<Dia> dias) {
        if (!sesion.esDueno() && !idBarbero.equals(sesion.idBarbero())) {
            throw new AccessDeniedException("Solo el dueño puede cambiar el horario de otro peluquero");
        }
        Barbero barbero = buscarBarbero(idBarbero);
        Set<Integer> vistos = new HashSet<>();
        for (Dia dia : dias) {
            if (!vistos.add(dia.diaSemana())) throw new ReglaNegocioException("Día repetido: " + dia.diaSemana());
            if (dia.activo() && (dia.horaInicio() == null || dia.horaFin() == null
                    || !dia.horaInicio().isBefore(dia.horaFin()))) {
                throw new ReglaNegocioException("El día " + dia.diaSemana() + " necesita un horario de inicio anterior al de fin");
            }

            HorarioAtencion h = horarios.findByBarberoIdAndDiaSemana(idBarbero, dia.diaSemana()).orElseGet(() -> {
                HorarioAtencion nuevo = new HorarioAtencion();
                nuevo.setBarbero(barbero);
                nuevo.setDiaSemana(dia.diaSemana());
                return nuevo;
            });
            if (!dia.activo() && h.getId() == null) continue;   // día libre que nunca tuvo horario: nada que guardar
            h.setActivo(dia.activo());
            if (dia.horaInicio() != null) h.setHoraInicio(dia.horaInicio());
            if (dia.horaFin() != null) h.setHoraFin(dia.horaFin());
            h.setDuracionSlotMin(dia.duracionSlotMin());
            if (h.getHoraInicio() == null || h.getHoraFin() == null) continue;
            horarios.save(h);
        }
        return deBarbero(idBarbero);
    }

    private Barbero buscarBarbero(Long id) {
        return barberos.findById(id).orElseThrow(() -> NoEncontradoException.de("Peluquero", id));
    }
}
