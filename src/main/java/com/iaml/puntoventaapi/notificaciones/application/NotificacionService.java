package com.iaml.puntoventaapi.notificaciones.application;

import com.iaml.puntoventaapi.notificaciones.NotificacionesContrato;
import com.iaml.puntoventaapi.notificaciones.domain.Notificacion;
import com.iaml.puntoventaapi.notificaciones.infrastructure.persistence.NotificacionRepository;
import com.iaml.puntoventaapi.pedidos.infrastructure.persistence.Pedido;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionService implements NotificacionesContrato {
    private final NotificacionRepository notificaciones;
    private final EntityManager entidades;

    public NotificacionService(NotificacionRepository notificaciones, EntityManager entidades) {
        this.notificaciones = notificaciones;
        this.entidades = entidades;
    }

    public record Aviso(long id, String titulo, String mensaje, Instant fecha, boolean leida, Long pedidoId) { }
    public record ConteoNoLeidas(long cantidad) { }

    @Transactional(readOnly = true)
    public ConteoNoLeidas contarNoLeidas(long usuarioId) {
        if (usuarioId <= 0) throw new IllegalArgumentException("El usuario no es válido.");
        return new ConteoNoLeidas(notificaciones.countByUsuario_IdAndLeidaFalse(usuarioId));
    }

    @Override
    @Transactional
    public void registrarParaPedido(long destinatarioId, long pedidoId, String titulo, String mensaje) {
        if (destinatarioId <= 0 || pedidoId <= 0 || titulo == null || titulo.isBlank()
                || titulo.length() > 200 || mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("Los datos de la notificación no son válidos.");
        }
        var aviso = new Notificacion();
        aviso.setUsuario(entidades.getReference(UsuarioEntity.class, destinatarioId));
        aviso.setPedido(entidades.getReference(Pedido.class, pedidoId));
        aviso.setTitulo(titulo.trim());
        aviso.setMensaje(mensaje);
        aviso.setLeida(false);
        notificaciones.save(aviso);
    }

    @Transactional(readOnly = true)
    public List<Aviso> listar(long usuarioId) {
        return notificaciones.findTop100ByUsuario_IdOrderByLeidaAscFechaDescIdDesc(usuarioId).stream()
            .map(n -> new Aviso(n.getId(), n.getTitulo(), n.getMensaje(), n.getFecha(),
                Boolean.TRUE.equals(n.getLeida()), n.getPedido() == null ? null : n.getPedido().getId()))
            .toList();
    }

    @Transactional
    public boolean marcarLeida(long usuarioId, long notificacionId) {
        return notificaciones.findByIdAndUsuario_Id(notificacionId, usuarioId).map(n -> {
            n.setLeida(true);
            return true;
        }).orElse(false);
    }
}
