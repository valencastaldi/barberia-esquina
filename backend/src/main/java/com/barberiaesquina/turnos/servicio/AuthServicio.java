package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.modelo.Barbero;
import com.barberiaesquina.turnos.repositorio.BarberoRepositorio;
import com.barberiaesquina.turnos.seguridad.EmisorDeTokens;
import com.barberiaesquina.turnos.seguridad.SesionActual;
import com.barberiaesquina.turnos.servicio.excepcion.CredencialesInvalidasException;
import com.barberiaesquina.turnos.servicio.excepcion.NoEncontradoException;
import com.barberiaesquina.turnos.web.dto.AuthDtos.LoginRespuesta;
import com.barberiaesquina.turnos.web.dto.AuthDtos.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServicio {

    private final BarberoRepositorio barberos;
    private final PasswordEncoder passwordEncoder;
    private final EmisorDeTokens emisor;
    private final SesionActual sesion;

    /** Hash de una contraseña cualquiera, para que un email inexistente tarde lo mismo que uno válido. */
    private final String hashDeRelleno;

    public AuthServicio(BarberoRepositorio barberos, PasswordEncoder passwordEncoder, EmisorDeTokens emisor,
                        SesionActual sesion) {
        this.barberos = barberos;
        this.passwordEncoder = passwordEncoder;
        this.emisor = emisor;
        this.sesion = sesion;
        this.hashDeRelleno = passwordEncoder.encode("relleno-para-igualar-tiempos");
    }

    /** RF-08. Mismo mensaje si el email no existe o la contraseña está mal. */
    public LoginRespuesta login(String email, String password) {
        Barbero barbero = barberos.findByEmailIgnoreCase(email.trim()).orElse(null);
        String hash = barbero == null ? hashDeRelleno : barbero.getPasswordHash();
        boolean coincide = passwordEncoder.matches(password, hash);
        if (barbero == null || !coincide || !barbero.isActivo()) {
            throw new CredencialesInvalidasException();
        }
        var token = emisor.emitir(barbero);
        return new LoginRespuesta(token.token(), token.vence(), Usuario.de(barbero));
    }

    public Usuario yo() {
        return barberos.findById(sesion.idBarbero())
                .map(Usuario::de)
                .orElseThrow(() -> new NoEncontradoException("La sesión no corresponde a un usuario"));
    }
}
