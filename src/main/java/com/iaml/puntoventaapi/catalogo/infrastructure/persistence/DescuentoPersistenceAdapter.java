package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoRepositorioPort;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DescuentoPersistenceAdapter implements DescuentoRepositorioPort {

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
        DescuentoEntity entity = descuentos.findById(descuentoId)
            .orElseThrow(() -> new IllegalStateException("El descuento no existe."));
        entity.getProductos().add(entityManager.getReference(ProductoEntity.class, productoId));
        descuentos.saveAndFlush(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Descuento> buscarActivoPorProducto(Long productoId) {
        return descuentos.activosPorProducto(productoId).stream().findFirst().map(DescuentoMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Descuento> buscarActivosPorProductos(Collection<Long> productoIds) {
        Map<Long, Descuento> resultado = new HashMap<>();
        if (productoIds.isEmpty()) {
            return resultado;
        }
        for (Object[] fila : descuentos.activosPorProductos(productoIds)) {
            resultado.putIfAbsent(((Number) fila[0]).longValue(), DescuentoMapper.toDomain((DescuentoEntity) fila[1]));
        }
        return resultado;
    }

    @Override
    @Transactional
    public void quitarDeProducto(Long productoId) {
        List<DescuentoEntity> vinculados = descuentos.todosPorProducto(productoId);
        for (DescuentoEntity descuento : vinculados) {
            descuento.getProductos().removeIf(p -> productoId.equals(p.getId()));
            if (descuento.getProductos().isEmpty()) {
                descuentos.delete(descuento);
            }
        }
        descuentos.flush();
    }
}
