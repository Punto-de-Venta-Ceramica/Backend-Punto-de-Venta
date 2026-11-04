package com.iaml.puntoventaapi.pedidos.api.registrar;

import com.iaml.puntoventaapi.pedidos.application.registrar.RegistrarPedidoService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
public class RegistrarPedidoController {
    private final RegistrarPedidoService pedidos;
    private final UsuarioActual usuario;
    public RegistrarPedidoController(RegistrarPedidoService pedidos, UsuarioActual usuario) {
        this.pedidos = pedidos;
        this.usuario = usuario;
    }

    @PostMapping
    public ResponseEntity<RegistrarPedidoService.Resultado> registrar(@Valid @RequestBody RegistrarPedidoRequest request) {
        var resultado = pedidos.registrar(request.toCommand(usuario.id()));
        return ResponseEntity.created(URI.create("/api/pedidos/" + resultado.id())).body(resultado);
    }
}
