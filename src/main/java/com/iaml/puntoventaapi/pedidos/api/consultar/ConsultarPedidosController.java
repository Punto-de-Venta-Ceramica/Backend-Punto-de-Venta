package com.iaml.puntoventaapi.pedidos.api.consultar;

import com.iaml.puntoventaapi.pedidos.application.consultar.ConsultarPedidosService;
import com.iaml.puntoventaapi.pedidos.domain.port.ConsultaPedidosPort;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
public class ConsultarPedidosController {
    private final ConsultarPedidosService pedidos;
    private final UsuarioActual usuario;
    public ConsultarPedidosController(ConsultarPedidosService pedidos, UsuarioActual usuario) {
        this.pedidos = pedidos;
        this.usuario = usuario;
    }
    @GetMapping
    public List<ConsultaPedidosPort.Vista> listar(@RequestParam long sucursalId) {
        return pedidos.listar(sucursalId, usuario.id());
    }
    @GetMapping("/{id}")
    public ConsultaPedidosPort.Vista consultar(@PathVariable long id) {
        return pedidos.consultar(id, usuario.id());
    }
}
