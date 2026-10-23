package com.iaml.puntoventaapi.autenticacion.infrastructure.security;

import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import com.iaml.puntoventaapi.autenticacion.domain.Permiso;
import com.iaml.puntoventaapi.autenticacion.domain.Sesion;
import com.iaml.puntoventaapi.autenticacion.domain.port.TokenPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtTokenAdapter implements TokenPort {

    static final String CLAIM_AUTORIDADES = "autoridades";

    private final JwtEncoder encoder;
    private final Duration duracion;

    public JwtTokenAdapter(JwtEncoder encoder, @Value("${jwt.expiracion-minutos:480}") long minutos) {
        this.encoder = encoder;
        this.duracion = Duration.ofMinutes(minutos);
    }

    @Override
    public Sesion generar(Credencial credencial) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(duracion);
        String rol = credencial.getRol().getNombre();

        List<String> autoridades = new ArrayList<>();
        autoridades.add("ROLE_" + rol);
        credencial.getRol().getPermisos().stream().map(Permiso::nombre).forEach(autoridades::add);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(credencial.getUsuarioId().toString())
            .issuedAt(ahora)
            .expiresAt(expira)
            .claim("correo", credencial.getCorreo())
            .claim("rol", rol)
            .claim(CLAIM_AUTORIDADES, autoridades)
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Sesion(token, expira, credencial.getUsuarioId(), rol);
    }
}
