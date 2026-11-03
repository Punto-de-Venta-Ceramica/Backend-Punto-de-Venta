package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.Area;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.port.AreaRepositorioPort;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class AreaPersistenceAdapter implements AreaRepositorioPort {

    private final AreaJpaRepository areas;
    private final EntityManager entityManager;

    public AreaPersistenceAdapter(AreaJpaRepository areas, EntityManager entityManager) {
        this.areas = areas;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Area guardar(Area area) {
        AreaEntity entity;
        if (area.getId() == null) {
            NegocioEntity negocio = entityManager.getReference(NegocioEntity.class, area.getNegocioId());
            entity = AreaMapper.toEntity(area, negocio);
        } else {
            entity = areas.findByIdAndNegocio_IdAndNegocio_ActivoTrue(area.getId(), area.getNegocioId())
                .orElseThrow(() -> new CatalogoNoEncontradoException("El área no existe o no tienes acceso."));
            AreaMapper.actualizar(area, entity);
        }
        return AreaMapper.toDomain(areas.saveAndFlush(entity));
    }

    @Override
    public Optional<Area> buscarPorId(Long areaId, Long negocioId) {
        return areas.findByIdAndNegocio_IdAndNegocio_ActivoTrue(areaId, negocioId).map(AreaMapper::toDomain);
    }

    @Override
    public List<Area> listarPorNegocio(Long negocioId) {
        return areas.findByNegocio_IdAndNegocio_ActivoTrueOrderByNombreAscIdAsc(negocioId).stream()
            .map(AreaMapper::toDomain).toList();
    }
}
