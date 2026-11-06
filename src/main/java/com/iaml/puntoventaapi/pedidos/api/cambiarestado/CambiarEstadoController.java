package com.iaml.puntoventaapi.pedidos.api.cambiarestado;

import com.iaml.puntoventaapi.pedidos.application.cambiarestado.CambiarEstadoService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
public class CambiarEstadoController {
    private final CambiarEstadoService estados;
    private final UsuarioActual usuario;
    public CambiarEstadoController(CambiarEstadoService estados, UsuarioActual usuario) {
        this.estados = estados;
        this.usuario = usuario;
    }
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiar(@PathVariable long id, @Valid @RequestBody CambiarEstadoRequest request) {
        estados.cambiar(id, usuario.id(), request.estado());
        return ResponseEntity.noContent().build();
    }
}
