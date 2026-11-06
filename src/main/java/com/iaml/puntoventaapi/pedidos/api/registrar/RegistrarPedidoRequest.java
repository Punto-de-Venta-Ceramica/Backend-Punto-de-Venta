package com.iaml.puntoventaapi.pedidos.api.registrar;

import com.iaml.puntoventaapi.pedidos.domain.PagoPedido.MetodoPago;
import com.iaml.puntoventaapi.pedidos.domain.ClientePedido;
import com.iaml.puntoventaapi.pedidos.application.registrar.RegistrarPedidoCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Los precios y descuentos se calculan en el servidor, no se aceptan del cliente. */
public record RegistrarPedidoRequest(
        @NotNull(message = "La sucursal es obligatoria.")
        @Positive(message = "La sucursal no es válida.") Long sucursalId,
        @Size(max = 2000, message = "La descripción no debe superar 2000 caracteres.") String descripcion,
        @NotNull(message = "Los datos del cliente son obligatorios.") @Valid Cliente cliente,
        @NotEmpty(message = "Agrega al menos un producto.")
        @Size(max = 100, message = "No se admiten más de 100 productos.")
        List<@NotNull(message = "El producto es obligatorio.") @Valid Producto> productos,
        @NotBlank(message = "El tipo de entrega es obligatorio.")
        @Size(max = 50, message = "El tipo de entrega no debe superar 50 caracteres.") String tipoEntrega,
        @Size(max = 2000, message = "La descripción de envío no debe superar 2000 caracteres.") String descripcionEnvio,
        @NotNull(message = "Indica la cantidad pagada y el método de pago.") @Valid PagoInicial pagoInicial) {

    public record Cliente(
            @NotBlank(message = "El nombre del cliente es obligatorio.") @Size(max = 150) String nombre,
            @NotBlank(message = "El teléfono del cliente es obligatorio.") @Size(max = 30) String telefono,
            @NotBlank(message = "La ciudad es obligatoria.") @Size(max = 120) String ciudad,
            @NotBlank(message = "La colonia es obligatoria.") @Size(max = 120) String colonia,
            @NotBlank(message = "La calle y el número son obligatorios.") @Size(max = 200) String calle) { }

    public record Producto(
            @NotNull(message = "El producto es obligatorio.") @Positive Long productoId,
            @NotNull(message = "La cantidad es obligatoria.") @Positive(message = "La cantidad debe ser mayor a cero.") Integer cantidad) { }

    public record PagoInicial(
            @NotNull(message = "La cantidad pagada es obligatoria.")
            @DecimalMin(value = "0.00", message = "La cantidad pagada no puede ser negativa.")
            @Digits(integer = 16, fraction = 2, message = "El monto admite hasta dos decimales.") BigDecimal monto,
            @NotNull(message = "El método de pago es obligatorio.") MetodoPago metodoPago,
            @Positive(message = "El corte de caja no es válido.") Long corteCajaId) { }

    public RegistrarPedidoCommand toCommand(long usuarioId) {
        var datosCliente = new ClientePedido(cliente.nombre(), cliente.telefono(), cliente.ciudad(),
            cliente.colonia(), cliente.calle());
        var lineas = productos.stream().map(producto ->
            new RegistrarPedidoCommand.ProductoSolicitado(producto.productoId(), producto.cantidad())).toList();
        return new RegistrarPedidoCommand(sucursalId, usuarioId, descripcion, datosCliente, lineas,
            tipoEntrega, descripcionEnvio, pagoInicial.monto(), pagoInicial.metodoPago(), pagoInicial.corteCajaId());
    }
}
