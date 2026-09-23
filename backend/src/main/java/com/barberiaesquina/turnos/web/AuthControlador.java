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
     * El JWT no se guarda en el servidor: cerrar sesión es borrarlo en el front.
     * El endpoint existe porque figura en la Etapa 4 y deja lugar para una lista
     * de tokens revocados si hiciera falta.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
    }

    @GetMapping("/me")
    public Usuario yo() {
        return auth.yo();
    }
}
