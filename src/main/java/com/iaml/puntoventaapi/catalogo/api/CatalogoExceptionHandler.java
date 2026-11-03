package com.iaml.puntoventaapi.catalogo.api;

import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DatosCatalogoInvalidosException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {AreaController.class, CategoriaController.class})
public class CatalogoExceptionHandler {

    @ExceptionHandler(CatalogoNoEncontradoException.class)
    public ProblemDetail noEncontrado(CatalogoNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DatosCatalogoInvalidosException.class)
    public ProblemDetail datosInvalidos(DatosCatalogoInvalidosException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("errores", ex.getErrores());
        return problem;
    }
}
