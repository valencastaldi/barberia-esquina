package com.barberiaesquina.turnos.seguridad;

import com.barberiaesquina.turnos.config.AppProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * API sin sesión: el panel manda "Authorization: Bearer <jwt>" en cada pedido.
 * Lo que usa el cliente (reservar, cancelar, encuesta) es público.
 */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    public static final String CLAIM_ROLES = "roles";

    /** El valor por defecto de application.yml. */
    private static final String SECRETO_DE_DESARROLLO = "solo-para-desarrollo-cambiar-en-produccion-1290";

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ConversorDeSesion conversor) throws Exception {
        http
            .csrf(c -> c.disable())
            .cors(withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/servicios", "/api/v1/barberos", "/api/v1/horarios", "/api/v1/resenas",
                        "/api/v1/disponibilidad/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/turnos").permitAll()
                .requestMatchers("/api/v1/turnos/cancelar/**", "/api/v1/encuestas/**").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversor)));
        return http.build();
    }

    /** RNF: contraseñas con bcrypt, costo mayor o igual a 10. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    SecretKey claveJwt(AppProperties props) {
        byte[] bytes = props.jwt().secreto().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secreto tiene que tener al menos 32 caracteres");
        }
        // El secreto de desarrollo está publicado en el repo: con él cualquiera firma un token de dueño.
        // Si la app corre sin datos de demo (o sea, con datos reales), exige uno propio.
        if (!props.datosDemo() && SECRETO_DE_DESARROLLO.equals(props.jwt().secreto())) {
            throw new IllegalStateException("Con datos reales hay que definir JWT_SECRET (el de desarrollo es público)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey claveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    /** Valida firma, emisor y vencimiento usando el mismo reloj que el resto de la app. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey claveJwt, Clock reloj) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
        var vencimiento = new JwtTimestampValidator();
        vencimiento.setClock(reloj);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                vencimiento, new JwtIssuerValidator(EmisorDeTokens.EMISOR)));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(props.origenesPermitidos());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setMaxAge(3600L);
        var fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }
}
