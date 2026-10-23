package com.iaml.puntoventaapi.sucursal.api;

import com.iaml.puntoventaapi.sucursal.api.dto.ConsultarSucursalesRequest;
import com.iaml.puntoventaapi.sucursal.api.dto.CrearSucursalRequest;
import com.iaml.puntoventaapi.sucursal.api.dto.EditarSucursalRequest;
import com.iaml.puntoventaapi.sucursal.api.dto.EliminarSucursalRequest;
import com.iaml.puntoventaapi.sucursal.api.dto.SucursalResponse;
import com.iaml.puntoventaapi.sucursal.application.SucursalService;
import com.iaml.puntoventaapi.sucursal.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.sucursal.domain.exception.SucursalConDependenciasException;
import com.iaml.puntoventaapi.sucursal.domain.exception.SucursalNoEncontradaException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/sucursales")
@PreAuthorize("isAuthenticated()")
public class SucursalController {

    private final SucursalService sucursalService;

    public SucursalController(SucursalService sucursalService) {
        this.sucursalService = sucursalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SucursalResponse crear(@Valid @RequestBody CrearSucursalRequest request) {
        return SucursalResponse.from(sucursalService.crear(request.toCommand()));
    }

    @GetMapping
    public List<SucursalResponse> consultar(@Valid @RequestBody ConsultarSucursalesRequest request) {
        return sucursalService.consultarPorNegocio(request.negocioId()).stream()
            .map(SucursalResponse::from).toList();
    }

    @PatchMapping
    public SucursalResponse editar(@Valid @RequestBody EditarSucursalRequest request) {
        return SucursalResponse.from(sucursalService.editar(request.toCommand()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Valid @RequestBody EliminarSucursalRequest request) {
        sucursalService.eliminar(request.id());
    }

    @ExceptionHandler({SucursalNoEncontradaException.class, NegocioNoEncontradoException.class})
    public ProblemDetail noEncontrado(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SucursalConDependenciasException.class)
    public ProblemDetail conflicto(SucursalConDependenciasException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

}

