package com.iaml.puntoventaapi.catalogo.api;

import com.iaml.puntoventaapi.catalogo.domain.exception.CategoriaNoEncontradaException;
import com.iaml.puntoventaapi.catalogo.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoNoEncontradoException;
import com.iaml.puntoventaapi.shared.security.UsuarioNoAutenticadoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DatosCatalogoInvalidosException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ClasificacionConProductosException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {ProductoController.class, AreaController.class, CategoriaController.class})
public class CatalogoExceptionHandler {

    @ExceptionHandler(ClasificacionConProductosException.class)
    public ProblemDetail clasificacionConProductos(ClasificacionConProductosException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({ProductoNoEncontradoException.class, CategoriaNoEncontradaException.class,
        NegocioNoEncontradoException.class, CatalogoNoEncontradoException.class})
    public ProblemDetail noEncontrado(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProductoInvalidoException.class)
    public ProblemDetail productoInvalido(ProductoInvalidoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DatosCatalogoInvalidosException.class)
    public ProblemDetail datosInvalidos(DatosCatalogoInvalidosException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("errores", ex.getErrores());
        return problem;
    }

    @ExceptionHandler(UsuarioNoAutenticadoException.class)
    public ProblemDetail usuarioNoAutenticado(UsuarioNoAutenticadoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
