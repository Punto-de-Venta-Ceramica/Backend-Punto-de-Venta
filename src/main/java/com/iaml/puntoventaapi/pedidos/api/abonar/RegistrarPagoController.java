package com.iaml.puntoventaapi.pedidos.api.abonar;

import com.iaml.puntoventaapi.pedidos.application.abonar.RegistrarPagoService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
public class RegistrarPagoController {
    private final RegistrarPagoService pagos;
    private final UsuarioActual usuario;
    public RegistrarPagoController(RegistrarPagoService pagos, UsuarioActual usuario) {
        this.pagos = pagos;
        this.usuario = usuario;
    }
    @PostMapping("/{id}/pagos")
    public RegistrarPagoService.Resultado registrar(@PathVariable long id, @Valid @RequestBody RegistrarPagoRequest request) {
        return pagos.registrar(id, usuario.id(), request.toDomain());
    }
}
