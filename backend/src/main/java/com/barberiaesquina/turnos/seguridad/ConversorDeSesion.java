package com.barberiaesquina.turnos.seguridad;

import com.barberiaesquina.turnos.modelo.Barbero;
import com.barberiaesquina.turnos.repositorio.BarberoRepositorio;
import com.barberiaesquina.turnos.repositorio.TokenRevocadoRepositorio;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Convierte el JWT en la sesión del pedido mirando la base, no solo el token:
 * el rol sale de la tabla barbero y el token deja de servir si el peluquero
 * fue dado de baja, cambió su contraseña o cerró esa sesión. Sin esto, un dueño
 * pasado a barbero (o un peluquero desactivado) seguía con sus permisos hasta
 * que vencía el token.
 */
@Component
public class ConversorDeSesion implements Converter<Jwt, AbstractAuthenticationToken> {

    /** Claim con una huella de la contraseña: cambiarla invalida los tokens anteriores. */
    public static final String CLAIM_CREDENCIAL = "cred";

    private final BarberoRepositorio barberos;
    private final TokenRevocadoRepositorio revocados;

    public ConversorDeSesion(BarberoRepositorio barberos, TokenRevocadoRepositorio revocados) {
        this.barberos = barberos;
        this.revocados = revocados;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Barbero barbero = idDe(jwt) == null ? null : barberos.findById(idDe(jwt)).orElse(null);
        if (barbero == null || !barbero.isActivo()
                || !huella(barbero).equals(jwt.getClaimAsString(CLAIM_CREDENCIAL))
                || jwt.getId() == null || revocados.existsById(jwt.getId())) {
            throw new InvalidBearerTokenException("La sesión ya no es válida");
        }
        var rol = new SimpleGrantedAuthority("ROLE_" + barbero.getRol().name());
        return new JwtAuthenticationToken(jwt, List.of(rol), jwt.getSubject());
    }

    /** Primeros 16 bytes del SHA-256 del hash bcrypt: identifica la contraseña sin revelarla. */
    static String huella(Barbero barbero) {
        try {
            byte[] sha = MessageDigest.getInstance("SHA-256")
                    .digest(barbero.getPasswordHash().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(sha, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Long idDe(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
