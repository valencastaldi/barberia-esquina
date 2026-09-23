package com.barberiaesquina.turnos.web.dto;

import com.barberiaesquina.turnos.modelo.Barbero;
import com.barberiaesquina.turnos.modelo.Rol;
import com.barberiaesquina.turnos.modelo.Servicio;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class BarberoDtos {

    private BarberoDtos() {}

    /** Lo que ve el cliente al elegir con quién atenderse. */
    public record Publico(Long id, String nombre, String apellido, List<Long> servicios) {
        public static Publico de(Barbero b) {
            return new Publico(b.getId(), b.getNombre(), b.getApellido(), idsServicios(b));
        }
    }

    /** Ficha completa para el panel. */
    public record Detalle(Long id, String nombre, String apellido, String dni, LocalDate fechaNacimiento,
                          String email, String telefono, Rol rol, Integer comisionPct, boolean activo,
                          LocalDateTime fechaAlta, List<Long> servicios, List<Integer> diasAtencion) {
        public static Detalle de(Barbero b, List<Integer> diasAtencion) {
            return new Detalle(b.getId(), b.getNombre(), b.getApellido(), b.getDni(), b.getFechaNacimiento(),
                    b.getEmail(), b.getTelefono(), b.getRol(), b.getComisionPct(), b.isActivo(),
                    b.getFechaAlta(), idsServicios(b), diasAtencion);
        }
    }

    /** Alta y edición. La contraseña es obligatoria solo al dar de alta. */
    public record Pedido(
            @NotBlank @Size(max = 60) String nombre,
            @NotBlank @Size(max = 60) String apellido,
            @Size(max = 15) String dni,
            @Past LocalDate fechaNacimiento,
            @NotBlank @Email @Size(max = 120) String email,
            @Size(max = 30) String telefono,
            @NotNull Rol rol,
            @NotNull @Min(0) @Max(100) Integer comisionPct,
            @NotNull List<Long> servicios,
            @Size(min = 8, max = 72) String password
    ) {}

    private static List<Long> idsServicios(Barbero b) {
        return b.getServicios().stream().map(Servicio::getId).sorted().toList();
    }
}
