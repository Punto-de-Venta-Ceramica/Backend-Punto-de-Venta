package com.iaml.puntoventaapi.usuarios.application;

import com.iaml.puntoventaapi.autenticacion.AutenticacionContrato;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import com.iaml.puntoventaapi.usuarios.domain.exception.CorreoYaRegistradoException;
import com.iaml.puntoventaapi.usuarios.domain.port.UsuarioRepositorioPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropietarioService {

    private static final String ROL_PROPIETARIO = "PROPIETARIO";

    private final UsuarioRepositorioPort usuarios;
    private final AutenticacionContrato autenticacion;

    public PropietarioService(UsuarioRepositorioPort usuarios, AutenticacionContrato autenticacion) {
        this.usuarios = usuarios;
        this.autenticacion = autenticacion;
    }

    @Transactional
    public Usuario registrar(RegistrarPropietarioCommand command) {
        String correo = command.correo().trim();
        if (usuarios.buscarPorCorreo(correo).isPresent()) {
            throw new CorreoYaRegistradoException();
        }
        Long rolId = autenticacion.buscarRolIdPorNombre(ROL_PROPIETARIO)
            .orElseThrow(() -> new IllegalStateException("No existe el rol " + ROL_PROPIETARIO));
        String passwordHash = autenticacion.hashearPassword(command.password());
        return usuarios.guardar(new Usuario(null, rolId, command.nombre().trim(), command.apellidoPaterno(),
            command.apellidoMaterno(), correo, command.telefono(), passwordHash));
    }
}
