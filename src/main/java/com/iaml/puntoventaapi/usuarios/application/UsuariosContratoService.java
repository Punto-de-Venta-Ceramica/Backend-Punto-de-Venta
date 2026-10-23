package com.iaml.puntoventaapi.usuarios.application;

import com.iaml.puntoventaapi.usuarios.CredencialesUsuario;
import com.iaml.puntoventaapi.usuarios.UsuariosContrato;
import com.iaml.puntoventaapi.usuarios.domain.port.UsuarioRepositorioPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuariosContratoService implements UsuariosContrato {

    private final UsuarioRepositorioPort usuarios;

    @Override
    public boolean existePorId(Long usuarioId) {
        return usuarios.existePorId(usuarioId);
    }

    @Override
    public Optional<CredencialesUsuario> buscarCredencialesPorCorreo(String correo) {
        return usuarios.buscarPorCorreo(correo)
            .map(u -> new CredencialesUsuario(u.getId(), u.getCorreo(), u.getPasswordHash(), u.getRolId()));
    }
}
