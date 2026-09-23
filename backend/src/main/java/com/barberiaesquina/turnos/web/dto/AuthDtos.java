package com.barberiaesquina.turnos.web.dto;

import com.barberiaesquina.turnos.modelo.Barbero;
import com.barberiaesquina.turnos.modelo.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginPedido(@NotBlank @Email String email, @NotBlank String password) {}

    public record LoginRespuesta(String token, Instant vence, Usuario usuario) {}

    public record Usuario(Long id, String nombre, String apellido, String email, Rol rol) {
        public static Usuario de(Barbero b) {
            return new Usuario(b.getId(), b.getNombre(), b.getApellido(), b.getEmail(), b.getRol());
        }
    }
}
