package com.iaml.puntoventaapi.ventas.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleVentaJpaRepository extends JpaRepository<DetalleVenta, DetalleVentaId> {
    @EntityGraph(attributePaths = "producto")
    List<DetalleVenta> findByVenta_IdOrderById_ProductoIdAsc(Long ventaId);
}
