package com.iaml.puntoventaapi.inventario.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface InventarioJpaRepository extends JpaRepository<InventarioEntity, Long> {

    // Bloquea únicamente la fila de inventario (sin joins, para no bloquear negocio/sucursal/producto).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventarioEntity i where i.id = :id")
    Optional<InventarioEntity> buscarConBloqueo(@Param("id") Long id);

    // El registro es accesible si su producto está activo y su sucursal pertenece a un negocio activo del usuario.
    @Query("""
        select count(i) > 0
        from InventarioEntity i
        join i.producto p
        join i.sucursal s
        join s.negocio n
        join n.usuarios u
        where i.id = :id and u.id = :usuarioId and n.activo = true and p.activo = true
        """)
    boolean esAccesibleParaUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("""
        select n.id
        from ProductoEntity p
        join p.categoria c
        join c.area a
        join a.negocio n
        join n.usuarios u
        where p.id = :productoId and u.id = :usuarioId and n.activo = true and p.activo = true
        """)
    Optional<Long> negocioDeProducto(@Param("productoId") Long productoId, @Param("usuarioId") Long usuarioId);

    @Query("""
        select n.id
        from Sucursal s
        join s.negocio n
        join n.usuarios u
        where s.id = :sucursalId and u.id = :usuarioId and n.activo = true
        """)
    Optional<Long> negocioDeSucursal(@Param("sucursalId") Long sucursalId, @Param("usuarioId") Long usuarioId);

    @Query("select count(i) > 0 from InventarioEntity i where i.sucursal.id = :sucursalId and i.producto.id = :productoId")
    boolean existe(@Param("sucursalId") Long sucursalId, @Param("productoId") Long productoId);

    @Query("""
        select i.id, p.id, p.nombre, s.id, s.nombre, i.existencia
        from InventarioEntity i
        join i.producto p
        join i.sucursal s
        join s.negocio n
        join n.usuarios u
        where n.id = :negocioId and u.id = :usuarioId and n.activo = true and p.activo = true
        order by s.nombre, p.nombre, i.id
        """)
    List<Object[]> panelPorNegocio(@Param("negocioId") Long negocioId, @Param("usuarioId") Long usuarioId);

    @Query("""
        select i.id, p.id, p.nombre, s.id, s.nombre, i.existencia
        from InventarioEntity i
        join i.producto p
        join i.sucursal s
        join s.negocio n
        join n.usuarios u
        where n.id = :negocioId and s.id = :sucursalId and u.id = :usuarioId
          and n.activo = true and p.activo = true
        order by p.nombre, i.id
        """)
    List<Object[]> panelPorSucursal(@Param("negocioId") Long negocioId, @Param("sucursalId") Long sucursalId,
                                    @Param("usuarioId") Long usuarioId);
}
