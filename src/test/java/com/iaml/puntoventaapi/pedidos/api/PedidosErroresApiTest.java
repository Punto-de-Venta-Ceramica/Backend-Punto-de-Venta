package com.iaml.puntoventaapi.pedidos.api;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.iaml.puntoventaapi.pedidos.api.registrar.RegistrarPedidoController;
import com.iaml.puntoventaapi.pedidos.application.registrar.RegistrarPedidoService;
import com.iaml.puntoventaapi.pedidos.domain.CorteCajaInvalidoException;
import com.iaml.puntoventaapi.pedidos.domain.PagoExcedeSaldoException;
import com.iaml.puntoventaapi.pedidos.domain.ProductoPedidoNoDisponibleException;
import com.iaml.puntoventaapi.shared.infrastructure.json.JsonConfig;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RegistrarPedidoController.class,
    properties = {"spring.config.import=", "jwt.secreto=clave-de-pruebas-de-al-menos-32-bytes"})
@AutoConfigureMockMvc(addFilters = false)
@Import(JsonConfig.class)
class PedidosErroresApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean RegistrarPedidoService pedidos;
    @MockitoBean UsuarioActual usuario;

    private static final String PEDIDO = """
        {"sucursalId":1,"cliente":{"nombre":"Marco","telefono":"5512345678"},
         "productos":[{"productoId":1,"cantidad":3}],"tipoEntrega":"LOCAL",
         "pagoInicial":{"monto":30,"metodoPago":"EFECTIVO"}}
        """;

    static Stream<Arguments> erroresDeNegocio() {
        return Stream.of(
            Arguments.of(new CorteCajaInvalidoException(), "CORTE_CAJA_NO_ABIERTO"),
            Arguments.of(new PagoExcedeSaldoException(), "PAGO_SUPERA_SALDO"),
            Arguments.of(new ProductoPedidoNoDisponibleException(), "PRODUCTO_NO_DISPONIBLE"));
    }

    @ParameterizedTest
    @MethodSource("erroresDeNegocio")
    void devuelveUnCodigoEspecificoSinAccederALaBase(RuntimeException error, String codigo) throws Exception {
        when(usuario.id()).thenReturn(1L);
        when(pedidos.registrar(any())).thenThrow(error);
        mvc.perform(post("/api/pedidos").contentType("application/json").content(PEDIDO))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value(codigo))
            .andExpect(jsonPath("$.detail").isString());
    }

    @Test
    void conservaElErrorPorCampoParaTelefonoInvalido() throws Exception {
        var invalido = PEDIDO.replace("5512345678", "123");
        mvc.perform(post("/api/pedidos").contentType("application/json").content(invalido))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores['cliente.telefono']")
                .value("El teléfono debe contener exactamente 10 dígitos."));
    }

    @Test
    void noPublicaDetallesTecnicosDeErroresDesconocidos() throws Exception {
        when(usuario.id()).thenReturn(1L);
        when(pedidos.registrar(any())).thenThrow(new IllegalArgumentException("SQL SECRETO"));
        mvc.perform(post("/api/pedidos").contentType("application/json").content(PEDIDO))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(not(containsString("SQL SECRETO"))));
    }
}
