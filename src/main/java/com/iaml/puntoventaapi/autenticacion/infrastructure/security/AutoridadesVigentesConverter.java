package com.iaml.puntoventaapi.autenticacion.infrastructure.security;

import com.iaml.puntoventaapi.autenticacion.domain.port.AutoridadesVigentesPort;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AutoridadesVigentesConverter implements Converter<Jwt, JwtAuthenticationToken> {
    private final AutoridadesVigentesPort autoridades;
    public AutoridadesVigentesConverter(AutoridadesVigentesPort autoridades) { this.autoridades = autoridades; }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        final long usuarioId;
        try { usuarioId = Long.parseLong(jwt.getSubject()); }
        catch (NumberFormatException | NullPointerException ex) { throw sesionInvalida(); }
        if (usuarioId <= 0) throw sesionInvalida();
        var actual = autoridades.buscar(usuarioId).orElseThrow(AutoridadesVigentesConverter::sesionInvalida);
        if (!actual.rol().equals(jwt.getClaimAsString("rol"))) throw sesionInvalida();
        List<GrantedAuthority> roles = new ArrayList<>();
        roles.add(new SimpleGrantedAuthority("ROLE_" + actual.rol()));
        actual.permisos().stream().map(SimpleGrantedAuthority::new).forEach(roles::add);
        return new JwtAuthenticationToken(jwt, roles);
    }

    private static InvalidBearerTokenException sesionInvalida() {
        return new InvalidBearerTokenException("La sesión ya no es válida. Inicia sesión de nuevo.");
    }
}
