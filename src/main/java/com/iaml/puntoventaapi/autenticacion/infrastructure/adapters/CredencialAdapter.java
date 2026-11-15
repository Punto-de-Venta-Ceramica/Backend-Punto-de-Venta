package com.iaml.puntoventaapi.autenticacion.infrastructure.adapters;

import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import com.iaml.puntoventaapi.autenticacion.domain.port.CredencialRepositorioPort;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolJpaRepository;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolMapper;
import com.iaml.puntoventaapi.usuarios.CredencialesUsuario;
import com.iaml.puntoventaapi.usuarios.UsuariosContrato;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * Obtiene el usuario mediante el contrato de usuarios y su rol de la persistencia propia.
 */
@Component
public class CredencialAdapter implements CredencialRepositorioPort {

    private final UsuariosContrato usuarios;
    private final RolJpaRepository roles;

    public CredencialAdapter(UsuariosContrato usuarios, RolJpaRepository roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Credencial> buscarPorCorreo(String correo) {
        return usuarios.buscarCredencialesPorCorreo(correo).flatMap(this::aCredencial);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Credencial> buscarPorUsuarioId(Long usuarioId) {
        return usuarios.buscarCredencialesPorId(usuarioId).flatMap(this::aCredencial);
    }

    private Optional<Credencial> aCredencial(CredencialesUsuario u) {
        return roles.findWithPermisosById(u.rolId())
            .map(rol -> new Credencial(u.usuarioId(), u.correo(), u.passwordHash(), RolMapper.toDomain(rol)));
    }
}
