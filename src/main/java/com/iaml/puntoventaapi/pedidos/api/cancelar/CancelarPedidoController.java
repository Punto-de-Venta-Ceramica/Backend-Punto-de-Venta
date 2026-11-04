package com.iaml.puntoventaapi.pedidos.api.cancelar;

import com.iaml.puntoventaapi.pedidos.application.cancelar.CancelarPedidoService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasRole('PROPIETARIO')")
public class CancelarPedidoController {
    private final CancelarPedidoService pedidos;
    private final UsuarioActual usuario;
    public CancelarPedidoController(CancelarPedidoService pedidos, UsuarioActual usuario) {
        this.pedidos = pedidos;
        this.usuario = usuario;
    }
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable long id) {
        pedidos.cancelar(id, usuario.id());
        return ResponseEntity.noContent().build();
    }
}
