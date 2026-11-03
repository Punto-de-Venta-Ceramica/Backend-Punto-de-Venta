package com.iaml.puntoventaapi.inventario.api;

import com.iaml.puntoventaapi.inventario.domain.exception.ExistenciaInsuficienteException;
import com.iaml.puntoventaapi.inventario.domain.exception.InventarioNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.shared.security.UsuarioNoAutenticadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = InventarioController.class)
public class InventarioExceptionHandler {

    @ExceptionHandler({InventarioNoEncontradoException.class, NegocioNoEncontradoException.class})
    public ProblemDetail noEncontrado(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ExistenciaInsuficienteException.class)
    public ProblemDetail existenciaInsuficiente(ExistenciaInsuficienteException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UsuarioNoAutenticadoException.class)
    public ProblemDetail usuarioNoAutenticado(UsuarioNoAutenticadoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
