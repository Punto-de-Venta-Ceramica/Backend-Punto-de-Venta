package com.iaml.puntoventaapi.sucursal.infrastructure.security;

import com.iaml.puntoventaapi.sucursal.domain.port.UsuarioAutenticadoPort;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class UsuarioAutenticadoAdapter implements UsuarioAutenticadoPort {

    @Override
    public Long obtenerId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Se requiere una sesión JWT válida.");
        }
        try {
            long id = Long.parseLong(jwt.getToken().getSubject());
            if (id <= 0) {
                throw new BadCredentialsException("La sesión no identifica a un usuario válido.");
            }
            return id;
        } catch (NumberFormatException ex) {
            throw new BadCredentialsException("La sesión no identifica a un usuario válido.");
        }
    }
}
