package com.iaml.puntoventaapi.negocios.api;

import com.iaml.puntoventaapi.negocios.api.dto.CrearNegocioRequest;
import com.iaml.puntoventaapi.negocios.api.dto.EditarNegocioRequest;
import com.iaml.puntoventaapi.negocios.api.dto.NegocioResponse;
import com.iaml.puntoventaapi.negocios.api.dto.NegocioResumenResponse;
import com.iaml.puntoventaapi.negocios.application.NegocioService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/negocios")
public class NegocioController {

    private final NegocioService negocios;
    private final UsuarioActual usuarioActual;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NegocioResponse> crear(@Valid @ModelAttribute CrearNegocioRequest request) {
        NegocioResponse negocio = NegocioResponse.from(negocios.crear(request, usuarioActual.id()));
        return ResponseEntity.created(URI.create("/api/negocios/" + negocio.getId())).body(negocio);
    }

    @GetMapping
    public List<NegocioResumenResponse> listar() {
        return negocios.listar(usuarioActual.id()).stream().map(NegocioResumenResponse::from).toList();
    }

    @GetMapping("/{id}")
    public NegocioResponse consultar(@PathVariable Long id) {
        return NegocioResponse.from(negocios.consultar(id, usuarioActual.id()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public NegocioResponse editar(@PathVariable Long id, @Valid @ModelAttribute EditarNegocioRequest request) {
        return NegocioResponse.from(negocios.editar(id, request, usuarioActual.id()));
    }
}
