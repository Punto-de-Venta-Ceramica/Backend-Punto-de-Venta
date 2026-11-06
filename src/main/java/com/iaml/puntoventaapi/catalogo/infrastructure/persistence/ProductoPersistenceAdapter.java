package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductoPersistenceAdapter implements ProductoRepositorioPort {

    private final ProductoJpaRepository productos;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Producto guardar(Producto producto) {
        CategoriaEntity categoria = entityManager.getReference(CategoriaEntity.class, producto.getCategoriaId());
        ProductoEntity entity;
        if (producto.getId() == null) {
            entity = ProductoMapper.toEntity(producto, categoria);
        } else {
            entity = productos.findById(producto.getId())
                .orElseThrow(() -> new ProductoNoEncontradoException());
            ProductoMapper.actualizar(producto, entity, categoria);
        }
        return ProductoMapper.toDomain(productos.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Producto> buscarPorId(Long id, Long usuarioId) {
        return productos.buscarActivoDelUsuario(id, usuarioId).map(ProductoMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarPorNegocio(Long negocioId) {
        return productos.listarActivosPorNegocio(negocioId).stream().map(ProductoMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existenPorCategoria(Long categoriaId) {
        return productos.existsByCategoria_Id(categoriaId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existenPorArea(Long areaId) {
        return productos.existsByCategoria_Area_Id(areaId);
    }
}
