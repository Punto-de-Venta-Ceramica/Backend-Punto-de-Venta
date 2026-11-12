package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.ClasificacionProducto;
import com.iaml.puntoventaapi.catalogo.domain.FiltroProductos;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.ProductoDetalle;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import jakarta.persistence.EntityManager;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public Optional<ProductoDetalle> buscarConClasificacion(Long id, Long usuarioId) {
        return productos.buscarActivoConClasificacion(id, usuarioId).map(ProductoPersistenceAdapter::conClasificacion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoDetalle> buscarConClasificacionEnNegocio(Long id, Long negocioId) {
        return productos.buscarActivoConClasificacionEnNegocio(id, negocioId)
            .map(ProductoPersistenceAdapter::conClasificacion);
    }

    private static ProductoDetalle conClasificacion(ProductoEntity entity) {
        CategoriaEntity categoria = entity.getCategoria();
        AreaEntity area = categoria.getArea();
        return new ProductoDetalle(ProductoMapper.toDomain(entity), null,
            new ClasificacionProducto(area.getId(), area.getNombre(), categoria.getId(), categoria.getNombre()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarPorNegocio(Long negocioId) {
        return productos.listarActivosPorNegocio(negocioId).stream().map(ProductoMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Producto> listarPagina(Long negocioId, FiltroProductos filtro, int pagina, int tamanio) {
        Page<ProductoEntity> resultado = productos.buscarActivosPorNegocio(negocioId, filtro.areaId(),
            filtro.categoriaId(), patronBusqueda(filtro.texto()), filtro.sucursalId(),
            filtro.precioMinimo(), filtro.precioMaximo(),
            PageRequest.of(pagina, tamanio));
        return new Pagina<>(resultado.getContent().stream().map(ProductoMapper::toDomain).toList(),
            pagina, tamanio, resultado.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClasificacionProducto> buscarClasificacion(Long categoriaId) {
        return productos.clasificacionDeCategoria(categoriaId).stream().findFirst()
            .map(f -> new ClasificacionProducto((Long) f[2], (String) f[3], (Long) f[0], (String) f[1]));
    }

    /** %texto% en minúsculas con los comodines del propio texto escapados; sin texto coincide con todo. */
    private static String patronBusqueda(String texto) {
        if (texto == null) {
            return "%";
        }
        String escapado = texto.toLowerCase()
            .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
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
