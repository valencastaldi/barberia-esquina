package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.modelo.Barbero;
import com.barberiaesquina.turnos.modelo.HorarioAtencion;
import com.barberiaesquina.turnos.modelo.Rol;
import com.barberiaesquina.turnos.modelo.Servicio;
import com.barberiaesquina.turnos.repositorio.BarberoRepositorio;
import com.barberiaesquina.turnos.repositorio.HorarioRepositorio;
import com.barberiaesquina.turnos.repositorio.ServicioRepositorio;
import com.barberiaesquina.turnos.servicio.excepcion.ConflictoException;
import com.barberiaesquina.turnos.servicio.excepcion.NoEncontradoException;
import com.barberiaesquina.turnos.servicio.excepcion.ReglaNegocioException;
import com.barberiaesquina.turnos.web.dto.BarberoDtos.Detalle;
import com.barberiaesquina.turnos.web.dto.BarberoDtos.Pedido;
import com.barberiaesquina.turnos.web.dto.BarberoDtos.Publico;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/** [Extensión] Equipo de peluqueros. */
@Service
@Transactional
public class BarberoServicio {

    private final BarberoRepositorio barberos;
    private final ServicioRepositorio servicios;
    private final HorarioRepositorio horarios;
    private final PasswordEncoder passwordEncoder;
    private final Calendario calendario;

    public BarberoServicio(BarberoRepositorio barberos, ServicioRepositorio servicios, HorarioRepositorio horarios,
                           PasswordEncoder passwordEncoder, Calendario calendario) {
        this.barberos = barberos;
        this.servicios = servicios;
        this.horarios = horarios;
        this.passwordEncoder = passwordEncoder;
        this.calendario = calendario;
    }

    @Transactional(readOnly = true)
    public List<Publico> publicos() {
        return barberos.findByActivoTrueOrderByIdAsc().stream().map(Publico::de).toList();
    }

    @Transactional(readOnly = true)
    public List<Detalle> equipo() {
        return barberos.findAllByOrderByIdAsc().stream().map(this::detalle).toList();
    }

    public Detalle crear(Pedido pedido) {
        if (pedido.password() == null) throw new ReglaNegocioException("La contraseña es obligatoria al dar de alta");
        if (barberos.existsByEmailIgnoreCase(pedido.email())) {
            throw new ConflictoException("Ya hay un peluquero con ese email");
        }
        Barbero b = new Barbero();
        b.setFechaAlta(calendario.ahora());
        aplicar(b, pedido);
        return detalle(barberos.save(b));
    }

    public Detalle actualizar(Long id, Pedido pedido) {
        Barbero b = buscar(id);
        if (barberos.existsByEmailIgnoreCaseAndIdNot(pedido.email(), id)) {
            throw new ConflictoException("Ya hay un peluquero con ese email");
        }
        if (b.getRol() == Rol.DUENO && pedido.rol() != Rol.DUENO) exigirOtroDuenoActivo(b);
        aplicar(b, pedido);
        return detalle(b);
    }

    /** Desactivar no borra: deja de tomar turnos pero conserva su historial. */
    public Detalle cambiarEstado(Long id, boolean activo) {
        Barbero b = buscar(id);
        if (!activo && b.getRol() == Rol.DUENO) exigirOtroDuenoActivo(b);
        b.setActivo(activo);
        return detalle(b);
    }

    /** Siempre tiene que quedar alguien que pueda entrar al panel como dueño. */
    private void exigirOtroDuenoActivo(Barbero b) {
        boolean hayOtro = barberos.findByActivoTrueOrderByIdAsc().stream()
                .anyMatch(o -> o.getRol() == Rol.DUENO && !o.getId().equals(b.getId()));
        if (!hayOtro) throw new ReglaNegocioException("Tiene que quedar al menos un dueño activo");
    }

    private void aplicar(Barbero b, Pedido p) {
        b.setNombre(p.nombre().trim());
        b.setApellido(p.apellido().trim());
        b.setDni(vacioANull(p.dni()));
        b.setFechaNacimiento(p.fechaNacimiento());
        b.setEmail(p.email().trim().toLowerCase());
        b.setTelefono(vacioANull(p.telefono()));
        b.setRol(p.rol());
        b.setComisionPct(p.rol() == Rol.DUENO ? 0 : p.comisionPct());
        List<Servicio> elegidos = servicios.findAllById(p.servicios());
        if (elegidos.size() != new HashSet<>(p.servicios()).size()) {
            throw new ReglaNegocioException("Algún servicio elegido no existe");
        }
        b.setServicios(new HashSet<>(elegidos));
        if (p.password() != null) b.setPasswordHash(passwordEncoder.encode(p.password()));
    }

    private Detalle detalle(Barbero b) {
        List<Integer> dias = horarios.findByBarberoIdOrderByDiaSemanaAsc(b.getId()).stream()
                .filter(HorarioAtencion::isActivo)
                .map(HorarioAtencion::getDiaSemana)
                .toList();
        return Detalle.de(b, dias);
    }

    private Barbero buscar(Long id) {
        return barberos.findById(id).orElseThrow(() -> NoEncontradoException.de("Peluquero", id));
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
