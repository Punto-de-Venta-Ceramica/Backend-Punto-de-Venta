package com.iaml.puntoventaapi.catalogo.api;

import com.iaml.puntoventaapi.catalogo.api.dto.CategoriaResponse;
import com.iaml.puntoventaapi.catalogo.api.dto.GuardarCategoriaRequest;
import com.iaml.puntoventaapi.catalogo.application.CategoriaService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/negocios/{negocioId}/areas/{areaId}/categorias")
public class CategoriaController {

    private final CategoriaService categorias;
    private final UsuarioActual usuarioActual;

    public CategoriaController(CategoriaService categorias, UsuarioActual usuarioActual) {
        this.categorias = categorias;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@PathVariable @Positive Long negocioId,
                                                 @PathVariable @Positive Long areaId,
                                                 @Valid @RequestBody GuardarCategoriaRequest request) {
        CategoriaResponse categoria = CategoriaResponse.from(
            categorias.crear(negocioId, areaId, request.toCommand(), usuarioActual.id()));
        URI ubicacion = URI.create("/api/negocios/" + negocioId + "/areas/" + areaId
            + "/categorias/" + categoria.id());
        return ResponseEntity.created(ubicacion).body(categoria);
    }

    @GetMapping
    public List<CategoriaResponse> listar(@PathVariable @Positive Long negocioId,
                                        @PathVariable @Positive Long areaId) {
        return categorias.listar(negocioId, areaId, usuarioActual.id()).stream()
            .map(CategoriaResponse::from).toList();
    }

    @GetMapping("/{categoriaId}")
    public CategoriaResponse consultar(@PathVariable @Positive Long negocioId,
                                       @PathVariable @Positive Long areaId,
                                       @PathVariable @Positive Long categoriaId) {
        return CategoriaResponse.from(categorias.consultar(negocioId, areaId, categoriaId, usuarioActual.id()));
    }

    @PutMapping("/{categoriaId}")
    public CategoriaResponse editar(@PathVariable @Positive Long negocioId,
                                    @PathVariable @Positive Long areaId,
                                    @PathVariable @Positive Long categoriaId,
                                    @Valid @RequestBody GuardarCategoriaRequest request) {
        return CategoriaResponse.from(
            categorias.editar(negocioId, areaId, categoriaId, request.toCommand(), usuarioActual.id()));
    }

    @DeleteMapping("/{categoriaId}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long negocioId,
                                         @PathVariable @Positive Long areaId,
                                         @PathVariable @Positive Long categoriaId) {
        categorias.eliminar(negocioId, areaId, categoriaId, usuarioActual.id());
        return ResponseEntity.noContent().build();
    }
}
