package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.port.CategoriaRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.Categoria;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoriaPersistenceAdapter implements CategoriaRepositorioPort {

    private final CategoriaJpaRepository categorias;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> buscarNegocioId(Long categoriaId) {
        return categorias.buscarNegocioId(categoriaId);
    }

    @Override
    @Transactional
    public Categoria guardar(Categoria categoria, Long negocioId) {
        CategoriaEntity entity;
        if (categoria.getId() == null) {
            AreaEntity area = entityManager.getReference(AreaEntity.class, categoria.getAreaId());
            entity = CategoriaMapper.toEntity(categoria, area);
        } else {
            entity = categorias.findByIdAndArea_IdAndArea_Negocio_IdAndArea_Negocio_ActivoTrue(
                categoria.getId(), categoria.getAreaId(), negocioId)
                .orElseThrow(() -> new CatalogoNoEncontradoException("La categoría no existe o no tienes acceso."));
            CategoriaMapper.actualizar(categoria, entity);
        }
        return CategoriaMapper.toDomain(categorias.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Categoria> buscarPorId(Long categoriaId, Long areaId, Long negocioId) {
        return categorias.findByIdAndArea_IdAndArea_Negocio_IdAndArea_Negocio_ActivoTrue(
            categoriaId, areaId, negocioId).map(CategoriaMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarPorArea(Long areaId, Long negocioId) {
        return categorias.findByArea_IdAndArea_Negocio_IdAndArea_Negocio_ActivoTrueOrderByNombreAscIdAsc(
            areaId, negocioId).stream().map(CategoriaMapper::toDomain).toList();
    }
}
