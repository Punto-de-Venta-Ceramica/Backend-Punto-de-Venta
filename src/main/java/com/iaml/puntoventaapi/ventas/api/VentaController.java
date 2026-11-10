package com.iaml.puntoventaapi.ventas.api;

import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import com.iaml.puntoventaapi.ventas.api.dto.PrepararVentaRequest;
import com.iaml.puntoventaapi.ventas.api.dto.ProductoDisponibleResponse;
import com.iaml.puntoventaapi.ventas.api.dto.VentaResponse;
import com.iaml.puntoventaapi.ventas.application.PrepararVentaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final PrepararVentaService ventas;
    private final UsuarioActual usuarioActual;

    public VentaController(PrepararVentaService ventas, UsuarioActual usuarioActual) {
        this.ventas = ventas;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping("/productos")
    public List<ProductoDisponibleResponse> listarProductos(@RequestParam @Positive Long sucursalId) {
        return ventas.listarProductos(sucursalId, usuarioActual.id()).stream()
            .map(ProductoDisponibleResponse::from).toList();
    }

    @PostMapping("/preparar")
    public VentaResponse preparar(@Valid @RequestBody PrepararVentaRequest request) {
        return VentaResponse.from(ventas.preparar(request.toCommand(), usuarioActual.id()));
    }
}
