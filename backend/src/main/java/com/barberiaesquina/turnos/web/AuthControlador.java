package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.servicio.AuthServicio;
import com.barberiaesquina.turnos.web.dto.AuthDtos.LoginPedido;
import com.barberiaesquina.turnos.web.dto.AuthDtos.LoginRespuesta;
import com.barberiaesquina.turnos.web.dto.AuthDtos.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** M1 — Autenticación. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthControlador {

    private final AuthServicio auth;

    public AuthControlador(AuthServicio auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginRespuesta login(@Valid @RequestBody LoginPedido pedido) {
        return auth.login(pedido.email(), pedido.password());
    }

    /**
     * Revoca el token con el que se hizo el pedido: aunque alguien se hubiera
     * guardado una copia, deja de servir. Las sesiones en otros dispositivos siguen.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        auth.cerrarSesion();
    }

    @GetMapping("/me")
    public Usuario yo() {
        return auth.yo();
    }
}
