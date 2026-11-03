package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;

public final class DescuentoMapper {

    private DescuentoMapper() {
    }

    public static Descuento toDomain(DescuentoEntity entity) {
        return new Descuento(entity.getId(), entity.getNegocio().getId(), entity.getNombre(),
            entity.getCantidadMinima(), entity.getCantidadMaxima(), entity.getDescuento(),
            Boolean.TRUE.equals(entity.getActivo()));
    }

    public static DescuentoEntity toEntity(Descuento descuento, NegocioEntity negocio) {
        DescuentoEntity entity = new DescuentoEntity();
        entity.setNegocio(negocio);
        actualizar(descuento, entity);
        return entity;
    }

    public static void actualizar(Descuento descuento, DescuentoEntity entity) {
        entity.setNombre(descuento.getNombre());
        entity.setCantidadMinima(descuento.getCantidadMinima());
        entity.setCantidadMaxima(descuento.getCantidadMaxima());
        entity.setDescuento(descuento.getPorcentaje());
        entity.setActivo(descuento.isActivo());
    }
}
