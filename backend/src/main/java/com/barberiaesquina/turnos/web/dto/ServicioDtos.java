package com.barberiaesquina.turnos.web.dto;

import com.barberiaesquina.turnos.modelo.Servicio;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class ServicioDtos {

    private ServicioDtos() {}

    public record Pedido(
            @NotBlank @Size(max = 60) String nombre,
            @Size(max = 160) String descripcion,
            @NotNull @Min(5) @Max(480) Integer duracionMinutos,
            @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal precio
    ) {}

    public record Respuesta(Long id, String nombre, String descripcion, Integer duracionMinutos,
                            BigDecimal precio, boolean activo) {
        public static Respuesta de(Servicio s) {
            return new Respuesta(s.getId(), s.getNombre(), s.getDescripcion(), s.getDuracionMinutos(),
                    s.getPrecio(), s.isActivo());
        }
    }
}
