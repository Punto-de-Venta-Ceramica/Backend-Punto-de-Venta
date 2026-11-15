package com.iaml.puntoventaapi.ventas.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VentaJpaRepository extends JpaRepository<Venta, Long> {
    @Query("select v from Venta v where v.caja.sucursal.id = :sucursalId and v.claveOperacion = :clave")
    Optional<Venta> buscarPorOperacion(@Param("sucursalId") Long sucursalId, @Param("clave") UUID claveOperacion);

    @Query("select v.caja.sucursal.id from Venta v where v.id = :ventaId")
    Optional<Long> buscarSucursalDeVenta(@Param("ventaId") Long ventaId);
}
