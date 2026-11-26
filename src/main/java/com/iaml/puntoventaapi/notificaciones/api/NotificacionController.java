package com.iaml.puntoventaapi.notificaciones.api;

import com.iaml.puntoventaapi.notificaciones.application.NotificacionService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** Paginado (20 por página, ?page=0), del más reciente al más antiguo. */
    @GetMapping
    public NotificacionService.PaginaAvisos listar(@RequestParam(defaultValue = "0") int page) {
        return notificaciones.listar(usuarioActual.id(), page);
    }

    @GetMapping("/no-leidas/conteo")
    public NotificacionService.ConteoNoLeidas contarNoLeidas() {
        return notificaciones.contarNoLeidas(usuarioActual.id());
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable long id) {
        return notificaciones.marcarLeida(usuarioActual.id(), id)
            ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
