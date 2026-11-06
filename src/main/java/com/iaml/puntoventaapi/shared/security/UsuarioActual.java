package com.iaml.puntoventaapi.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActual {

    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()) {
            throw new UsuarioNoAutenticadoException();
        }
        try {
            long usuarioId = Long.parseLong(jwt.getToken().getSubject());
            if (usuarioId > 0) {
                return usuarioId;
            }
        } catch (NumberFormatException ex) {
            throw new UsuarioNoAutenticadoException();
        }
        throw new UsuarioNoAutenticadoException();
    }
}
