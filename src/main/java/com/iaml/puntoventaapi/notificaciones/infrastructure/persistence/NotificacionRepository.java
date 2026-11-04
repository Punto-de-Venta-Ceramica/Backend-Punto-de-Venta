package com.iaml.puntoventaapi.notificaciones.infrastructure.persistence;

import com.iaml.puntoventaapi.notificaciones.domain.Notificacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    @EntityGraph(attributePaths = "pedido")
    List<Notificacion> findTop100ByUsuario_IdOrderByFechaDescIdDesc(Long usuarioId);
    Optional<Notificacion> findByIdAndUsuario_Id(Long id, Long usuarioId);
}
