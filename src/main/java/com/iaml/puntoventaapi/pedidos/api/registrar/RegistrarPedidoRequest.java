package com.iaml.puntoventaapi.pedidos.api.registrar;

import com.iaml.puntoventaapi.pedidos.domain.PagoPedido.MetodoPago;
import com.iaml.puntoventaapi.pedidos.domain.ClientePedido;
import com.iaml.puntoventaapi.pedidos.domain.TextoPedido;
import com.iaml.puntoventaapi.pedidos.application.registrar.RegistrarPedidoCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Los precios y descuentos se calculan en el servidor, no se aceptan del cliente. */
public record RegistrarPedidoRequest(
        @NotNull(message = "La sucursal es obligatoria.")
        @Positive(message = "La sucursal no es válida.") Long sucursalId,
        @Size(max = 2000, message = "La descripción no debe superar 2000 caracteres.")
        @Pattern(regexp = "^[^\\p{Cc}]*$", message = "La descripción contiene caracteres no válidos.") String descripcion,
        @NotNull(message = "Los datos del cliente son obligatorios.") @Valid Cliente cliente,
        @NotEmpty(message = "Agrega al menos un producto.")
        @Size(max = 100, message = "No se admiten más de 100 productos.")
        List<@NotNull(message = "El producto es obligatorio.") @Valid Producto> productos,
        @NotBlank(message = "El tipo de entrega es obligatorio.")
        @Size(max = 50, message = "El tipo de entrega no debe superar 50 caracteres.") String tipoEntrega,
        @Size(max = 2000, message = "La descripción de envío no debe superar 2000 caracteres.")
        @Pattern(regexp = "^[^\\p{Cc}]*$", message = "La descripción de envío contiene caracteres no válidos.") String descripcionEnvio,
        @NotNull(message = "Indica la cantidad pagada y el método de pago.") @Valid PagoInicial pagoInicial) {

    public RegistrarPedidoRequest {
        descripcion = TextoPedido.normalizar(descripcion);
        descripcionEnvio = TextoPedido.normalizar(descripcionEnvio);
    }

    public record Cliente(
            @NotBlank(message = "El nombre del cliente es obligatorio.") @Size(max = 150)
            @Pattern(regexp = "^[\\p{L}\\p{M}]+(?:[ '’-][\\p{L}\\p{M}]+)*$", message = "Ingresa un nombre válido, sin números.") String nombre,
            @NotBlank(message = "El teléfono del cliente es obligatorio.")
            @Pattern(regexp = "^(?:[0-9]{10})?$", message = "El teléfono debe contener exactamente 10 dígitos.") String telefono,
            @Size(max = 120, message = "La ciudad no debe superar 120 caracteres.")
            @Pattern(regexp = "^[^\\p{Cc}]*$", message = "La ciudad contiene caracteres no válidos.") String ciudad,
            @Size(max = 120, message = "La colonia no debe superar 120 caracteres.")
            @Pattern(regexp = "^[^\\p{Cc}]*$", message = "La colonia contiene caracteres no válidos.") String colonia,
            @Size(max = 200, message = "La calle y el número no deben superar 200 caracteres.")
            @Pattern(regexp = "^[^\\p{Cc}]*$", message = "La calle contiene caracteres no válidos.") String calle) {
        public Cliente {
            nombre = TextoPedido.normalizar(nombre);
            ciudad = TextoPedido.normalizar(ciudad);
            colonia = TextoPedido.normalizar(colonia);
            calle = TextoPedido.normalizar(calle);
        }
    }

    public record Producto(
            @NotNull(message = "El producto es obligatorio.") @Positive Long productoId,
            @NotNull(message = "La cantidad es obligatoria.") @Positive(message = "La cantidad debe ser mayor a cero.") Integer cantidad) { }

    public record PagoInicial(
            @NotNull(message = "La cantidad pagada es obligatoria.")
            @DecimalMin(value = "0.00", message = "La cantidad pagada no puede ser negativa.")
            @Digits(integer = 16, fraction = 2, message = "El monto admite hasta dos decimales.") BigDecimal monto,
            @NotNull(message = "El método de pago es obligatorio.") MetodoPago metodoPago) { }

    public RegistrarPedidoCommand toCommand(long usuarioId) {
        var datosCliente = new ClientePedido(cliente.nombre(), cliente.telefono(), cliente.ciudad(),
            cliente.colonia(), cliente.calle());
        var lineas = productos.stream().map(producto ->
            new RegistrarPedidoCommand.ProductoSolicitado(producto.productoId(), producto.cantidad())).toList();
        return new RegistrarPedidoCommand(sucursalId, usuarioId, descripcion, datosCliente, lineas,
            tipoEntrega, descripcionEnvio, pagoInicial.monto(), pagoInicial.metodoPago());
    }
}
