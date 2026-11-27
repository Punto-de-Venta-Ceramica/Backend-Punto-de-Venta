package com.iaml.puntoventaapi.pedidos.domain;

import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iaml.puntoventaapi.pedidos.application.abonar.RegistrarPagoService;
import com.iaml.puntoventaapi.pedidos.application.cambiarestado.CambiarEstadoService;
import com.iaml.puntoventaapi.pedidos.application.consultar.ConsultarPedidosService;
import com.iaml.puntoventaapi.pedidos.application.editar.EditarDatosPedidoService;
import com.iaml.puntoventaapi.pedidos.application.registrar.RegistrarPedidoCommand;
import com.iaml.puntoventaapi.pedidos.application.registrar.RegistrarPedidoCommand.ProductoSolicitado;
import com.iaml.puntoventaapi.pedidos.application.registrar.RegistrarPedidoService;
import com.iaml.puntoventaapi.pedidos.domain.PagoPedido.MetodoPago;
import com.iaml.puntoventaapi.pedidos.domain.exception.TransicionEstadoInvalidaException;
import com.iaml.puntoventaapi.pedidos.domain.port.CatalogoPort;
import com.iaml.puntoventaapi.pedidos.domain.port.ClientePort;
import com.iaml.puntoventaapi.pedidos.domain.port.ConsultaPedidosPort;
import com.iaml.puntoventaapi.pedidos.domain.port.NotificacionPort;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort;
import com.iaml.puntoventaapi.pedidos.domain.port.PedidoRepositorioPort.DatosEdicion;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Cada regla de los datos de un pedido, también cuando el servicio se llama sin pasar por la API. */
class PedidoValidacionesTest {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final String LARGO = "x".repeat(2001);
    private static final Instant FUTURO = Instant.parse("2099-12-31T23:59:59Z");
    private static final ClientePedido CLIENTE = new ClientePedido("Ana", "5512345678", null, null, null);

    @Test
    void validaElCliente() {
        assertInvalidos(
            () -> new ClientePedido(" ", "5512345678", null, null, null),
            () -> new ClientePedido("a".repeat(151), "5512345678", null, null, null),
            () -> new ClientePedido("An\u0007a", "5512345678", null, null, null),
            () -> new ClientePedido("Ana", null, null, null, null),
            () -> new ClientePedido("Ana", "5512345678", "Pue\u0007bla", null, null),
            () -> new ClientePedido("Ana", "5512345678", "p".repeat(121), null, null));
        assertThat(new ClientePedido("Ana", "5512345678", "  ", null, null).ciudad()).isNull();
    }

    @Test
    void validaLineasEscalonesYPagos() {
        assertInvalidos(
            () -> new DetallePedido(0, 1, CIEN, BigDecimal.ZERO),
            () -> new DetallePedido(1, 0, CIEN, BigDecimal.ZERO),
            () -> new DetallePedido(1, 1, new BigDecimal("-1"), BigDecimal.ZERO),
            () -> new DetallePedido(1, 1, new BigDecimal("1.001"), BigDecimal.ZERO),
            () -> new DetallePedido(1, 1, CIEN, new BigDecimal("1.001")),
            () -> new DetallePedido(1, 1, CIEN, new BigDecimal("-1")),
            () -> new DetallePedido(1, 1, CIEN, new BigDecimal("101")),
            () -> new EscalonDescuento(0, null, BigDecimal.TEN),
            () -> new EscalonDescuento(5, 4, BigDecimal.TEN),
            () -> new EscalonDescuento(1, null, new BigDecimal("-1")),
            () -> new EscalonDescuento(1, null, new BigDecimal("101")),
            () -> new EscalonDescuento(1, null, new BigDecimal("1.001")),
            () -> new PagoPedido(BigDecimal.ZERO, MetodoPago.EFECTIVO),
            () -> new PagoPedido(new BigDecimal("1.001"), MetodoPago.EFECTIVO),
            () -> new Pedido(Arrays.asList(new DetallePedido(1, 1, CIEN, BigDecimal.ZERO), null)));
        assertThat(DetallePedido.desdeCatalogo(1, 2, CIEN, null).porcentajeDescuento()).isZero();
        assertThat(TextoPedido.contieneControl(null)).isFalse();
    }

