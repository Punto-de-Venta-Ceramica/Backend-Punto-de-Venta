package com.iaml.puntoventaapi.catalogo.api;

import com.iaml.puntoventaapi.catalogo.api.dto.request.CrearProductoRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.request.EditarProductoRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.response.PaginaResponse;
import com.iaml.puntoventaapi.catalogo.api.dto.response.ProductoCatalogoResponse;
import com.iaml.puntoventaapi.catalogo.api.dto.response.ProductoResponse;
import com.iaml.puntoventaapi.catalogo.api.dto.response.ProductoResumenResponse;
import com.iaml.puntoventaapi.catalogo.application.ProductoService;
import com.iaml.puntoventaapi.catalogo.domain.FiltroProductos;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos")
/** El empleado solo puede listar y consultar productos de su sucursal; el resto es del propietario. */
@PreAuthorize("hasRole('PROPIETARIO')")
public class ProductoController {

    private final ProductoService productos;
    private final UsuarioActual usuarioActual;

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody CrearProductoRequest request) {
        ProductoResponse producto = ProductoResponse.from(productos.crear(request.toCommand(), usuarioActual.id()));
        return ResponseEntity.created(URI.create("/api/productos/" + producto.id())).body(producto);
    }

    /**
     * Listado paginado (20 por página, ?page=0&size=20) con lo mínimo para las tarjetas. Siempre ordenado por
     * nombre: se ignora cualquier ?sort. Los filtros son opcionales.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
    public PaginaResponse<ProductoResumenResponse> listar(
            @RequestParam Long negocioId,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) BigDecimal precioMinimo,
            @RequestParam(required = false) BigDecimal precioMaximo,
            @PageableDefault(size = 20) Pageable pageable) {
        FiltroProductos filtro = new FiltroProductos(areaId, categoriaId, buscar, sucursalId,
            precioMinimo, precioMaximo);
        return PaginaResponse.from(productos.listar(negocioId, usuarioActual.id(), filtro,
            pageable.getPageNumber(), pageable.getPageSize()).mapear(ProductoResumenResponse::from));
    }

    /** Todos los productos del negocio con id, nombre y categoría, sin paginar (selectores de otros módulos). */
    @GetMapping("/catalogo")
    public List<ProductoCatalogoResponse> catalogo(@RequestParam Long negocioId) {
        return productos.catalogo(negocioId, usuarioActual.id()).stream()
            .map(ProductoCatalogoResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
    public ProductoResponse consultar(@PathVariable Long id, @RequestParam(required = false) Long sucursalId) {
        return ProductoResponse.from(productos.consultar(id, usuarioActual.id(), sucursalId));
    }

    @PutMapping("/{id}")
    public ProductoResponse editar(@PathVariable Long id, @Valid @RequestBody EditarProductoRequest request) {
        return ProductoResponse.from(productos.editar(request.toCommand(id), usuarioActual.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productos.eliminar(id, usuarioActual.id());
        return ResponseEntity.noContent().build();
    }
}
