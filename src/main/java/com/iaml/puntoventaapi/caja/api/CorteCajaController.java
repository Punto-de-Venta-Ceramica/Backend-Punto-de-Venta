package com.iaml.puntoventaapi.caja.api;

import com.iaml.puntoventaapi.caja.api.dto.*;
import com.iaml.puntoventaapi.caja.application.CorteCajaService;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cortes-caja")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
public class CorteCajaController {

    private final CorteCajaService corteCajaService;
    private final UsuarioActual usuarioActual;

    public CorteCajaController(CorteCajaService corteCajaService, UsuarioActual usuarioActual) {
        this.corteCajaService = corteCajaService;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping("/abrir")
    public ResponseEntity<CorteCajaResponse> abrirCorte(@Valid @RequestBody AbrirCorteRequest request) {
        CorteCajaResponse respuesta = corteCajaService.abrirCorte(request, usuarioActual.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/activo")
    public ResponseEntity<CorteCajaResponse> obtenerCorteActivo(@RequestParam(name = "sucursalId", required = false) Long sucursalId) {
        CorteCajaResponse respuesta = corteCajaService.obtenerCorteActivo(sucursalId, usuarioActual.id());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<CorteCajaResponse> cerrarCorte(
        @PathVariable("id") Long id,
        @Valid @RequestBody CerrarCorteRequest request
    ) {
        CorteCajaResponse respuesta = corteCajaService.cerrarCorte(id, request, usuarioActual.id());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CorteCajaResponse> consultarCorte(@PathVariable("id") Long id) {
        CorteCajaResponse respuesta = corteCajaService.consultarCorte(id, usuarioActual.id());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping
    public ResponseEntity<Page<ResumenCorteCajaDto>> listarHistorial(
        @RequestParam(name = "sucursalId", required = false) Long sucursalId,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaHoraApertura"));
        Page<ResumenCorteCajaDto> historial = corteCajaService.listarHistorial(sucursalId, usuarioActual.id(), pageable);
        return ResponseEntity.ok(historial);
    }
}
