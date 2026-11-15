package com.iaml.puntoventaapi.ventas.api;

import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import com.iaml.puntoventaapi.ventas.api.dto.PrepararVentaRequest;
import com.iaml.puntoventaapi.ventas.api.dto.PaginaProductosVentaResponse;
import com.iaml.puntoventaapi.ventas.api.dto.VentaResponse;
import com.iaml.puntoventaapi.ventas.api.dto.VentaConfirmadaResponse;
import com.iaml.puntoventaapi.ventas.application.ConfirmarVentaService;
import com.iaml.puntoventaapi.ventas.application.PrepararVentaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
    public PaginaProductosVentaResponse listarProductos(
            @RequestParam @Positive Long sucursalId,
            @RequestParam(defaultValue = "") @Size(max = 150) String buscar,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(100) int size) {
        return PaginaProductosVentaResponse.from(ventas.listarProductos(sucursalId, usuarioActual.id(), buscar, page, size));
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
