package com.iaml.puntoventaapi.catalogo.api;

import com.iaml.puntoventaapi.catalogo.api.dto.response.CategoriaResponse;
import com.iaml.puntoventaapi.catalogo.application.CategoriaService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Categorías de todo el negocio en una sola petición (filtros y selectores). La gestión de cada categoría
 * sigue en {@link CategoriaController}, bajo su área.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/negocios/{negocioId}/categorias")
public class CategoriasNegocioController {

    private final CategoriaService categorias;
    private final UsuarioActual usuarioActual;

    /** Con sucursalId, un empleado asignado a ella también puede consultar. */
    @GetMapping
    public List<CategoriaResponse> listar(@PathVariable @Positive Long negocioId,
                                          @RequestParam(required = false) Long sucursalId) {
        return categorias.listarPorNegocio(negocioId, usuarioActual.id(), sucursalId).stream()
            .map(CategoriaResponse::from).toList();
    }
}
