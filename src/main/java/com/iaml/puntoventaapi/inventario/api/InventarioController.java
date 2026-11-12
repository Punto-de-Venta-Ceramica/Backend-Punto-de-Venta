package com.iaml.puntoventaapi.inventario.api;

import com.iaml.puntoventaapi.inventario.api.dto.AjustarExistenciaRequest;
import com.iaml.puntoventaapi.inventario.api.dto.AsignarProductoRequest;
import com.iaml.puntoventaapi.inventario.api.dto.ExistenciaResponse;
import com.iaml.puntoventaapi.inventario.api.dto.InventarioPanelResponse;
import com.iaml.puntoventaapi.inventario.application.InventarioService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventario")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
public class InventarioController {

    private final InventarioService inventario;
    private final UsuarioActual usuarioActual;

    /**
     * Panel global: existencias de todas las sucursales del negocio (propietario), o de una si se envía
     * sucursalId (propietario o empleado asignado a ella).
     */
    @GetMapping
    public List<InventarioPanelResponse> panel(@RequestParam Long negocioId,
                                               @RequestParam(required = false) Long sucursalId) {
        return inventario.consultarPanel(negocioId, sucursalId, usuarioActual.id()).stream()
            .map(InventarioPanelResponse::from).toList();
    }

    /** Asigna un producto existente a una sucursal del mismo negocio. */
    @PostMapping
    @PreAuthorize("hasRole('PROPIETARIO')")
    public ResponseEntity<ExistenciaResponse> asignar(@Valid @RequestBody AsignarProductoRequest request) {
        ExistenciaResponse creada = ExistenciaResponse.from(inventario.asignar(request.productoId(),
            request.sucursalId(), request.existenciaInicial(), usuarioActual.id()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /** Ajuste manual: cantidad positiva suma, negativa resta. */
    @PatchMapping("/{id}/existencia")
    public ExistenciaResponse ajustar(@PathVariable Long id, @Valid @RequestBody AjustarExistenciaRequest request) {
        return ExistenciaResponse.from(inventario.ajustar(id, request.cantidad(), usuarioActual.id()));
    }
}
