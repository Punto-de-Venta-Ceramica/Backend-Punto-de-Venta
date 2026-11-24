package com.iaml.puntoventaapi.notificaciones.infrastructure.persistence;

import com.iaml.puntoventaapi.notificaciones.domain.Notificacion;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    @EntityGraph(attributePaths = "pedido")
    Page<Notificacion> findByUsuario_IdOrderByFechaDescIdDesc(Long usuarioId, Pageable pageable);
    Optional<Notificacion> findByIdAndUsuario_Id(Long id, Long usuarioId);
    long countByUsuario_IdAndLeidaFalse(Long usuarioId);
}
