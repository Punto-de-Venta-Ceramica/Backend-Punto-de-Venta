package com.iaml.puntoventaapi.catalogo.api;

import com.iaml.puntoventaapi.catalogo.api.dto.response.AreaResponse;
import com.iaml.puntoventaapi.catalogo.api.dto.request.GuardarAreaRequest;
import com.iaml.puntoventaapi.catalogo.application.AreaService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/negocios/{negocioId}/areas")
public class AreaController {

    private final AreaService areas;
    private final UsuarioActual usuarioActual;

    public AreaController(AreaService areas, UsuarioActual usuarioActual) {
        this.areas = areas;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping
    public ResponseEntity<AreaResponse> crear(@PathVariable @Positive Long negocioId,
                                            @Valid @RequestBody GuardarAreaRequest request) {
        AreaResponse area = AreaResponse.from(areas.crear(negocioId, request.toCommand(), usuarioActual.id()));
        return ResponseEntity.created(URI.create("/api/negocios/" + negocioId + "/areas/" + area.id()))
            .body(area);
    }

    @GetMapping
    public List<AreaResponse> listar(@PathVariable @Positive Long negocioId) {
        return areas.listar(negocioId, usuarioActual.id()).stream().map(AreaResponse::from).toList();
    }

    @GetMapping("/{areaId}")
    public AreaResponse consultar(@PathVariable @Positive Long negocioId,
                                 @PathVariable @Positive Long areaId) {
        return AreaResponse.from(areas.consultar(negocioId, areaId, usuarioActual.id()));
    }

    @PutMapping("/{areaId}")
    public AreaResponse editar(@PathVariable @Positive Long negocioId,
                              @PathVariable @Positive Long areaId,
                              @Valid @RequestBody GuardarAreaRequest request) {
        return AreaResponse.from(areas.editar(negocioId, areaId, request.toCommand(), usuarioActual.id()));
    }

    @DeleteMapping("/{areaId}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long negocioId,
                                         @PathVariable @Positive Long areaId) {
        areas.eliminar(negocioId, areaId, usuarioActual.id());
        return ResponseEntity.noContent().build();
    }
}
