package com.iaml.puntoventaapi.negocios.api;

import com.iaml.puntoventaapi.negocios.domain.exception.LogoInvalidoException;
import com.iaml.puntoventaapi.negocios.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.shared.security.UsuarioNoAutenticadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.OptimisticLockingFailureException;
import jakarta.persistence.OptimisticLockException;

@RestControllerAdvice(assignableTypes = NegocioController.class)
public class NegocioExceptionHandler {

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ProblemDetail modificacionConcurrente(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "El negocio cambió mientras se procesaba la solicitud. Actualiza la información e inténtalo de nuevo.");
    }

    @ExceptionHandler(NegocioNoEncontradoException.class)
    public ProblemDetail negocioNoEncontrado(NegocioNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(LogoInvalidoException.class)
    public ProblemDetail logoInvalido(LogoInvalidoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UsuarioNoAutenticadoException.class)
    public ProblemDetail usuarioNoAutenticado(UsuarioNoAutenticadoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