    @Test
    void noSeRetrocedeDeEstado() {
        var pedido = Pedido.restaurar(List.of(new DetallePedido(1, 1, CIEN, BigDecimal.ZERO)), List.of(),
            EstadoPedido.TERMINADO);
        assertThatThrownBy(() -> pedido.cambiarEstado(EstadoPedido.ENTREGADO, true))
            .isInstanceOf(TransicionEstadoInvalidaException.class);
        var entregado = Pedido.restaurar(List.of(new DetallePedido(1, 1, CIEN, BigDecimal.ZERO)), List.of(),
            EstadoPedido.ENTREGADO);
        assertThatThrownBy(() -> entregado.cambiarEstado(EstadoPedido.PRODUCCION, true))
            .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @ParameterizedTest
    @EnumSource(EstadoPedido.class)
    void elMensajeDiceElEstadoActual(EstadoPedido estado) {
        var error = new TransicionEstadoInvalidaException(estado);
        assertThat(error.getEstadoActual()).isEqualTo(estado);
        assertThat(error.getMessage()).isNotBlank();
    }

    @Test
    void losServiciosRechazanIdentificadoresInvalidos() {
        var pedidos = mock(PedidoRepositorioPort.class);
        var pago = new PagoPedido(BigDecimal.ONE, MetodoPago.EFECTIVO);
        var pagos = new RegistrarPagoService(pedidos);
        var estados = new CambiarEstadoService(pedidos, mock(NotificacionPort.class));
        var consultas = new ConsultarPedidosService(mock(ConsultaPedidosPort.class));
        var edicion = new EditarDatosPedidoService(pedidos, mock(ClientePort.class));
        var datos = new DatosEdicion(null, "LOCAL", null);
        assertInvalidos(
            () -> pagos.registrar(0, 1, pago), () -> pagos.registrar(1, 0, pago),
            () -> estados.cambiar(0, 1, EstadoPedido.TERMINADO), () -> estados.cambiar(1, 0, EstadoPedido.TERMINADO),
            () -> consultas.consultar(0, 1), () -> consultas.consultar(1, 0),
            () -> consultas.listar(0, 1, null, null, 0), () -> consultas.listar(1, 0, null, null, 0),
            () -> edicion.editar(0, 1, CLIENTE, datos), () -> edicion.editar(1, 0, CLIENTE, datos),
            () -> edicion.editar(1, 1, CLIENTE, new DatosEdicion(null, null, null)),
            () -> edicion.editar(1, 1, CLIENTE, new DatosEdicion(null, " ", null)),
            () -> edicion.editar(1, 1, CLIENTE, new DatosEdicion(null, "x".repeat(51), null)),
            () -> edicion.editar(1, 1, CLIENTE, new DatosEdicion(LARGO, "LOCAL", null)),
            () -> edicion.editar(1, 1, CLIENTE, new DatosEdicion(null, "LOCAL", LARGO)));
    }

    private static RegistrarPedidoCommand comando(long sucursal, long usuario, String descripcion, ClientePedido cliente,
                                                  List<ProductoSolicitado> productos, String entrega, String envio,
                                                  BigDecimal monto, MetodoPago metodo) {
        return new RegistrarPedidoCommand(sucursal, usuario, descripcion, cliente, productos, entrega, envio, monto, metodo,
            FUTURO);
    }

    static Stream<RegistrarPedidoCommand> registrosInvalidos() {
        var uno = List.of(new ProductoSolicitado(1, 1));
        var conNulo = new ArrayList<ProductoSolicitado>(uno);
        conNulo.add(null);
        var ef = MetodoPago.EFECTIVO;
        return Stream.of(
            comando(0, 1, null, CLIENTE, uno, "LOCAL", null, CIEN, ef),
            comando(1, 0, null, CLIENTE, uno, "LOCAL", null, CIEN, ef),
            comando(1, 1, null, CLIENTE, uno, null, null, CIEN, ef),
            comando(1, 1, null, CLIENTE, uno, " ", null, CIEN, ef),
            comando(1, 1, null, CLIENTE, uno, "x".repeat(51), null, CIEN, ef),
            comando(1, 1, LARGO, CLIENTE, uno, "LOCAL", null, CIEN, ef),
            comando(1, 1, null, CLIENTE, uno, "LOCAL", LARGO, CIEN, ef),
            comando(1, 1, "a\u0007b", CLIENTE, uno, "LOCAL", null, CIEN, ef),
            comando(1, 1, null, CLIENTE, uno, "LOCAL", "a\u0007b", CIEN, ef),
            comando(1, 1, null, null, uno, "LOCAL", null, CIEN, ef),
            comando(1, 1, null, CLIENTE, null, "LOCAL", null, CIEN, ef),
            comando(1, 1, null, CLIENTE, List.of(), "LOCAL", null, CIEN, ef),
            comando(1, 1, null, CLIENTE, Collections.nCopies(101, uno.get(0)), "LOCAL", null, CIEN, ef),
            comando(1, 1, null, CLIENTE, uno, "LOCAL", null, null, ef),
            comando(1, 1, null, CLIENTE, uno, "LOCAL", null, new BigDecimal("-1"), ef),
            comando(1, 1, null, CLIENTE, uno, "LOCAL", null, new BigDecimal("1.001"), ef),
            comando(1, 1, null, CLIENTE, uno, "LOCAL", null, CIEN, null),
            comando(1, 1, null, CLIENTE, conNulo, "LOCAL", null, BigDecimal.ZERO, ef),
            comando(1, 1, null, CLIENTE, List.of(new ProductoSolicitado(0, 1)), "LOCAL", null, BigDecimal.ZERO, ef),
            comando(1, 1, null, CLIENTE, List.of(new ProductoSolicitado(1, 0)), "LOCAL", null, BigDecimal.ZERO, ef),
            new RegistrarPedidoCommand(1, 1, null, CLIENTE, uno, "LOCAL", null, CIEN, ef, null),
            new RegistrarPedidoCommand(1, 1, null, CLIENTE, uno, "LOCAL", null, CIEN, ef,
                Instant.now().minusSeconds(60)));
    }

    @ParameterizedTest
    @MethodSource("registrosInvalidos")
    void rechazaRegistrosInvalidos(RegistrarPedidoCommand comando) {
        var catalogo = mock(CatalogoPort.class);
        when(catalogo.consultar(anyLong(), anyLong(), anyLong())).thenReturn(new CatalogoPort.Producto(CIEN, List.of()));
        var servicio = new RegistrarPedidoService(mock(PedidoRepositorioPort.class), catalogo, mock(ClientePort.class),
            mock(NotificacionPort.class));
        assertThatThrownBy(() -> servicio.registrar(comando)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaProductosQueElCatalogoYaNoTiene() {
        var servicio = new RegistrarPedidoService(mock(PedidoRepositorioPort.class), mock(CatalogoPort.class),
            mock(ClientePort.class), mock(NotificacionPort.class));
        var comando = comando(1, 1, null, CLIENTE, List.of(new ProductoSolicitado(1, 1)), "LOCAL", null,
            BigDecimal.ZERO, MetodoPago.EFECTIVO);
        assertThatThrownBy(() -> servicio.registrar(comando)).isInstanceOf(ProductoPedidoNoDisponibleException.class);
    }

    private static void assertInvalidos(Executable... casos) {
        for (Executable caso : casos) {
            assertThatThrownBy(caso::execute).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
