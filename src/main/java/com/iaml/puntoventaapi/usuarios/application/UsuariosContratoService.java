package com.iaml.puntoventaapi.usuarios.application;

import com.iaml.puntoventaapi.usuarios.CredencialesUsuario;
import com.iaml.puntoventaapi.usuarios.UsuariosContrato;
import com.iaml.puntoventaapi.usuarios.domain.port.UsuarioRepositorioPort;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UsuariosContratoService implements UsuariosContrato {

    private final UsuarioRepositorioPort usuarios;

    public UsuariosContratoService(UsuarioRepositorioPort usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<CredencialesUsuario> buscarCredencialesPorCorreo(String correo) {
        return usuarios.buscarPorCorreo(correo)
            .map(u -> new CredencialesUsuario(u.getId(), u.getCorreo(), u.getPasswordHash(), u.getRolId()));
    }
}
