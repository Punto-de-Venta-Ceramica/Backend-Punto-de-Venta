package com.iaml.puntoventaapi.caja.api;

import com.iaml.puntoventaapi.caja.domain.exception.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CorteCajaController.class)
public class CorteCajaExceptionHandler {

    @ExceptionHandler(CorteCajaNoEncontradoException.class)
    public ProblemDetail noEncontrado(CorteCajaNoEncontradoException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setProperty("codigo", "CORTE_CAJA_NO_ENCONTRADO");
        return problem;
    }

    @ExceptionHandler(CorteCajaNoAbiertoException.class)
    public ProblemDetail noAbierto(CorteCajaNoAbiertoException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setProperty("codigo", "CORTE_CAJA_NO_ABIERTO");
        return problem;
    }

    @ExceptionHandler(CorteCajaYaAbiertoException.class)
    public ProblemDetail yaAbierto(CorteCajaYaAbiertoException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("codigo", "CORTE_CAJA_YA_ABIERTO");
        return problem;
    }

    @ExceptionHandler(CorteCajaYaCerradoException.class)
    public ProblemDetail yaCerrado(CorteCajaYaCerradoException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("codigo", "CORTE_CAJA_YA_CERRADO");
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegado(AccessDeniedException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setProperty("codigo", "ACCESO_DENEGADO");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail solicitudInvalida(IllegalArgumentException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("codigo", "SOLICITUD_INVALIDA");
        return problem;
    }
}

