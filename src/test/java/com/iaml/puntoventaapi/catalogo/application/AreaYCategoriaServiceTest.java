package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.CatalogoContrato.ProductoPedido;
import com.iaml.puntoventaapi.catalogo.domain.Area;
import com.iaml.puntoventaapi.catalogo.domain.Categoria;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ClasificacionConProductosException;
import com.iaml.puntoventaapi.catalogo.domain.port.AreaRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.CategoriaRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.ProductoEntity;
import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.ProductoJpaRepository;
import com.iaml.puntoventaapi.negocios.NegociosContrato;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AreaYCategoriaServiceTest {

    private static final Long NEGOCIO = 100L;
    private static final Long USUARIO = 7L;

    private AreaRepositorioPort areas;
    private NegociosContrato negocios;
    private CategoriaRepositorioPort categorias;
    private ProductoRepositorioPort productos;
    private SucursalContrato sucursales;
    private AreaService areaService;
    private CategoriaService categoriaService;

    @BeforeEach
    void preparar() {
        areas = mock(AreaRepositorioPort.class);
        negocios = mock(NegociosContrato.class);
        categorias = mock(CategoriaRepositorioPort.class);
        productos = mock(ProductoRepositorioPort.class);
        sucursales = mock(SucursalContrato.class);
        areaService = new AreaService(areas, negocios, categorias, productos, sucursales);
        categoriaService = new CategoriaService(categorias, areaService, productos);
        when(negocios.perteneceAUsuario(NEGOCIO, USUARIO)).thenReturn(true);
        when(negocios.perteneceAUsuario(999L, USUARIO)).thenReturn(false);
    }

    private static Area area(Long id) {
        return new Area(id, NEGOCIO, "Cocina", null);
    }

    // ---------------- AreaService ----------------

    @Test
    void creaUnAreaDelNegocioPropio() {
        when(areas.guardar(any())).thenAnswer(i -> i.getArgument(0));

        Area creada = areaService.crear(NEGOCIO, new GuardarAreaCommand(" Cocina ", "d"), USUARIO);

        assertEquals("Cocina", creada.getNombre());
        assertThrows(CatalogoNoEncontradoException.class,
            () -> areaService.crear(999L, new GuardarAreaCommand("X", null), USUARIO));
    }

    @Test
    void listaLasAreasDelNegocioOPorSucursalAsignada() {
        when(areas.listarPorNegocio(NEGOCIO)).thenReturn(List.of(area(1L)));
        when(sucursales.negocioAutorizado(5L, 8L)).thenReturn(Optional.of(NEGOCIO));
        when(sucursales.negocioAutorizado(6L, 8L)).thenReturn(Optional.of(555L));
        when(sucursales.negocioAutorizado(7L, 8L)).thenReturn(Optional.empty());

        assertEquals(1, areaService.listar(NEGOCIO, USUARIO).size());
        assertEquals(1, areaService.listar(NEGOCIO, 8L, 5L).size()); // empleado de la sucursal, sin ser dueño
        assertThrows(CatalogoNoEncontradoException.class, () -> areaService.listar(NEGOCIO, 8L, 6L));
        assertThrows(CatalogoNoEncontradoException.class, () -> areaService.listar(NEGOCIO, 8L, 7L));
        assertThrows(CatalogoNoEncontradoException.class, () -> areaService.listar(999L, USUARIO));
    }

    @Test
    void consultaUnArea() {
        when(areas.buscarPorId(1L, NEGOCIO)).thenReturn(Optional.of(area(1L)));
        when(areas.buscarPorId(2L, NEGOCIO)).thenReturn(Optional.empty());
        when(sucursales.negocioAutorizado(5L, 8L)).thenReturn(Optional.of(NEGOCIO));

        assertEquals(1L, areaService.consultar(NEGOCIO, 1L, USUARIO).getId());
        assertEquals(1L, areaService.consultar(NEGOCIO, 1L, 8L, 5L).getId());
        assertThrows(CatalogoNoEncontradoException.class, () -> areaService.consultar(NEGOCIO, 2L, USUARIO));
        assertThrows(CatalogoNoEncontradoException.class, () -> areaService.consultar(NEGOCIO, 2L, USUARIO, null));
        assertThrows(CatalogoNoEncontradoException.class, () -> areaService.consultar(999L, 1L, USUARIO));
    }

    @Test
    void editaUnAreaYFallaSiNoExiste() {
        when(areas.buscarPorIdParaActualizar(1L, NEGOCIO)).thenReturn(Optional.of(area(1L)));
        when(areas.buscarPorIdParaActualizar(2L, NEGOCIO)).thenReturn(Optional.empty());
        when(areas.guardar(any())).thenAnswer(i -> i.getArgument(0));

        assertEquals("Barra", areaService.editar(NEGOCIO, 1L, new GuardarAreaCommand("Barra", null), USUARIO).getNombre());
        assertThrows(CatalogoNoEncontradoException.class,
            () -> areaService.editar(NEGOCIO, 2L, new GuardarAreaCommand("X", null), USUARIO));
    }

    @Test
    void eliminaElAreaYSusCategoriasSiNoTieneProductos() {
        Categoria c1 = new Categoria(10L, 1L, "Café", null);
        Categoria c2 = new Categoria(11L, 1L, "Té", null);
        when(areas.buscarPorIdParaActualizar(1L, NEGOCIO)).thenReturn(Optional.of(area(1L)));
        when(productos.existenPorArea(1L)).thenReturn(false);
        when(categorias.listarPorArea(1L, NEGOCIO)).thenReturn(List.of(c1, c2));

        areaService.eliminar(NEGOCIO, 1L, USUARIO);

        verify(categorias, times(2)).guardar(any(), eq(NEGOCIO));
        ArgumentCaptor<Area> captor = ArgumentCaptor.forClass(Area.class);
        verify(areas).guardar(captor.capture());
        assertFalse(captor.getValue().isActivo());
    }

    @Test
    void noEliminaUnAreaConProductos() {
        when(areas.buscarPorIdParaActualizar(1L, NEGOCIO)).thenReturn(Optional.of(area(1L)));
        when(productos.existenPorArea(1L)).thenReturn(true);

        assertThrows(ClasificacionConProductosException.class, () -> areaService.eliminar(NEGOCIO, 1L, USUARIO));
        verify(areas, never()).guardar(any());
    }

    // ---------------- CategoriaService ----------------

    private void areaDisponible() {
        when(areas.buscarPorIdParaActualizar(1L, NEGOCIO)).thenReturn(Optional.of(area(1L)));
        when(areas.buscarPorId(1L, NEGOCIO)).thenReturn(Optional.of(area(1L)));
    }

    @Test
    void creaUnaCategoriaEnUnAreaPropia() {
        areaDisponible();
        when(categorias.guardar(any(), eq(NEGOCIO))).thenAnswer(i -> i.getArgument(0));

        Categoria creada = categoriaService.crear(NEGOCIO, 1L, new GuardarCategoriaCommand("Café", null), USUARIO);

        assertEquals("Café", creada.getNombre());
    }

    @Test
    void listaCategoriasPorAreaYPorNegocio() {
        areaDisponible();
        when(categorias.listarPorArea(1L, NEGOCIO)).thenReturn(List.of(new Categoria(1L, 1L, "Café", null)));
        when(categorias.listarPorNegocio(NEGOCIO)).thenReturn(List.of());
        when(sucursales.negocioAutorizado(5L, 8L)).thenReturn(Optional.of(NEGOCIO));

        assertEquals(1, categoriaService.listar(NEGOCIO, 1L, USUARIO).size());
        assertEquals(1, categoriaService.listar(NEGOCIO, 1L, 8L, 5L).size());
        assertTrue(categoriaService.listarPorNegocio(NEGOCIO, USUARIO, null).isEmpty());
        assertTrue(categoriaService.listarPorNegocio(NEGOCIO, 8L, 5L).isEmpty());
    }

    @Test
    void consultaYEditaUnaCategoria() {
        areaDisponible();
        when(categorias.buscarPorId(3L, 1L, NEGOCIO)).thenReturn(Optional.of(new Categoria(3L, 1L, "Café", null)));
        when(categorias.buscarPorId(4L, 1L, NEGOCIO)).thenReturn(Optional.empty());
        when(categorias.guardar(any(), eq(NEGOCIO))).thenAnswer(i -> i.getArgument(0));

        assertEquals(3L, categoriaService.consultar(NEGOCIO, 1L, 3L, USUARIO).getId());
        assertEquals("Té", categoriaService.editar(NEGOCIO, 1L, 3L, new GuardarCategoriaCommand("Té", null), USUARIO)
            .getNombre());
        assertThrows(CatalogoNoEncontradoException.class, () -> categoriaService.consultar(NEGOCIO, 1L, 4L, USUARIO));
        assertThrows(CatalogoNoEncontradoException.class,
            () -> categoriaService.editar(NEGOCIO, 1L, 4L, new GuardarCategoriaCommand("Té", null), USUARIO));
    }

    @Test
    void eliminaUnaCategoriaSinProductosYRechazaConProductos() {
        areaDisponible();
        when(categorias.buscarPorId(3L, 1L, NEGOCIO)).thenReturn(Optional.of(new Categoria(3L, 1L, "Café", null)));
        when(productos.existenPorCategoria(3L)).thenReturn(false, true);

        categoriaService.eliminar(NEGOCIO, 1L, 3L, USUARIO);
        verify(categorias).guardar(any(), eq(NEGOCIO));

        assertThrows(ClasificacionConProductosException.class,
            () -> categoriaService.eliminar(NEGOCIO, 1L, 3L, USUARIO));
        verify(categorias, times(1)).guardar(any(), eq(NEGOCIO));
    }

    // ---------------- CatalogoContratoService ----------------

    @Test
    void contratoDevuelvePrecioYEscalonesDelProducto() {
        ProductoJpaRepository jpa = mock(ProductoJpaRepository.class);
        DescuentoRepositorioPort descuentos = mock(DescuentoRepositorioPort.class);
        ProductoEntity entity = new ProductoEntity();
        entity.setPrecio(new BigDecimal("12.50"));
        when(jpa.productoParaPedido(1L, NEGOCIO)).thenReturn(Optional.of(entity));
        when(jpa.productoParaPedido(2L, NEGOCIO)).thenReturn(Optional.empty());
        when(descuentos.buscarEscalonesActivosPorProducto(1L)).thenReturn(List.of(
            new Descuento(1L, NEGOCIO, "Mayoreo", 5, null, new BigDecimal("10"), true)));
        CatalogoContratoService contrato = new CatalogoContratoService(jpa, descuentos);

        ProductoPedido pedido = contrato.productoParaPedido(1L, NEGOCIO).orElseThrow();

        assertEquals(new BigDecimal("12.50"), pedido.precio());
        assertEquals(5, pedido.escalones().get(0).cantidadMinima());
        assertNull(pedido.escalones().get(0).cantidadMaxima());
        assertEquals(new BigDecimal("10"), pedido.escalones().get(0).porcentaje());
        assertTrue(contrato.productoParaPedido(2L, NEGOCIO).isEmpty());
        assertNotNull(ReflectionTestUtils.getField(entity, "precio"));
    }

    @Test
    void comandosSonRegistrosSimples() {
        assertEquals("N", new GuardarAreaCommand("N", "d").nombre());
        assertEquals("d", new GuardarAreaCommand("N", "d").descripcion());
        assertEquals("N", new GuardarCategoriaCommand("N", "d").nombre());
        assertEquals("d", new GuardarCategoriaCommand("N", "d").descripcion());
        assertEquals(1, new ExistenciaInicialCommand(2L, 1).existencia());
        assertEquals(2L, new ExistenciaInicialCommand(2L, 1).sucursalId());
        DescuentoProductoCommand d = new DescuentoProductoCommand(1, 2, BigDecimal.TEN);
        assertEquals(1, d.cantidadMinima());
        assertEquals(2, d.cantidadMaxima());
        assertEquals(BigDecimal.TEN, d.porcentaje());
        CrearProductoCommand crear = new CrearProductoCommand("N", BigDecimal.ONE, 1L, "u", "p", d, List.of());
        assertEquals("u", crear.imagenUrl());
        assertEquals("p", crear.imagenPublicId());
        EditarProductoCommand editar = new EditarProductoCommand(1L, "N", BigDecimal.ONE, 2L, "u", "p", d);
        assertEquals("u", editar.imagenUrl());
        assertEquals("p", editar.imagenPublicId());
        assertEquals(2L, editar.categoriaId());
        LineaDescuentoCommand linea = new LineaDescuentoCommand(1L, "N", 1, 2, BigDecimal.ONE, true);
        assertTrue(linea.activo());
        GuardarDescuentoCommand guardar = new GuardarDescuentoCommand("N", 1, 2, BigDecimal.ONE);
        assertEquals("N", guardar.nombre());
        assertEquals(1, guardar.cantidadMinima());
        assertEquals(2, guardar.cantidadMaxima());
        assertEquals(BigDecimal.ONE, guardar.porcentaje());
        assertEquals(1L, linea.id());
        assertEquals("N", linea.nombre());
        assertEquals(1, linea.cantidadMinima());
        assertEquals(2, linea.cantidadMaxima());
        assertEquals(BigDecimal.ONE, linea.porcentaje());
    }
}
