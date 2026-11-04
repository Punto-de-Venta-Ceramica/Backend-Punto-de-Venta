package com.iaml.puntoventaapi.catalogo.api;

import com.iaml.puntoventaapi.catalogo.application.OpcionesRegistroProductoService;
import com.iaml.puntoventaapi.catalogo.domain.port.CategoriasParaRegistroPort;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos/opciones")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
public class OpcionesRegistroProductoController {
    private final OpcionesRegistroProductoService opciones;
    private final UsuarioActual usuario;
    public OpcionesRegistroProductoController(OpcionesRegistroProductoService opciones, UsuarioActual usuario) {
        this.opciones = opciones;
        this.usuario = usuario;
    }

    @GetMapping("/categorias")
    public List<CategoriasParaRegistroPort.Categoria> categorias(@RequestParam long sucursalId) {
        return opciones.categorias(sucursalId, usuario.id());
    }
}
