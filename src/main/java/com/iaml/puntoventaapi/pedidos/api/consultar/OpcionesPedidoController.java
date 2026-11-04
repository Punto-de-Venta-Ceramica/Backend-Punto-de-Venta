package com.iaml.puntoventaapi.pedidos.api.consultar;

import com.iaml.puntoventaapi.pedidos.domain.port.OpcionesPedidoPort;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/pedidos/opciones")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
public class OpcionesPedidoController {
    private final OpcionesPedidoPort opciones;
    private final UsuarioActual usuario;
    public OpcionesPedidoController(OpcionesPedidoPort opciones, UsuarioActual usuario) {
        this.opciones = opciones;
        this.usuario = usuario;
    }
    @GetMapping("/productos")
    public List<OpcionesPedidoPort.Producto> productos(@RequestParam long sucursalId) {
        return opciones.productos(sucursalId, usuario.id());
    }
    @GetMapping("/cortes")
    public List<OpcionesPedidoPort.Corte> cortes(@RequestParam long sucursalId) {
        return opciones.cortesAbiertos(sucursalId, usuario.id());
    }
    @GetMapping("/sucursal-asignada")
    public ResponseEntity<OpcionesPedidoPort.SucursalAsignada> sucursalAsignada() {
        return ResponseEntity.of(opciones.sucursalAsignada(usuario.id()));
    }
}
