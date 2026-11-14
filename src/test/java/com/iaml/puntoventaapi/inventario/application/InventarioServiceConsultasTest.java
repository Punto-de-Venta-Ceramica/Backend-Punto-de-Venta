package com.iaml.puntoventaapi.inventario.application;

import com.iaml.puntoventaapi.inventario.domain.ExistenciaProducto;
import com.iaml.puntoventaapi.inventario.domain.exception.InventarioNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.exception.ProductoOSucursalNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.port.InventarioRepositorioPort;
import com.iaml.puntoventaapi.inventario.domain.port.NegocioPort;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventarioServiceConsultasTest {

    private final InventarioRepositorioPort repositorio = mock(InventarioRepositorioPort.class);
    private final NegocioPort negocios = mock(NegocioPort.class);
    private final InventarioService service = new InventarioService(repositorio, negocios);

    @Test
    void consultaLasExistenciasDeUnProducto() {
        List<ExistenciaProducto> filas = List.of(new ExistenciaProducto(1L, 2L, "Taza", 3L, "Centro", 9));
        when(repositorio.listarPorProducto(2L, 7L)).thenReturn(filas);

        assertEquals(filas, service.consultarPorProducto(2L, 7L));
    }

    @Test
    void panelConFiltroDeProductoYSucursalPermitida() {
        when(negocios.puedeOperarSucursal(5L, 3L, 7L)).thenReturn(true);
        when(repositorio.listarPanel(5L, 3L, 2L, 7L)).thenReturn(List.of());

        assertEquals(List.of(), service.consultarPanel(5L, 3L, 2L, 7L));
    }

    @Test
    void asignarRechazaProductoOSucursalInaccesibles() {
        when(repositorio.negocioDeProducto(2L, 7L)).thenReturn(Optional.empty());
        assertThrows(ProductoOSucursalNoEncontradoException.class, () -> service.asignar(2L, 3L, 0, 7L));

        when(repositorio.negocioDeProducto(2L, 7L)).thenReturn(Optional.of(5L));
        when(repositorio.negocioDeSucursal(3L, 7L)).thenReturn(Optional.empty());
        assertThrows(ProductoOSucursalNoEncontradoException.class, () -> service.asignar(2L, 3L, 0, 7L));

        when(repositorio.negocioDeSucursal(3L, 7L)).thenReturn(Optional.of(6L));
        assertThrows(ProductoOSucursalNoEncontradoException.class, () -> service.asignar(2L, 3L, 0, 7L));
    }

    @Test
    void quitarYAjustarFallanSiNoHayRegistroAccesible() {
        when(repositorio.buscarParaQuitar(1L, 7L)).thenReturn(Optional.empty());
        when(repositorio.buscarParaAjuste(1L, 7L)).thenReturn(Optional.empty());

        assertThrows(InventarioNoEncontradoException.class, () -> service.quitar(1L, 7L));
        assertThrows(InventarioNoEncontradoException.class, () -> service.ajustar(1L, 1, 7L));
    }
}
