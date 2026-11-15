package com.iaml.puntoventaapi.ventas.api;

import com.iaml.puntoventaapi.shared.security.UsuarioNoAutenticadoException;
import com.iaml.puntoventaapi.ventas.domain.exception.ConflictoConfirmacionVentaException;
import com.iaml.puntoventaapi.ventas.domain.exception.DatosVentaInvalidosException;
import com.iaml.puntoventaapi.ventas.domain.exception.ExistenciaVentaInsuficienteException;
import com.iaml.puntoventaapi.ventas.domain.exception.RecursoVentaNoEncontradoException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {VentaController.class, TicketController.class})
public class VentaExceptionHandler {

    @ExceptionHandler(DatosVentaInvalidosException.class)
    public ProblemDetail datosInvalidos(DatosVentaInvalidosException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("errores", ex.getErrores());
        return problem;
    }

    @ExceptionHandler(RecursoVentaNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoVentaNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoConfirmacionVentaException.class)
    public ProblemDetail confirmacionEnConflicto(ConflictoConfirmacionVentaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ExistenciaVentaInsuficienteException.class)
    public ProblemDetail existenciaInsuficiente(ExistenciaVentaInsuficienteException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("productoId", ex.getProductoId());
        problem.setProperty("cantidadSolicitada", ex.getCantidadSolicitada());
        problem.setProperty("existenciaDisponible", ex.getExistenciaDisponible());
        return problem;
    }

    @ExceptionHandler(UsuarioNoAutenticadoException.class)
    public ProblemDetail noAutenticado(UsuarioNoAutenticadoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
