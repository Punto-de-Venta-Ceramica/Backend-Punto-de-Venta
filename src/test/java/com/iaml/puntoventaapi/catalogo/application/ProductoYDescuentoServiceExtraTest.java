package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.ClasificacionProducto;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.DescuentoConAsignaciones;
import com.iaml.puntoventaapi.catalogo.domain.DestinoDescuento;
import com.iaml.puntoventaapi.catalogo.domain.FiltroProductos;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.ProductoDetalle;
import com.iaml.puntoventaapi.catalogo.domain.AsignacionesDescuento;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.CategoriaNoEncontradaException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DescuentoTraslapadoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.port.AlmacenImagenesPort;
import com.iaml.puntoventaapi.catalogo.domain.port.CategoriaRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoGestionPort;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.InventarioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.NegocioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.SucursalPort;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductoYDescuentoServiceExtraTest {

    private static final Long USUARIO = 7L;
    private static final Long NEGOCIO = 100L;
    private static final String URL = "https://res.cloudinary.com/demo/image/upload/a.jpg";
    private static final String URL_2 = "https://res.cloudinary.com/demo/image/upload/b.jpg";

    private ProductoRepositorioPort productos;
    private CategoriaRepositorioPort categorias;
    private DescuentoRepositorioPort descuentos;
    private NegocioPort negocios;
    private SucursalPort sucursales;
    private InventarioPort inventario;
    private AlmacenImagenesPort almacen;
    private ProductoService service;

    @BeforeEach
    void preparar() {
        productos = mock(ProductoRepositorioPort.class);
        categorias = mock(CategoriaRepositorioPort.class);
        descuentos = mock(DescuentoRepositorioPort.class);
        negocios = mock(NegocioPort.class);
        sucursales = mock(SucursalPort.class);
        inventario = mock(InventarioPort.class);
        almacen = mock(AlmacenImagenesPort.class);
        service = new ProductoService(productos, categorias, descuentos, negocios, sucursales, inventario, almacen);
        when(negocios.perteneceAUsuario(NEGOCIO, USUARIO)).thenReturn(true);
        when(categorias.buscarNegocioId(1L)).thenReturn(Optional.of(NEGOCIO));
        when(categorias.buscarNegocioIdParaActualizar(1L)).thenReturn(Optional.of(NEGOCIO));
        when(sucursales.idsPorNegocio(NEGOCIO, USUARIO)).thenReturn(List.of(10L, 11L));
        when(productos.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(productos.buscarClasificacion(1L)).thenReturn(Optional.empty());
        when(descuentos.buscarEscalonesActivosPorProducto(anyLong())).thenReturn(List.of());
        when(descuentos.buscarActivoPorProducto(anyLong())).thenReturn(Optional.empty());
    }

    private Producto producto(String url, String publicId) {
        return new Producto(5L, 1L, "Taza", BigDecimal.TEN, url, publicId, true);
    }

    private CrearProductoCommand crear(List<ExistenciaInicialCommand> existencias) {
        return new CrearProductoCommand("Taza", BigDecimal.TEN, 1L, null, existencias);
    }

    // ---------- crear: existencias iniciales ----------

    @Test
    void crearValidaLasExistenciasIniciales() {
        when(sucursales.idsPorNegocio(NEGOCIO, USUARIO)).thenReturn(List.of());
        assertThrows(ProductoInvalidoException.class, () -> service.crear(crear(null), USUARIO));

        when(sucursales.idsPorNegocio(NEGOCIO, USUARIO)).thenReturn(List.of(10L, 11L));
        assertThrows(ProductoInvalidoException.class, () -> service.crear(crear(null), USUARIO));
        assertThrows(ProductoInvalidoException.class, () -> service.crear(crear(List.of()), USUARIO));
        assertThrows(ProductoInvalidoException.class,
            () -> service.crear(crear(List.of(new ExistenciaInicialCommand(null, 1))), USUARIO));
        assertThrows(ProductoInvalidoException.class,
            () -> service.crear(crear(List.of(new ExistenciaInicialCommand(99L, 1))), USUARIO));
        assertThrows(ProductoInvalidoException.class,
            () -> service.crear(crear(List.of(new ExistenciaInicialCommand(10L, null))), USUARIO));
        assertThrows(ProductoInvalidoException.class,
            () -> service.crear(crear(List.of(new ExistenciaInicialCommand(10L, -1))), USUARIO));
        assertThrows(ProductoInvalidoException.class, () -> service.crear(
            crear(List.of(new ExistenciaInicialCommand(10L, 1), new ExistenciaInicialCommand(10L, 2))), USUARIO));
        verify(productos, never()).guardar(any());
    }

    @Test
    void crearRechazaCategoriasAjenasOInexistentes() {
        when(categorias.buscarNegocioIdParaActualizar(2L)).thenReturn(Optional.empty());
        when(categorias.buscarNegocioIdParaActualizar(3L)).thenReturn(Optional.of(555L));

        assertThrows(CategoriaNoEncontradaException.class,
            () -> service.crear(new CrearProductoCommand("T", BigDecimal.ONE, 2L, null, null), USUARIO));
        assertThrows(CategoriaNoEncontradaException.class,
            () -> service.crear(new CrearProductoCommand("T", BigDecimal.ONE, 3L, null, null), USUARIO));
    }

    @Test
    void crearConDescuentoPropioLoGuardaYVincula() {
        when(descuentos.guardar(any())).thenAnswer(i -> {
            Descuento d = i.getArgument(0);
            return new Descuento(9L, d.getNegocioId(), d.getNombre(), d.getCantidadMinima(), d.getCantidadMaxima(),
                d.getPorcentaje(), true);
        });
        CrearProductoCommand comando = new CrearProductoCommand("Taza", BigDecimal.TEN, 1L, null, null,
            new DescuentoProductoCommand(2, null, BigDecimal.ONE), List.of(new ExistenciaInicialCommand(10L, 3)));
        when(productos.guardar(any())).thenAnswer(i -> {
            Producto p = i.getArgument(0);
            return new Producto(5L, p.getCategoriaId(), p.getNombre(), p.getPrecio(), true);
        });

        ProductoDetalle detalle = service.crear(comando, USUARIO);

        assertEquals(9L, detalle.descuento().getId());
        verify(descuentos).vincularAProducto(9L, 5L);
        verify(inventario).registrarExistenciasIniciales(5L, Map.of(10L, 3));
    }

    // ---------- editar ----------

    @Test
    void editarFallaSiElProductoOLaCategoriaNoExisten() {
        when(productos.buscarPorId(5L, USUARIO)).thenReturn(Optional.empty());
        EditarProductoCommand comando = new EditarProductoCommand(5L, "Taza", BigDecimal.TEN, 1L, null);
        assertThrows(ProductoNoEncontradoException.class, () -> service.editar(comando, USUARIO));

        when(productos.buscarPorId(5L, USUARIO)).thenReturn(Optional.of(producto(null, null)));
        when(categorias.buscarNegocioId(1L)).thenReturn(Optional.empty());
        assertThrows(CategoriaNoEncontradaException.class, () -> service.editar(comando, USUARIO));
    }

    @Test
    void editarNoPermiteMoverElProductoAOtroNegocio() {
        when(productos.buscarPorId(5L, USUARIO)).thenReturn(Optional.of(producto(null, null)));
        when(categorias.buscarNegocioIdParaActualizar(2L)).thenReturn(Optional.of(101L));
        when(negocios.perteneceAUsuario(101L, USUARIO)).thenReturn(true);

        assertThrows(ProductoInvalidoException.class,
            () -> service.editar(new EditarProductoCommand(5L, "Taza", BigDecimal.TEN, 2L, null), USUARIO));
    }

    @Test
    void editarEliminaLaImagenAnteriorSoloSiCambioYNoLaUsaOtroProducto() {
        when(productos.buscarPorId(5L, USUARIO)).thenAnswer(i -> Optional.of(producto(URL, "viejo")));
        when(productos.imagenUsadaPorOtroProducto("viejo", 5L)).thenReturn(false, true, false);

        // Cambia el identificador público: se elimina la anterior.
        service.editar(new EditarProductoCommand(5L, "Taza", BigDecimal.TEN, 1L, URL, "nuevo", null), USUARIO);
        verify(almacen).eliminar("viejo");

        // Cambia la imagen pero la usa otro producto: no se elimina.
        service.editar(new EditarProductoCommand(5L, "Taza", BigDecimal.TEN, 1L, URL_2, "viejo2", null), USUARIO);
        verify(almacen, org.mockito.Mockito.times(1)).eliminar(any());

        // Cambia solo la URL, conservando el identificador público.
        service.editar(new EditarProductoCommand(5L, "Taza", BigDecimal.TEN, 1L, URL_2, "viejo", null), USUARIO);
        verify(almacen, org.mockito.Mockito.times(2)).eliminar("viejo");
    }

    @Test
    void editarSinCambiarLaImagenNoLaTocaYSinImagenPreviaTampoco() {
        when(productos.buscarPorId(5L, USUARIO)).thenReturn(Optional.of(producto(URL, "igual")));
        service.editar(new EditarProductoCommand(5L, "Taza", BigDecimal.TEN, 1L, URL, "igual", null), USUARIO);

        when(productos.buscarPorId(5L, USUARIO)).thenReturn(Optional.of(producto(null, null)));
        service.editar(new EditarProductoCommand(5L, "Taza", BigDecimal.TEN, 1L, URL, "nueva", null), USUARIO);

        verify(almacen, never()).eliminar(any());
    }

    @Test
    void editarConDescuentoModificaElPropioReutilizaElHeredadoOCreaUnoNuevo() {
        when(productos.buscarPorId(5L, USUARIO)).thenAnswer(i -> Optional.of(producto(null, null)));
        when(descuentos.guardar(any())).thenAnswer(i -> {
            Descuento d = i.getArgument(0);
            return d.getId() != null ? d
                : new Descuento(9L, d.getNegocioId(), d.getNombre(), d.getCantidadMinima(), d.getCantidadMaxima(),
                    d.getPorcentaje(), true);
        });
        DescuentoProductoCommand datos = new DescuentoProductoCommand(2, 5, new BigDecimal("10"));

        // 1) Ya tiene uno propio: se modifica.
        Descuento propio = new Descuento(3L, NEGOCIO, "Descuento Taza", 1, null, BigDecimal.ONE, true);
        when(descuentos.buscarPropioActivoDeProducto(5L)).thenReturn(Optional.of(propio));
        assertSame(propio, service.editar(new EditarProductoCommand(5L, "Taza", BigDecimal.TEN, 1L, datos), USUARIO)
            .descuento());
        assertEquals(2, propio.getCantidadMinima());

        // 2) Sin propio y con uno heredado idéntico: se reutiliza.
        when(descuentos.buscarPropioActivoDeProducto(5L)).thenReturn(Optional.empty());
        Descuento heredado = new Descuento(4L, NEGOCIO, "Categoría", 2, 5, new BigDecimal("10.00"), true);
        when(descuentos.buscarActivoPorProducto(5L)).thenReturn(Optional.of(heredado));
        assertSame(heredado, service.editar(new EditarProductoCommand(5L, "Taza", BigDecimal.TEN, 1L, datos),
            USUARIO).descuento());

        // 3) Heredado distinto (mínima, máxima o porcentaje) o inexistente: se crea uno propio.
        for (DescuentoProductoCommand distinto : List.of(
            new DescuentoProductoCommand(3, 5, new BigDecimal("10")),
            new DescuentoProductoCommand(2, 6, new BigDecimal("10")),
            new DescuentoProductoCommand(2, 5, new BigDecimal("11")))) {
            assertEquals(9L, service.editar(new EditarProductoCommand(5L, "Taza", BigDecimal.TEN, 1L, distinto),
                USUARIO).descuento().getId());
        }
        when(descuentos.buscarActivoPorProducto(5L)).thenReturn(Optional.empty());
        assertEquals(9L, service.editar(new EditarProductoCommand(5L, "Taza", BigDecimal.TEN, 1L, datos),
            USUARIO).descuento().getId());
        // Porcentaje nulo en los datos con heredado presente: no coincide.
        when(descuentos.buscarActivoPorProducto(5L)).thenReturn(Optional.of(heredado));
        assertThrows(RuntimeException.class, () -> service.editar(new EditarProductoCommand(5L, "Taza",
            BigDecimal.TEN, 1L, new DescuentoProductoCommand(2, 5, null)), USUARIO));
    }

    // ---------- eliminar ----------

    @Test
    void eliminarDesactivaYBorraLaImagenSiNadieMasLaUsa() {
        when(productos.buscarPorId(5L, USUARIO)).thenReturn(Optional.of(producto(URL, "img")));
        when(productos.imagenUsadaPorOtroProducto("img", 5L)).thenReturn(false);

        service.eliminar(5L, USUARIO);

        verify(almacen).eliminar("img");
    }

    @Test
    void eliminarConservaLaImagenSiLaUsaOtroProductoYTolerarFallosDelAlmacen() {
        when(productos.buscarPorId(5L, USUARIO)).thenAnswer(i -> Optional.of(producto(URL, "img")));
        when(productos.imagenUsadaPorOtroProducto("img", 5L)).thenReturn(true, false);

        service.eliminar(5L, USUARIO);
        verify(almacen, never()).eliminar(any());

        doThrow(new IllegalStateException("cloudinary caído")).when(almacen).eliminar("img");
        assertDoesNotThrow(() -> service.eliminar(5L, USUARIO));
    }

    @Test
    void eliminarSinImagenOSinProducto() {
        when(productos.buscarPorId(5L, USUARIO)).thenReturn(Optional.of(producto(null, null)));
        service.eliminar(5L, USUARIO);
        verify(almacen, never()).eliminar(any());

        when(productos.buscarPorId(6L, USUARIO)).thenReturn(Optional.empty());
        assertThrows(ProductoNoEncontradoException.class, () -> service.eliminar(6L, USUARIO));
    }

    // ---------- consultar / listar / catálogo ----------

    @Test
    void consultarPorPropietarioOPorSucursalDelEmpleado() {
        ProductoDetalle detalle = new ProductoDetalle(producto(null, null), null,
            new ClasificacionProducto(1L, "A", 2L, "C"));
        when(productos.buscarConClasificacion(5L, USUARIO)).thenReturn(Optional.of(detalle));
        when(productos.buscarConClasificacion(5L, 8L)).thenReturn(Optional.empty());
        when(productos.buscarConClasificacion(6L, 8L)).thenReturn(Optional.empty());
        when(negocios.negocioDeSucursal(3L, 8L)).thenReturn(Optional.of(NEGOCIO));
        when(negocios.negocioDeSucursal(4L, 8L)).thenReturn(Optional.empty());
        when(productos.buscarConClasificacionEnNegocio(5L, NEGOCIO)).thenReturn(Optional.of(detalle));
        when(productos.buscarConClasificacionEnNegocio(6L, NEGOCIO)).thenReturn(Optional.empty());
        Descuento escalon = new Descuento(2L, NEGOCIO, "E", 1, null, BigDecimal.ONE, true);
        when(descuentos.buscarEscalonesActivosPorProducto(5L)).thenReturn(List.of(escalon));

        assertSame(escalon, service.consultar(5L, USUARIO).descuento());
        assertEquals("C", service.consultar(5L, 8L, 3L).clasificacion().categoriaNombre());
        assertThrows(ProductoNoEncontradoException.class, () -> service.consultar(5L, 8L));
        assertThrows(ProductoNoEncontradoException.class, () -> service.consultar(5L, 8L, 4L));
        assertThrows(ProductoNoEncontradoException.class, () -> service.consultar(6L, 8L, 3L));
    }

    @Test
    void listarAcotaLaPaginaYValidaElAcceso() {
        Pagina<Producto> vacia = new Pagina<>(List.of(), 0, 100, 0);
        when(negocios.puedeConsultar(NEGOCIO, null, USUARIO)).thenReturn(true);
        when(negocios.puedeConsultar(999L, null, USUARIO)).thenReturn(false);
        when(productos.listarPagina(eq(NEGOCIO), any(), eq(0), eq(100))).thenReturn(vacia);
        when(productos.listarPagina(eq(NEGOCIO), any(), eq(2), eq(1))).thenReturn(vacia);

        assertSame(vacia, service.listar(NEGOCIO, USUARIO, FiltroProductos.SIN_FILTROS, -5, 5000));
        assertSame(vacia, service.listar(NEGOCIO, USUARIO, FiltroProductos.SIN_FILTROS, 2, 0));
        assertThrows(NegocioNoEncontradoException.class,
            () -> service.listar(999L, USUARIO, FiltroProductos.SIN_FILTROS, 0, 10));
    }

    @Test
    void catalogoListaLosProductosDelNegocioPropio() {
        when(productos.listarPorNegocio(NEGOCIO)).thenReturn(List.of(producto(null, null)));

        assertEquals(1, service.catalogo(NEGOCIO, USUARIO).size());
        assertThrows(NegocioNoEncontradoException.class, () -> service.catalogo(999L, USUARIO));
    }

    // ================= DescuentoService =================

    @Test
    void descuentoServiceCubreDestinosEstadosYLineas() {
        DescuentoGestionPort gestion = mock(DescuentoGestionPort.class);
        com.iaml.puntoventaapi.catalogo.domain.port.NegocioPort permisos =
            mock(com.iaml.puntoventaapi.catalogo.domain.port.NegocioPort.class);
        when(permisos.perteneceAUsuario(NEGOCIO, USUARIO)).thenReturn(true);
        DescuentoService descuentoService = new DescuentoService(gestion, permisos);

        Descuento existente = new Descuento(3L, NEGOCIO, "Mayoreo", 5, 10, BigDecimal.TEN, true);
        Descuento otro = new Descuento(4L, NEGOCIO, "Otro", 6, 12, BigDecimal.ONE, true);
        when(gestion.buscarPorId(3L, NEGOCIO)).thenReturn(Optional.of(existente));
        when(gestion.destinoPerteneceAlNegocio(any(), anyLong(), eq(NEGOCIO))).thenReturn(true);
        when(gestion.asignaciones(anyLong())).thenReturn(AsignacionesDescuento.vacia());
        when(gestion.guardar(any())).thenAnswer(i -> i.getArgument(0));

        // Mensajes y destinos: traslape en categoría y en área; destino inexistente de cada tipo.
        for (DestinoDescuento destino : DestinoDescuento.values()) {
            when(gestion.activosAsignadosA(eq(destino), anyCollection())).thenReturn(Map.of(1L, List.of(otro)));
            DescuentoTraslapadoException ex = assertThrows(DescuentoTraslapadoException.class,
                () -> descuentoService.asignar(NEGOCIO, 3L, destino, 20L, USUARIO));
            assertTrue(ex.getMessage().contains("Otro"));
            when(gestion.destinoPerteneceAlNegocio(destino, 21L, NEGOCIO)).thenReturn(false);
            assertThrows(CatalogoNoEncontradoException.class,
                () -> descuentoService.asignar(NEGOCIO, 3L, destino, 21L, USUARIO));
        }

        // Guardar un producto: línea existente (activa e inactiva), línea nueva y baja de otro.
        when(gestion.activosAsignadosA(any(), anyCollection())).thenReturn(Map.of());
        when(gestion.destinoPerteneceAlNegocio(DestinoDescuento.PRODUCTO, 8L, NEGOCIO)).thenReturn(true);
        when(gestion.buscarPorId(4L, NEGOCIO)).thenReturn(Optional.of(otro));
        when(gestion.asignacionesDe(anyCollection())).thenReturn(Map.of(3L, AsignacionesDescuento.vacia(),
            4L, AsignacionesDescuento.vacia()));
        List<DescuentoConAsignaciones> guardados = descuentoService.guardarDeProducto(NEGOCIO, 8L, List.of(),
            List.of(new LineaDescuentoCommand(3L, "Mayoreo", 5, 10, BigDecimal.TEN, true),
                new LineaDescuentoCommand(4L, "Otro", 11, null, BigDecimal.ONE, false)), USUARIO);

        assertTrue(guardados.get(0).descuento().isActivo());
        assertFalse(guardados.get(1).descuento().isActivo());
        verify(gestion, org.mockito.Mockito.atLeastOnce()).asignar(anyLong(), eq(DestinoDescuento.PRODUCTO), eq(8L));
        assertTrue(Set.of().isEmpty());
    }
}
