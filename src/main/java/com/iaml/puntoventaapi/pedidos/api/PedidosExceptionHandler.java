package com.iaml.puntoventaapi.pedidos.api;

import com.iaml.puntoventaapi.pedidos.api.abonar.RegistrarPagoController;
import com.iaml.puntoventaapi.pedidos.api.cambiarestado.CambiarEstadoController;
import com.iaml.puntoventaapi.pedidos.api.cancelar.CancelarPedidoController;
import com.iaml.puntoventaapi.pedidos.api.consultar.ConsultarPedidosController;
import com.iaml.puntoventaapi.pedidos.api.registrar.RegistrarPedidoController;
import com.iaml.puntoventaapi.pedidos.infrastructure.persistence.PedidoNoEncontradoException;
import com.iaml.puntoventaapi.pedidos.domain.CorteCajaInvalidoException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {RegistrarPedidoController.class, ConsultarPedidosController.class,
    RegistrarPagoController.class, CambiarEstadoController.class, CancelarPedidoController.class})
public class PedidosExceptionHandler {
    @ExceptionHandler(CorteCajaInvalidoException.class)
    public ProblemDetail corteInvalido(CorteCajaInvalidoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    @ExceptionHandler(PedidoNoEncontradoException.class)
    public ProblemDetail noEncontrado(PedidoNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ProblemDetail prohibido(SecurityException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail conflicto(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "No se puede completar la operación en el estado actual.");
    }
}
