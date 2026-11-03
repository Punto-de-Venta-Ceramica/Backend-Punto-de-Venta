package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface DescuentoJpaRepository extends JpaRepository<DescuentoEntity, Long> {

    @Query("""
        select d
        from DescuentoEntity d
        join d.productos p
        where p.id = :productoId and d.activo = true
        order by d.id
        """)
    List<DescuentoEntity> activosPorProducto(@Param("productoId") Long productoId);

    // Cada fila: [idProducto, DescuentoEntity].
    @Query("""
        select p.id, d
        from DescuentoEntity d
        join d.productos p
        where p.id in :productoIds and d.activo = true
        order by d.id
        """)
    List<Object[]> activosPorProductos(@Param("productoIds") Collection<Long> productoIds);

    @Query("select d from DescuentoEntity d join d.productos p where p.id = :productoId")
    List<DescuentoEntity> todosPorProducto(@Param("productoId") Long productoId);
}
