package com.iaml.puntoventaapi.catalogo.api;

import com.iaml.puntoventaapi.catalogo.api.dto.response.DescuentoResponse;
import com.iaml.puntoventaapi.catalogo.api.dto.request.GuardarDescuentoRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.request.GuardarDescuentosProductoRequest;
import com.iaml.puntoventaapi.catalogo.application.DescuentoService;
import com.iaml.puntoventaapi.catalogo.domain.DestinoDescuento;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/negocios/{negocioId}/descuentos")
@PreAuthorize("hasRole('PROPIETARIO')")
public class DescuentoController {

    private final DescuentoService descuentos;
    private final UsuarioActual usuarioActual;

    @PostMapping
    public ResponseEntity<DescuentoResponse> crear(@PathVariable @Positive Long negocioId,
                                                   @Valid @RequestBody GuardarDescuentoRequest request) {
        DescuentoResponse descuento = DescuentoResponse.from(
            descuentos.crear(negocioId, request.toCommand(), usuarioActual.id()));
        return ResponseEntity.created(URI.create("/api/negocios/" + negocioId + "/descuentos/" + descuento.id()))
            .body(descuento);
    }

    /** Con ?productoId devuelve solo los descuentos asignados directamente a ese producto. */
    @GetMapping
    public List<DescuentoResponse> listar(@PathVariable @Positive Long negocioId,
                                          @RequestParam(required = false) @Positive Long productoId) {
        return descuentos.listar(negocioId, productoId, usuarioActual.id()).stream()
            .map(DescuentoResponse::from).toList();
    }

    /** Guarda de una vez (todo o nada) los descuentos propios de un producto. */
    @PutMapping("/productos/{productoId}")
    public List<DescuentoResponse> guardarDeProducto(@PathVariable @Positive Long negocioId,
                                                     @PathVariable @Positive Long productoId,
                                                     @Valid @RequestBody GuardarDescuentosProductoRequest request) {
        return descuentos.guardarDeProducto(negocioId, productoId, request.idsAEliminar(), request.toCommands(),
            usuarioActual.id()).stream().map(DescuentoResponse::from).toList();
    }

    @GetMapping("/{descuentoId}")
    public DescuentoResponse consultar(@PathVariable @Positive Long negocioId,
                                       @PathVariable @Positive Long descuentoId) {
        return DescuentoResponse.from(descuentos.consultar(negocioId, descuentoId, usuarioActual.id()));
    }

    @PutMapping("/{descuentoId}")
    public DescuentoResponse editar(@PathVariable @Positive Long negocioId,
                                    @PathVariable @Positive Long descuentoId,
                                    @Valid @RequestBody GuardarDescuentoRequest request) {
        return DescuentoResponse.from(
            descuentos.editar(negocioId, descuentoId, request.toCommand(), usuarioActual.id()));
    }

    @DeleteMapping("/{descuentoId}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long negocioId,
                                         @PathVariable @Positive Long descuentoId) {
        descuentos.eliminar(negocioId, descuentoId, usuarioActual.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{descuentoId}/activar")
    public DescuentoResponse activar(@PathVariable @Positive Long negocioId,
                                     @PathVariable @Positive Long descuentoId) {
        return DescuentoResponse.from(descuentos.cambiarEstado(negocioId, descuentoId, true, usuarioActual.id()));
    }

    @PatchMapping("/{descuentoId}/desactivar")
    public DescuentoResponse desactivar(@PathVariable @Positive Long negocioId,
                                        @PathVariable @Positive Long descuentoId) {
        return DescuentoResponse.from(descuentos.cambiarEstado(negocioId, descuentoId, false, usuarioActual.id()));
    }

    // ---- Asignaciones (idempotentes): devuelven el descuento con sus asignaciones actualizadas.

    @PutMapping("/{descuentoId}/productos/{productoId}")
    public DescuentoResponse asignarProducto(@PathVariable @Positive Long negocioId,
                                             @PathVariable @Positive Long descuentoId,
                                             @PathVariable @Positive Long productoId) {
        return asignar(negocioId, descuentoId, DestinoDescuento.PRODUCTO, productoId);
    }

    @DeleteMapping("/{descuentoId}/productos/{productoId}")
    public DescuentoResponse quitarProducto(@PathVariable @Positive Long negocioId,
                                            @PathVariable @Positive Long descuentoId,
                                            @PathVariable @Positive Long productoId) {
        return quitar(negocioId, descuentoId, DestinoDescuento.PRODUCTO, productoId);
    }

    @PutMapping("/{descuentoId}/categorias/{categoriaId}")
    public DescuentoResponse asignarCategoria(@PathVariable @Positive Long negocioId,
                                              @PathVariable @Positive Long descuentoId,
                                              @PathVariable @Positive Long categoriaId) {
        return asignar(negocioId, descuentoId, DestinoDescuento.CATEGORIA, categoriaId);
    }

    @DeleteMapping("/{descuentoId}/categorias/{categoriaId}")
    public DescuentoResponse quitarCategoria(@PathVariable @Positive Long negocioId,
                                             @PathVariable @Positive Long descuentoId,
                                             @PathVariable @Positive Long categoriaId) {
        return quitar(negocioId, descuentoId, DestinoDescuento.CATEGORIA, categoriaId);
    }

    @PutMapping("/{descuentoId}/areas/{areaId}")
    public DescuentoResponse asignarArea(@PathVariable @Positive Long negocioId,
                                         @PathVariable @Positive Long descuentoId,
                                         @PathVariable @Positive Long areaId) {
        return asignar(negocioId, descuentoId, DestinoDescuento.AREA, areaId);
    }

    @DeleteMapping("/{descuentoId}/areas/{areaId}")
    public DescuentoResponse quitarArea(@PathVariable @Positive Long negocioId,
                                        @PathVariable @Positive Long descuentoId,
                                        @PathVariable @Positive Long areaId) {
        return quitar(negocioId, descuentoId, DestinoDescuento.AREA, areaId);
    }

    private DescuentoResponse asignar(Long negocioId, Long descuentoId, DestinoDescuento destino, Long destinoId) {
        return DescuentoResponse.from(
            descuentos.asignar(negocioId, descuentoId, destino, destinoId, usuarioActual.id()));
    }

    private DescuentoResponse quitar(Long negocioId, Long descuentoId, DestinoDescuento destino, Long destinoId) {
        return DescuentoResponse.from(
            descuentos.quitar(negocioId, descuentoId, destino, destinoId, usuarioActual.id()));
    }
}
