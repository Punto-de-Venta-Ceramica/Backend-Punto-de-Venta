package com.iaml.puntoventaapi.autenticacion.infrastructure.security;

import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import com.iaml.puntoventaapi.autenticacion.domain.Permiso;
import com.iaml.puntoventaapi.autenticacion.domain.Sesion;
import com.iaml.puntoventaapi.autenticacion.domain.exception.CredencialesInvalidasException;
import com.iaml.puntoventaapi.autenticacion.domain.port.RefreshTokenPort;
import com.iaml.puntoventaapi.autenticacion.domain.port.TokenPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Emite el par de tokens: acceso (corto, autoriza la API) y renovación (largo, solo sirve en
 * {@code /api/auth/refresh}).
 */
@Component
public class JwtTokenAdapter implements TokenPort, RefreshTokenPort {

    static final String CLAIM_AUTORIDADES = "autoridades";
    static final String CLAIM_TIPO = "tipo";
    static final String TIPO_ACCESO = "acceso";
    static final String TIPO_RENOVACION = "renovacion";
    static final String CLAIM_HUELLA = "huella";

    private final JwtEncoder encoder;
    private final JwtDecoder decoderRenovacion;
    private final Duration duracionAcceso;
    private final Duration duracionRenovacion;

    public JwtTokenAdapter(JwtEncoder encoder,
                           @Value("${jwt.secreto}") String secreto,
                           @Value("${jwt.expiracion-minutos:15}") long minutosAcceso,
                           @Value("${jwt.refresh-expiracion-dias:20}") long diasRenovacion) {
        this.encoder = encoder;
        // Decodificador propio: el del filtro de seguridad rechaza los tokens de renovación.
        this.decoderRenovacion = NimbusJwtDecoder
            .withSecretKey(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
        this.duracionAcceso = Duration.ofMinutes(minutosAcceso);
        this.duracionRenovacion = Duration.ofDays(diasRenovacion);
    }

    @Override
    public Sesion generar(Credencial credencial) {
        Instant ahora = Instant.now();
        Instant expiraRenovacion = ahora.plus(duracionRenovacion);
        String renovacion = codificar(JwtClaimsSet.builder()
            .subject(credencial.getUsuarioId().toString())
            .issuedAt(ahora)
            .expiresAt(expiraRenovacion)
            .claim(CLAIM_TIPO, TIPO_RENOVACION)
            .claim(CLAIM_HUELLA, credencial.huellaPassword())
            .build());
        return renovarAcceso(credencial, renovacion, expiraRenovacion);
    }

    @Override
    public Sesion renovarAcceso(Credencial credencial, String renovacion, Instant expiraRenovacion) {
        Instant ahora = Instant.now();
        Instant expiraAcceso = ahora.plus(duracionAcceso);
        String rol = credencial.getRol().getNombre();

        List<String> autoridades = new ArrayList<>();
        autoridades.add("ROLE_" + rol);
        credencial.getRol().getPermisos().stream().map(Permiso::nombre).forEach(autoridades::add);

        String acceso = codificar(JwtClaimsSet.builder()
            .subject(credencial.getUsuarioId().toString())
            .issuedAt(ahora)
            .expiresAt(expiraAcceso)
            .claim(CLAIM_TIPO, TIPO_ACCESO)
            .claim("correo", credencial.getCorreo())
            .claim("rol", rol)
            .claim(CLAIM_AUTORIDADES, autoridades)
            .build());
        return new Sesion(acceso, expiraAcceso, renovacion, expiraRenovacion, credencial.getUsuarioId(), rol);
    }

    @Override
    public RefreshValido validar(String refreshToken) {
        try {
            Jwt jwt = decoderRenovacion.decode(refreshToken);
            if (!TIPO_RENOVACION.equals(jwt.getClaimAsString(CLAIM_TIPO))) {
                throw new CredencialesInvalidasException();
            }
            return new RefreshValido(Long.valueOf(jwt.getSubject()), jwt.getExpiresAt(),
                jwt.getClaimAsString(CLAIM_HUELLA));
        } catch (JwtException | NumberFormatException ex) {
            throw new CredencialesInvalidasException();
        }
    }

    private String codificar(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
