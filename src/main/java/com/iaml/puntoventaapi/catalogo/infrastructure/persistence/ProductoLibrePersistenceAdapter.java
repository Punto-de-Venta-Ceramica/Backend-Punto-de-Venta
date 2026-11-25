package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.ProductoLibreContrato;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el producto "Artículo artesanal" colgado de un área y una categoría dadas de baja: así ninguna consulta del
 * catálogo (listados, edición, pedidos, filtros) lo muestra y no necesita filtros propios. Las clases de
 * clasificación solo permiten dar de baja desde este paquete, por eso la creación vive aquí.
 */
@Component
public class ProductoLibrePersistenceAdapter implements ProductoLibreContrato {

    private static final String CLASIFICACION = "Sin asignar";

    private final ProductoJpaRepository productos;
    private final EntityManager entityManager;

    public ProductoLibrePersistenceAdapter(ProductoJpaRepository productos, EntityManager entityManager) {
        this.productos = productos;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public ProductoLibre obtenerOCrear(Long negocioId) {
        var existente = productos.productosLibresDelNegocio(negocioId);
        if (!existente.isEmpty()) {
            return aRespuesta(normalizarNombre(existente.get(0)));
        }
        // Se bloquea el negocio y se vuelve a buscar para que dos cobros simultáneos no creen dos productos.
        if (entityManager.find(NegocioEntity.class, negocioId, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new IllegalStateException("El negocio no existe.");
        }
        existente = productos.productosLibresDelNegocio(negocioId);
        if (!existente.isEmpty()) {
            return aRespuesta(normalizarNombre(existente.get(0)));
        }

        Instant baja = Instant.now();
        AreaEntity area = new AreaEntity();
        area.setNegocio(entityManager.getReference(NegocioEntity.class, negocioId));
        area.setNombre(CLASIFICACION);
        area.setActivo(false);
        area.setFechaEliminacion(baja);
        entityManager.persist(area);

        CategoriaEntity categoria = new CategoriaEntity();
        categoria.setArea(area);
        categoria.setNombre(CLASIFICACION);
        categoria.setActivo(false);
        categoria.setFechaEliminacion(baja);
        entityManager.persist(categoria);

        ProductoEntity producto = new ProductoEntity();
        producto.setCategoria(categoria);
        producto.setNombre(NOMBRE);
        producto.setPrecio(BigDecimal.ZERO);
        producto.setPrecioVariable(true);
        entityManager.persist(producto);
        entityManager.flush();
        return aRespuesta(producto);
    }

    /** Un producto creado con un nombre anterior ("Varios") pasa al nombre vigente; las ventas ya hechas lo conservan. */
    private static ProductoEntity normalizarNombre(ProductoEntity producto) {
        if (!NOMBRE.equals(producto.getNombre())) {
            producto.setNombre(NOMBRE);
        }
        return producto;
    }

    private static ProductoLibre aRespuesta(ProductoEntity producto) {
        return new ProductoLibre(producto.getId(), producto.getNombre());
    }
}
