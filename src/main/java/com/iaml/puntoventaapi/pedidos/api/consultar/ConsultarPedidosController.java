package com.iaml.puntoventaapi.pedidos.api.consultar;

import com.iaml.puntoventaapi.pedidos.application.consultar.ConsultarPedidosService;
import com.iaml.puntoventaapi.pedidos.domain.EstadoPedido;
import com.iaml.puntoventaapi.pedidos.domain.port.ConsultaPedidosPort;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
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
    /**
     * Listado paginado (20 por página, ?page=0), del más reciente al más antiguo. ?estado= filtra por estado;
     * sin él devuelve todos. No incluye el folio: está en GET /api/pedidos/{id}.
     */
    @GetMapping
    public PaginaPedidosResponse<ConsultaPedidosPort.Resumen> listar(
            @RequestParam long sucursalId,
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(defaultValue = "0") int page) {
        return PaginaPedidosResponse.from(pedidos.listar(sucursalId, usuario.id(), estado, page));
    }
    @GetMapping("/{id}")
    public ConsultaPedidosPort.Vista consultar(@PathVariable long id) {
        return pedidos.consultar(id, usuario.id());
    }
}
