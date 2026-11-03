package com.iaml.puntoventaapi.catalogo.api;

import com.iaml.puntoventaapi.catalogo.domain.exception.CategoriaNoEncontradaException;
import com.iaml.puntoventaapi.catalogo.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoNoEncontradoException;
import com.iaml.puntoventaapi.shared.security.UsuarioNoAutenticadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ProductoController.class)
public class CatalogoExceptionHandler {

    @ExceptionHandler({ProductoNoEncontradoException.class, CategoriaNoEncontradaException.class,
        NegocioNoEncontradoException.class})
    public ProblemDetail noEncontrado(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProductoInvalidoException.class)
    public ProblemDetail productoInvalido(ProductoInvalidoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UsuarioNoAutenticadoException.class)
    public ProblemDetail usuarioNoAutenticado(UsuarioNoAutenticadoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
