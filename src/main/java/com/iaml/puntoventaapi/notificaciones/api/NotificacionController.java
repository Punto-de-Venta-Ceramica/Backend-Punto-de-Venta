package com.iaml.puntoventaapi.notificaciones.api;

import com.iaml.puntoventaapi.notificaciones.application.NotificacionService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {
    private final NotificacionService notificaciones;
    private final UsuarioActual usuarioActual;

    public NotificacionController(NotificacionService notificaciones, UsuarioActual usuarioActual) {
        this.notificaciones = notificaciones;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public List<NotificacionService.Aviso> listar() {
        return notificaciones.listar(usuarioActual.id());
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable long id) {
        return notificaciones.marcarLeida(usuarioActual.id(), id)
            ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
