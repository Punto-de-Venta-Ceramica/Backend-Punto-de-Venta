package com.iaml.puntoventaapi.shared.api;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ManejadorGlobalExcepciones extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = new LinkedHashMap<String, String>();
        ex.getBindingResult().getFieldErrors().stream()
            .sorted(java.util.Comparator.comparingInt(error ->
                java.util.Set.of("NotBlank", "NotNull", "NotEmpty").contains(error.getCode()) ? 0 : 1))
            .forEach(error -> errors.putIfAbsent(error.getField(),
                error.getDefaultMessage() == null ? "El campo no es válido." : error.getDefaultMessage()));
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los datos del formulario.");
        problem.setProperty("errores", errors);
        return new ResponseEntity<>(problem, headers, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = ex instanceof HttpMessageNotReadableException
            ? "Envía un body JSON válido, con los campos y tipos de datos esperados."
            : switch (status.value()) {
                case 400 -> "La solicitud no es válida. Revisa los datos enviados.";
                case 404 -> "El recurso solicitado no existe.";
                case 405 -> "El método HTTP no está permitido para este recurso.";
                case 406 -> "El formato de respuesta solicitado no está disponible.";
                case 415 -> "El tipo de contenido no está admitido. Para enviar JSON usa Content-Type: application/json.";
                default -> status.is5xxServerError()
                    ? "No se pudo completar la solicitud. Inténtalo más tarde."
                    : "No se pudo procesar la solicitud.";
            };
        // Nunca se reutiliza el mensaje técnico ni el body generado por la excepción.
        return super.handleExceptionInternal(ex, ProblemDetail.forStatusAndDetail(status, detail),
            headers, status, request);
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ProblemDetail datosInvalidos(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflicto(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "No se pudo completar la operación porque existen datos relacionados o duplicados.");
    }

    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ProblemDetail datosNoDisponibles(Exception ex) {
        LOG.error("No se pudo completar el acceso a datos.", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
            "El servicio no está disponible temporalmente. Inténtalo más tarde.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail autenticacion(AuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
            "Necesitas una sesión válida para realizar esta operación.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegado(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
            "No tienes permiso para realizar esta operación.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception ex) {
        LOG.error("Error inesperado al procesar una solicitud.", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocurrió un error inesperado. Inténtalo más tarde.");
    }
}
