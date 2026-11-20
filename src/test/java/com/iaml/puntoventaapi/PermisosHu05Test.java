package com.iaml.puntoventaapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.iaml.puntoventaapi.caja.api.CorteCajaController;
import com.iaml.puntoventaapi.catalogo.api.AreaController;
import com.iaml.puntoventaapi.catalogo.api.ProductoController;
import com.iaml.puntoventaapi.inventario.api.InventarioController;
import com.iaml.puntoventaapi.pedidos.api.abonar.RegistrarPagoController;
import com.iaml.puntoventaapi.pedidos.api.cambiarestado.CambiarEstadoController;
import com.iaml.puntoventaapi.pedidos.api.cancelar.CancelarPedidoController;
import com.iaml.puntoventaapi.pedidos.api.consultar.ConsultarPedidosController;
import com.iaml.puntoventaapi.pedidos.api.editar.EditarDatosPedidoController;
import com.iaml.puntoventaapi.pedidos.api.registrar.RegistrarPedidoController;
import com.iaml.puntoventaapi.ventas.api.TicketController;
import com.iaml.puntoventaapi.ventas.api.VentaController;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Matriz de la HU-05 (accesos y permisos): permiso efectivo de cada acción del ticket, el del método o, si
 * no tiene, el de la clase. El front oculta exactamente lo que aquí es solo del propietario.
 */
class PermisosHu05Test {

    private static final String AMBOS = "hasAnyRole('PROPIETARIO', 'EMPLEADO')";
    private static final String PROPIETARIO = "hasRole('PROPIETARIO')";

    static Stream<Arguments> matriz() {
        return Stream.of(
            // El empleado registra ventas (HU-08), pedidos y productos.
            Arguments.of(VentaController.class, null, AMBOS),
            Arguments.of(TicketController.class, null, AMBOS),
            Arguments.of(RegistrarPedidoController.class, null, AMBOS),
            Arguments.of(ConsultarPedidosController.class, null, AMBOS),
            Arguments.of(RegistrarPagoController.class, null, AMBOS),
            Arguments.of(CambiarEstadoController.class, null, AMBOS), // las reglas por estado están en el dominio
            Arguments.of(ProductoController.class, "crear", AMBOS),
            Arguments.of(ProductoController.class, "listar", AMBOS),
            Arguments.of(ProductoController.class, "consultar", AMBOS),
            Arguments.of(InventarioController.class, "panel", AMBOS),
            Arguments.of(AreaController.class, "listar", AMBOS),
            Arguments.of(CorteCajaController.class, "abrirCorte", AMBOS),
            Arguments.of(CorteCajaController.class, "obtenerCorteActivo", AMBOS),
            Arguments.of(CorteCajaController.class, "cerrarCorte", AMBOS),
            // El propietario modifica productos, existencias, pedidos y datos del cliente.
            Arguments.of(ProductoController.class, "editar", PROPIETARIO),
            Arguments.of(ProductoController.class, "eliminar", PROPIETARIO),
            Arguments.of(InventarioController.class, "ajustar", PROPIETARIO),
            Arguments.of(InventarioController.class, "asignar", PROPIETARIO),
            Arguments.of(InventarioController.class, "quitar", PROPIETARIO),
            Arguments.of(EditarDatosPedidoController.class, null, PROPIETARIO),
            Arguments.of(AreaController.class, "crear", PROPIETARIO),
            // Solo el propietario cancela pedidos.
            Arguments.of(CancelarPedidoController.class, null, PROPIETARIO),
            // Información financiera: cortes anteriores.
            Arguments.of(CorteCajaController.class, "listarHistorial", PROPIETARIO),
            Arguments.of(CorteCajaController.class, "consultarCorte", PROPIETARIO));
    }

    @ParameterizedTest(name = "{0}.{1} -> {2}")
    @MethodSource("matriz")
    void permisoEfectivo(Class<?> controlador, String metodo, String esperado) {
        PreAuthorize clase = controlador.getAnnotation(PreAuthorize.class);
        if (metodo == null) {
            assertEquals(esperado, clase == null ? null : clase.value());
            return;
        }
        PreAuthorize propio = Arrays.stream(controlador.getDeclaredMethods())
            .filter(m -> m.getName().equals(metodo)).findFirst().orElseThrow()
            .getAnnotation(PreAuthorize.class);
        PreAuthorize efectivo = propio != null ? propio : clase;
        assertEquals(esperado, efectivo == null ? null : efectivo.value());
    }
}
