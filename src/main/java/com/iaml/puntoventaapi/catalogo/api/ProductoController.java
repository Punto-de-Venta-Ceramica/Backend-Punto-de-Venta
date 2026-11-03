package com.iaml.puntoventaapi.catalogo.api;

import com.iaml.puntoventaapi.catalogo.api.dto.CrearProductoRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.EditarProductoRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.ProductoResponse;
import com.iaml.puntoventaapi.catalogo.application.ProductoService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/productos")
@PreAuthorize("hasRole('PROPIETARIO')")
public class ProductoController {

    private final ProductoService productos;
    private final UsuarioActual usuarioActual;

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody CrearProductoRequest request) {
        ProductoResponse producto = ProductoResponse.from(productos.crear(request.toCommand(), usuarioActual.id()));
        return ResponseEntity.created(URI.create("/api/productos/" + producto.id())).body(producto);
    }

    @GetMapping
    public List<ProductoResponse> listar(@RequestParam Long negocioId) {
        return productos.listar(negocioId, usuarioActual.id()).stream().map(ProductoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductoResponse consultar(@PathVariable Long id) {
        return ProductoResponse.from(productos.consultar(id, usuarioActual.id()));
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
