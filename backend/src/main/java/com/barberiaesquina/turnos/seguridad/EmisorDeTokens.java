package com.barberiaesquina.turnos.seguridad;

import com.barberiaesquina.turnos.config.AppProperties;
import com.barberiaesquina.turnos.modelo.Barbero;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** RNF: JWT con expiración. */
@Component
public class EmisorDeTokens {

    public static final String EMISOR = "barberia-esquina";

    private final JwtEncoder encoder;
    private final AppProperties props;
    private final Clock reloj;

    public EmisorDeTokens(JwtEncoder encoder, AppProperties props, Clock reloj) {
        this.encoder = encoder;
        this.props = props;
        this.reloj = reloj;
    }

    public TokenEmitido emitir(Barbero barbero) {
        Instant ahora = reloj.instant();
        Instant vence = ahora.plus(props.jwt().expiracionMinutos(), ChronoUnit.MINUTES);
        var claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(String.valueOf(barbero.getId()))
                .issuedAt(ahora)
                .expiresAt(vence)
                .claim("email", barbero.getEmail())
                .claim("nombre", barbero.nombreCompleto())
                .claim(SeguridadConfig.CLAIM_ROLES, List.of(barbero.getRol().name()))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, vence);
    }

    public record TokenEmitido(String token, Instant vence) {}
}
