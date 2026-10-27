package com.iaml.puntoventaapi.negocios.infrastructure.persistence;

import com.iaml.puntoventaapi.negocios.domain.Negocio;
import com.iaml.puntoventaapi.negocios.domain.NegocioResumen;
import com.iaml.puntoventaapi.negocios.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.negocios.domain.port.NegocioRepositorioPort;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NegocioPersistenceAdapter implements NegocioRepositorioPort {

    private final NegocioJpaRepository negocios;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Negocio guardar(Negocio negocio, Long usuarioId) {
        NegocioEntity entity;
        if (negocio.getId() == null) {
            var usuario = entityManager.getReference(UsuarioEntity.class, usuarioId);
            entity = NegocioMapper.toEntity(negocio, usuario);
        } else {
            entity = negocios.findByIdAndUsuarios_IdAndActivoTrue(negocio.getId(), usuarioId)
                .orElseThrow(NegocioNoEncontradoException::new);
            if (entity.getVersion() != negocio.getVersion()) {
                throw new ObjectOptimisticLockingFailureException(NegocioEntity.class, negocio.getId());
            }
            NegocioMapper.actualizar(negocio, entity);
        }
        return NegocioMapper.toDomain(negocios.saveAndFlush(entity));
    }

    @Override
    public Optional<Negocio> buscarPorId(Long negocioId, Long usuarioId) {
        return negocios.findByIdAndUsuarios_IdAndActivoTrue(negocioId, usuarioId).map(NegocioMapper::toDomain);
    }

    @Override
    public List<NegocioResumen> listarPorUsuario(Long usuarioId) {
        return negocios.findAllByUsuarios_IdAndActivoTrueOrderByNombreAscIdAsc(usuarioId);
    }

    @Override
    public boolean perteneceAUsuario(Long negocioId, Long usuarioId) {
        return negocios.existsByIdAndUsuarios_IdAndActivoTrue(negocioId, usuarioId);
    }
}
