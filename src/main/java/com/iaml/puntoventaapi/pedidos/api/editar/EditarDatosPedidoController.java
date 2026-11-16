package com.iaml.puntoventaapi.pedidos.api.editar;

import com.iaml.puntoventaapi.pedidos.application.editar.EditarDatosPedidoService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasRole('PROPIETARIO')")
public class EditarDatosPedidoController {
    private final EditarDatosPedidoService pedidos;
    private final UsuarioActual usuario;
    public EditarDatosPedidoController(EditarDatosPedidoService pedidos, UsuarioActual usuario) {
        this.pedidos = pedidos;
        this.usuario = usuario;
    }

    @PutMapping("/{id}/datos")
    public ResponseEntity<Void> editar(@PathVariable long id, @Valid @RequestBody EditarDatosPedidoRequest request) {
        pedidos.editar(id, usuario.id(), request.clienteDomain(), request.datosDomain());
        return ResponseEntity.noContent().build();
    }
}
