package com.iaml.puntoventaapi.pedidos.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoJpaRepository extends JpaRepository<Pedido, Long> {
    @EntityGraph(attributePaths = {"usuario", "usuario.rol", "sucursal"})
    Optional<Pedido> findWithUsuarioById(Long id);
}
