package com.iaml.puntoventaapi.usuarios.infrastructure.security;

import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import com.iaml.puntoventaapi.usuarios.domain.port.UsuarioAutenticadoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("empleadosUsuarioAutenticadoAdapter")
@RequiredArgsConstructor
public class UsuarioAutenticadoAdapter implements UsuarioAutenticadoPort {

    private final UsuarioActual usuarioActual;

    @Override
    public Long obtenerId() {
        return usuarioActual.id();
    }
}

