package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.Producto;

public final class ProductoMapper {

    private ProductoMapper() {
    }

    public static Producto toDomain(ProductoEntity entity) {
        return new Producto(entity.getId(), entity.getCategoria().getId(), entity.getNombre(),
            entity.getPrecio(), entity.getImagenUrl(), entity.getImagenPublicId(), entity.isActivo());
    }

    public static ProductoEntity toEntity(Producto producto, CategoriaEntity categoria) {
        ProductoEntity entity = new ProductoEntity();
        actualizar(producto, entity, categoria);
        return entity;
    }

    public static void actualizar(Producto producto, ProductoEntity entity, CategoriaEntity categoria) {
        entity.setCategoria(categoria);
        entity.setNombre(producto.getNombre());
        entity.setPrecio(producto.getPrecio());
        entity.setImagenUrl(producto.getImagenUrl());
        entity.setImagenPublicId(producto.getImagenPublicId());
        entity.setActivo(producto.isActivo());
    }
}
