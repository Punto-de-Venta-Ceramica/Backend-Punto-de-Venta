package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.AsignacionesDescuento;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.DestinoDescuento;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoGestionPort;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoRepositorioPort;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DescuentoPersistenceAdapter implements DescuentoRepositorioPort, DescuentoGestionPort {

    private final DescuentoJpaRepository descuentos;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Descuento guardar(Descuento descuento) {
        DescuentoEntity entity;
        if (descuento.getId() == null) {
            NegocioEntity negocio = entityManager.getReference(NegocioEntity.class, descuento.getNegocioId());
            entity = DescuentoMapper.toEntity(descuento, negocio);
        } else {
            entity = descuentos.findById(descuento.getId())
                .orElseThrow(() -> new IllegalStateException("El descuento no existe."));
            DescuentoMapper.actualizar(descuento, entity);
        }
        return DescuentoMapper.toDomain(descuentos.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void vincularAProducto(Long descuentoId, Long productoId) {
        asignar(descuentoId, DestinoDescuento.PRODUCTO, productoId);
    }

    // ---- Descuento que aplica a un producto: producto > categoría > área, y a igual nivel el de menor id.

    @Override
    @Transactional(readOnly = true)
    public Optional<Descuento> buscarActivoPorProducto(Long productoId) {
        return Optional.ofNullable(buscarActivosPorProductos(List.of(productoId)).get(productoId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Descuento> buscarActivosPorProductos(Collection<Long> productoIds) {
        Map<Long, Descuento> resultado = new HashMap<>();
        if (productoIds.isEmpty()) {
            return resultado;
        }
        agregarPrimeros(resultado, descuentos.activosDirectosPorProductos(productoIds));
        agregarPrimeros(resultado, descuentos.activosPorCategoriaDeProductos(productoIds));
        agregarPrimeros(resultado, descuentos.activosPorAreaDeProductos(productoIds));
        return resultado;
    }

    /** Las filas llegan ordenadas por id de descuento: el primero de cada producto, si aún no tiene, gana. */
    private static void agregarPrimeros(Map<Long, Descuento> resultado, List<Object[]> filas) {
        for (Object[] fila : filas) {
            resultado.putIfAbsent(((Number) fila[0]).longValue(), DescuentoMapper.toDomain((DescuentoEntity) fila[1]));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Descuento> buscarPropioActivoDeProducto(Long productoId) {
        return descuentos.propiosDeProducto(productoId).stream()
            .filter(d -> Boolean.TRUE.equals(d.getActivo()))
            .findFirst().map(DescuentoMapper::toDomain);
    }

    // ---- Gestión de descuentos

    @Override
    @Transactional(readOnly = true)
    public Optional<Descuento> buscarPorId(Long id, Long negocioId) {
        return descuentos.findByIdAndNegocioId(id, negocioId).map(DescuentoMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Descuento> listarPorNegocio(Long negocioId) {
        return descuentos.findByNegocioIdOrderById(negocioId).stream().map(DescuentoMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void eliminar(Long descuentoId) {
        // Las tablas de asignación pertenecen a DescuentoEntity: sus filas se borran con el descuento.
        descuentos.deleteById(descuentoId);
        descuentos.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public AsignacionesDescuento asignaciones(Long descuentoId) {
        return asignacionesDe(List.of(descuentoId)).get(descuentoId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AsignacionesDescuento> asignacionesDe(Collection<Long> descuentoIds) {
        Map<Long, AsignacionesDescuento> resultado = new HashMap<>();
        if (descuentoIds.isEmpty()) {
            return resultado;
        }
        Map<Long, Set<Long>> productos = agrupar(descuentos.productosAsignados(descuentoIds));
        Map<Long, Set<Long>> categorias = agrupar(descuentos.categoriasAsignadas(descuentoIds));
        Map<Long, Set<Long>> areas = agrupar(descuentos.areasAsignadas(descuentoIds));
        for (Long id : descuentoIds) {
            resultado.put(id, new AsignacionesDescuento(productos.getOrDefault(id, Set.of()),
                categorias.getOrDefault(id, Set.of()), areas.getOrDefault(id, Set.of())));
        }
        return resultado;
    }

    private static Map<Long, Set<Long>> agrupar(List<Object[]> filas) {
        Map<Long, Set<Long>> agrupado = new HashMap<>();
        for (Object[] fila : filas) {
            agrupado.computeIfAbsent(((Number) fila[0]).longValue(), k -> new HashSet<>())
                .add(((Number) fila[1]).longValue());
        }
        return agrupado;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean destinoPerteneceAlNegocio(DestinoDescuento destino, Long destinoId, Long negocioId) {
        return switch (destino) {
            case PRODUCTO -> descuentos.productoActivoDelNegocio(destinoId, negocioId);
            case CATEGORIA -> descuentos.categoriaActivaDelNegocio(destinoId, negocioId);
            case AREA -> descuentos.areaActivaDelNegocio(destinoId, negocioId);
        };
    }

    @Override
    @Transactional
    public void asignar(Long descuentoId, DestinoDescuento destino, Long destinoId) {
        DescuentoEntity entity = cargar(descuentoId);
        switch (destino) {
            case PRODUCTO -> entity.getProductos()
                .add(entityManager.getReference(ProductoEntity.class, destinoId));
            case CATEGORIA -> entity.getCategorias()
                .add(entityManager.getReference(CategoriaEntity.class, destinoId));
            case AREA -> entity.getAreas()
                .add(entityManager.getReference(AreaEntity.class, destinoId));
        }
        descuentos.saveAndFlush(entity);
    }

    @Override
    @Transactional
    public void quitar(Long descuentoId, DestinoDescuento destino, Long destinoId) {
        DescuentoEntity entity = cargar(descuentoId);
        switch (destino) {
            case PRODUCTO -> entity.getProductos().removeIf(p -> destinoId.equals(p.getId()));
            case CATEGORIA -> entity.getCategorias().removeIf(c -> destinoId.equals(c.getId()));
            case AREA -> entity.getAreas().removeIf(a -> destinoId.equals(a.getId()));
        }
        descuentos.saveAndFlush(entity);
    }

    private DescuentoEntity cargar(Long descuentoId) {
        return descuentos.findById(descuentoId)
            .orElseThrow(() -> new IllegalStateException("El descuento no existe."));
    }
}
