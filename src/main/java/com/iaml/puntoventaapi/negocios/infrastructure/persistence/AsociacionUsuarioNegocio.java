package com.iaml.puntoventaapi.negocios.infrastructure.persistence;

import com.iaml.puntoventaapi.negocios.domain.Negocio;
import com.iaml.puntoventaapi.shared.security.UsuarioNoAutenticadoException;
import com.iaml.puntoventaapi.usuarios.UsuariosContrato;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AsociacionUsuarioNegocio {

    private final UsuariosContrato usuarios;
    private final EntityManager entityManager;

    public void asociar(Negocio negocio, Long usuarioId) {
        if (!usuarios.existePorId(usuarioId)) {
            throw new UsuarioNoAutenticadoException();
        }
        negocio.getUsuarios().add(entityManager.getReference(UsuarioEntity.class, usuarioId));
    }
}
