package com.iaml.puntoventaapi.catalogo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.iaml.puntoventaapi.catalogo.application.CrearProductoCommand;
import com.iaml.puntoventaapi.catalogo.application.ExistenciaInicialCommand;
import com.iaml.puntoventaapi.catalogo.application.ProductoService;
import com.iaml.puntoventaapi.catalogo.domain.exception.CategoriaNoEncontradaException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.port.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PermisosRegistroProductoTest {
    private final ProductoRepositorioPort productos = mock(ProductoRepositorioPort.class);
    private final CategoriaRepositorioPort categorias = mock(CategoriaRepositorioPort.class);
    private final DescuentoRepositorioPort descuentos = mock(DescuentoRepositorioPort.class);
    private final NegocioPort negocios = mock(NegocioPort.class);
    private final SucursalPort sucursales = mock(SucursalPort.class);
    private final InventarioPort inventario = mock(InventarioPort.class);
    private ProductoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ProductoService(productos, categorias, descuentos, negocios, sucursales, inventario);
        when(categorias.buscarNegocioIdParaActualizar(2L)).thenReturn(Optional.of(3L));
        when(sucursales.idsParaRegistrarProductos(3L, 4L)).thenReturn(List.of(5L));
    }

    private CrearProductoCommand producto(List<ExistenciaInicialCommand> existencias) {
        return new CrearProductoCommand("Taza", new BigDecimal("50.00"), 2L, null, existencias);
    }

    @Test
    void empleadoSoloRegistraEnSuSucursalAsignada() {
        when(productos.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        servicio.crear(producto(List.of(new ExistenciaInicialCommand(5L, 3))), 4L);
        verify(inventario).registrarExistenciasIniciales(any(), eq(java.util.Map.of(5L, 3)));
    }

    @Test
    void rechazaSucursalYNegocioAjenosAntesDeGuardar() {
        assertThrows(ProductoInvalidoException.class,
            () -> servicio.crear(producto(List.of(new ExistenciaInicialCommand(6L, 3))), 4L));
        when(sucursales.idsParaRegistrarProductos(3L, 4L)).thenReturn(List.of());
        assertThrows(CategoriaNoEncontradaException.class, () -> servicio.crear(producto(null), 4L));
        verify(productos, never()).guardar(any());
        verifyNoInteractions(inventario);
    }
}
