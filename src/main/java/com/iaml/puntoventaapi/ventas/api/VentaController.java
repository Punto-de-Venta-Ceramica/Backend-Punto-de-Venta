package com.iaml.puntoventaapi.ventas.api;

import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import com.iaml.puntoventaapi.ventas.api.dto.PrepararVentaRequest;
import com.iaml.puntoventaapi.ventas.api.dto.ProductoDisponibleResponse;
import com.iaml.puntoventaapi.ventas.api.dto.VentaResponse;
import com.iaml.puntoventaapi.ventas.api.dto.VentaConfirmadaResponse;
import com.iaml.puntoventaapi.ventas.application.ConfirmarVentaService;
import com.iaml.puntoventaapi.ventas.application.PrepararVentaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    private final ConfirmarVentaService confirmacion;
    private final UsuarioActual usuarioActual;

    public VentaController(PrepararVentaService ventas, ConfirmarVentaService confirmacion,
                           UsuarioActual usuarioActual) {
        this.ventas = ventas;
        this.confirmacion = confirmacion;
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

    @PostMapping("/confirmar")
    @ResponseStatus(HttpStatus.CREATED)
    public VentaConfirmadaResponse confirmar(
            @Valid @RequestBody PrepararVentaRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID claveOperacion) {
        return VentaConfirmadaResponse.from(
            confirmacion.confirmar(request.toCommand(), usuarioActual.id(), claveOperacion));
    }
}
