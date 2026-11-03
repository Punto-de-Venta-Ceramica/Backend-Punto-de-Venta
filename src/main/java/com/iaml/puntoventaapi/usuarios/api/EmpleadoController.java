package com.iaml.puntoventaapi.usuarios.api;

import com.iaml.puntoventaapi.usuarios.api.dto.*;
import com.iaml.puntoventaapi.usuarios.application.EliminarEmpleadoCommand;
import com.iaml.puntoventaapi.usuarios.application.EmpleadoService;
import com.iaml.puntoventaapi.usuarios.domain.exception.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/empleados")
@PreAuthorize("hasRole('PROPIETARIO')")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpleadoCreadoResponse registrar(@Valid @RequestBody RegistrarEmpleadoRequest request) {
        return EmpleadoCreadoResponse.from(empleadoService.registrar(request.toCommand()));
    }

    @GetMapping
    public List<EmpleadoResponse> listar(
            @RequestParam(required = false) Long negocioId,
            @RequestParam(defaultValue = "false") boolean incluirAsignacion,
            @Valid @RequestBody(required = false) ConsultarEmpleadosRequest body) {
        Long id = body != null && body.negocioId() != null ? body.negocioId() : negocioId;
        if (id == null) {
            throw new IllegalArgumentException("El id del negocio es obligatorio.");
        }
        boolean conAsignacion = body != null && body.incluirAsignacion() != null
            ? body.incluirAsignacion()
            : incluirAsignacion;
        return empleadoService.listarPorNegocio(id, conAsignacion).stream()
            .map(EmpleadoResponse::from)
            .toList();
    }

    // Endpoint POST alternativo compatible con Flutter Web y navegadores
    @PostMapping("/consultar")
    public List<EmpleadoResponse> consultarDesdeWeb(@Valid @RequestBody ConsultarEmpleadosRequest request) {
        boolean conAsignacion = Boolean.TRUE.equals(request.incluirAsignacion());
        return empleadoService.listarPorNegocio(request.negocioId(), conAsignacion).stream()
            .map(EmpleadoResponse::from)
            .toList();
    }

    @PatchMapping("/asignar-sucursal")
    public EmpleadoResponse asignarSucursal(@Valid @RequestBody AsignarSucursalEmpleadoRequest request) {
        return EmpleadoResponse.from(empleadoService.asignarSucursal(request.toCommand()));
    }

    @PatchMapping
    public EmpleadoResponse editar(@Valid @RequestBody EditarEmpleadoRequest request) {
        return EmpleadoResponse.from(empleadoService.editar(request.toCommand()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Valid @RequestBody EliminarEmpleadoRequest request) {
        empleadoService.eliminar(request.toCommand());
    }

    @DeleteMapping("/{empleadoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPorPath(@PathVariable Long empleadoId, @RequestParam Long negocioId) {
        empleadoService.eliminar(new EliminarEmpleadoCommand(negocioId, empleadoId));
    }

    @ExceptionHandler(NegocioNoEncontradoException.class)
    public ProblemDetail negocioNoEncontrado(NegocioNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SucursalNoEncontradaException.class)
    public ProblemDetail sucursalNoEncontrada(SucursalNoEncontradaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmpleadoNoEncontradoException.class)
    public ProblemDetail empleadoNoEncontrado(EmpleadoNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ProblemDetail correoYaRegistrado(CorreoYaRegistradoException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("errores", Map.of("correoElectronico", ex.getMessage()));
        return problem;
    }

    @ExceptionHandler(EmpleadoYaTieneSucursalException.class)
    public ProblemDetail empleadoYaTieneSucursal(EmpleadoYaTieneSucursalException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EmpleadoConDependenciasException.class)
    public ProblemDetail empleadoConDependencias(EmpleadoConDependenciasException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
