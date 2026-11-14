package com.iaml.puntoventaapi.catalogo.domain;

import com.iaml.puntoventaapi.catalogo.CatalogoContrato;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.CategoriaNoEncontradaException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ClasificacionConProductosException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DatosCatalogoInvalidosException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DescuentoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DescuentoTraslapadoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.port.NegocioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DominioCatalogoCompletoTest {

    private static final Instant FECHA = Instant.parse("2026-01-01T00:00:00Z");

    // ---------- DatosClasificacion ----------

    @Test
    void datosDeClasificacionNormalizanYValidan() {
        DatosClasificacion datos = new DatosClasificacion("  Bebidas ", "  frías  ");
        assertEquals("Bebidas", datos.nombre());
        assertEquals("frías", datos.descripcion());
        assertNull(new DatosClasificacion("A", "   ").descripcion());
        assertNull(new DatosClasificacion("A", null).descripcion());

        assertThrows(DatosCatalogoInvalidosException.class, () -> new DatosClasificacion(null, null));
        assertThrows(DatosCatalogoInvalidosException.class, () -> new DatosClasificacion("  ", null));
        assertThrows(DatosCatalogoInvalidosException.class,
            () -> new DatosClasificacion("x".repeat(DatosClasificacion.MAX_NOMBRE + 1), null));
        assertThrows(DatosCatalogoInvalidosException.class, () -> new DatosClasificacion("a\nb", null));
        assertDoesNotThrow(() -> new DatosClasificacion("x".repeat(DatosClasificacion.MAX_NOMBRE), null));
    }

    // ---------- Area ----------

    @Test
    void areaValidaSusDatosYSuEstado() {
        Area area = new Area(1L, 2L, " Cocina ", null);
        assertEquals(1L, area.getId());
        assertEquals(2L, area.getNegocioId());
        assertEquals("Cocina", area.getNombre());
        assertNull(area.getDescripcion());
        assertTrue(area.isActivo());
        assertNull(area.getFechaEliminacion());
        assertNull(new Area(null, 2L, "A", "d").getId());

        assertThrows(IllegalArgumentException.class, () -> new Area(0L, 2L, "A", null));
        assertThrows(IllegalArgumentException.class, () -> new Area(1L, null, "A", null));
        assertThrows(IllegalArgumentException.class, () -> new Area(1L, 0L, "A", null));
        assertThrows(IllegalArgumentException.class, () -> new Area(1L, 2L, "A", null, false, null));
        assertThrows(IllegalArgumentException.class, () -> new Area(1L, 2L, "A", null, true, FECHA));
    }

    @Test
    void areaSeEditaYSeElimina() {
        Area area = new Area(1L, 2L, "Cocina", "d");
        Area editada = area.editar("Barra", "x");
        Area eliminada = area.eliminar(FECHA);

        assertEquals("Barra", editada.getNombre());
        assertFalse(eliminada.isActivo());
        assertEquals(FECHA, eliminada.getFechaEliminacion());
        assertSame(eliminada, eliminada.eliminar(FECHA));
        assertThrows(CatalogoNoEncontradoException.class, () -> eliminada.editar("X", null));
        assertThrows(NullPointerException.class, () -> area.eliminar(null));
    }

    // ---------- Categoria ----------

    @Test
    void categoriaValidaSusDatosYSuEstado() {
        Categoria categoria = new Categoria(1L, 2L, " Café ", "d");
        assertEquals(1L, categoria.getId());
        assertEquals(2L, categoria.getAreaId());
        assertEquals("Café", categoria.getNombre());
        assertEquals("d", categoria.getDescripcion());
        assertTrue(categoria.isActivo());
        assertNull(categoria.getFechaEliminacion());

        assertThrows(IllegalArgumentException.class, () -> new Categoria(0L, 2L, "A", null));
        assertThrows(IllegalArgumentException.class, () -> new Categoria(1L, null, "A", null));
        assertThrows(IllegalArgumentException.class, () -> new Categoria(1L, 0L, "A", null));
        assertThrows(IllegalArgumentException.class, () -> new Categoria(1L, 2L, "A", null, false, null));
        assertThrows(IllegalArgumentException.class, () -> new Categoria(1L, 2L, "A", null, true, FECHA));
    }

    @Test
    void categoriaSeEditaYSeElimina() {
        Categoria categoria = new Categoria(null, 2L, "Café", null);
        Categoria eliminada = categoria.eliminar(FECHA);

        assertEquals("Té", categoria.editar("Té", null).getNombre());
        assertFalse(eliminada.isActivo());
        assertSame(eliminada, eliminada.eliminar(FECHA));
        assertThrows(CatalogoNoEncontradoException.class, () -> eliminada.editar("X", null));
        assertThrows(NullPointerException.class, () -> categoria.eliminar(null));
    }

    // ---------- Descuento ----------

    private static Descuento descuento(Long id, int min, Integer max) {
        return new Descuento(id, 1L, "Mayoreo", min, max, new BigDecimal("10.50"), true);
    }

    @Test
    void descuentoValidaSusDatos() {
        Descuento d = descuento(5L, 2, 10);
        assertEquals(5L, d.getId());
        assertEquals(1L, d.getNegocioId());
        assertEquals("Mayoreo", d.getNombre());
        assertEquals(2, d.getCantidadMinima());
        assertEquals(10, d.getCantidadMaxima());
        assertEquals(new BigDecimal("10.50"), d.getPorcentaje());
        assertTrue(d.isActivo());
        d.desactivar();
        assertFalse(d.isActivo());
        d.activar();
        assertTrue(d.isActivo());

        BigDecimal pct = BigDecimal.TEN;
        assertThrows(IllegalArgumentException.class, () -> new Descuento(1L, null, "N", 1, null, pct, true));
        assertThrows(IllegalArgumentException.class, () -> new Descuento(1L, 0L, "N", 1, null, pct, true));
        assertThrows(IllegalArgumentException.class, () -> new Descuento(1L, 1L, null, 1, null, pct, true));
        assertThrows(IllegalArgumentException.class, () -> new Descuento(1L, 1L, " ", 1, null, pct, true));
        assertThrows(IllegalArgumentException.class, () -> new Descuento(1L, 1L, "x".repeat(121), 1, null, pct, true));
        assertThrows(IllegalArgumentException.class, () -> new Descuento(1L, 1L, "N", null, null, pct, true));
        assertThrows(IllegalArgumentException.class, () -> new Descuento(1L, 1L, "N", 0, null, pct, true));
        assertThrows(IllegalArgumentException.class, () -> new Descuento(1L, 1L, "N", 5, 4, pct, true));
        assertThrows(IllegalArgumentException.class, () -> new Descuento(1L, 1L, "N", 1, null, null, true));
        assertThrows(IllegalArgumentException.class,
            () -> new Descuento(1L, 1L, "N", 1, null, new BigDecimal("-1"), true));
        assertThrows(IllegalArgumentException.class,
            () -> new Descuento(1L, 1L, "N", 1, null, new BigDecimal("100.01"), true));
        assertThrows(IllegalArgumentException.class,
            () -> new Descuento(1L, 1L, "N", 1, null, new BigDecimal("1.234"), true));
        assertDoesNotThrow(() -> new Descuento(1L, 1L, "N", 1, 1, new BigDecimal("100"), true));
    }

    @Test
    void descuentoModificaYCambiaDeNombre() {
        Descuento d = descuento(5L, 2, 10);
        d.modificar(3, null, BigDecimal.ONE);
        d.setNombre("  Nuevo ");

        assertEquals(3, d.getCantidadMinima());
        assertNull(d.getCantidadMaxima());
        assertEquals("Nuevo", d.getNombre());
    }

    @Test
    void detectaTraslapesEntreEscalones() {
        Descuento a = descuento(1L, 1, 5);
        assertTrue(a.seTraslapaCon(descuento(2L, 5, 10)));
        assertFalse(a.seTraslapaCon(descuento(3L, 6, 10)));
        assertTrue(a.seTraslapaCon(descuento(4L, 3, null)));
        assertFalse(a.seTraslapaCon(descuento(5L, 6, null)));
        assertTrue(descuento(6L, 10, null).seTraslapaCon(descuento(7L, 1, 12)));
        assertFalse(descuento(6L, 10, null).seTraslapaCon(descuento(7L, 1, 9)));
        assertTrue(descuento(8L, 1, null).seTraslapaCon(descuento(9L, 4, null)));
    }

    // ---------- Producto ----------

    @Test
    void productoValidaNombrePrecioYCategoria() {
        Producto p = new Producto(1L, 2L, "  Taza ", new BigDecimal("10.00"), true);
        assertEquals(1L, p.getId());
        assertEquals(2L, p.getCategoriaId());
        assertEquals("Taza", p.getNombre());
        assertEquals(new BigDecimal("10.00"), p.getPrecio());
        assertNull(p.getImagenUrl());
        assertNull(p.getImagenPublicId());
        assertTrue(p.isActivo());
        p.desactivar();
        assertFalse(p.isActivo());

        BigDecimal ok = BigDecimal.ONE;
        assertThrows(IllegalArgumentException.class, () -> new Producto(1L, null, "N", ok, true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(1L, 0L, "N", ok, true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(1L, 1L, null, ok, true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(1L, 1L, " ", ok, true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(1L, 1L, "x".repeat(151), ok, true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(1L, 1L, "N", null, true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(1L, 1L, "N", new BigDecimal("-1"), true));
        assertThrows(IllegalArgumentException.class, () -> new Producto(1L, 1L, "N", new BigDecimal("1.234"), true));
        assertThrows(IllegalArgumentException.class,
            () -> new Producto(1L, 1L, "N", new BigDecimal("10000000000.00"), true));
        assertDoesNotThrow(() -> new Producto(1L, 1L, "N", new BigDecimal("9999999999.99"), true));
    }

    @Test
    void productoValidaLaImagen() {
        Producto p = new Producto(1L, 2L, "Taza", BigDecimal.ONE, " https://res.cloudinary.com/demo/img.png ", " id ", true);
        assertEquals("https://res.cloudinary.com/demo/img.png", p.getImagenUrl());
        assertEquals("id", p.getImagenPublicId());

        p.setImagenUrl(null);
        p.setImagenPublicId("  ");
        assertNull(p.getImagenUrl());
        assertNull(p.getImagenPublicId());

        for (String invalida : List.of("", "   ", "http://res.cloudinary.com/a", "https://otro.com/a",
            "https://user@res.cloudinary.com/a", "https://res.cloudinary.com", "https://res.cloudinary.com/",
            "https://res.cloudinary.com/a b", "https://res.cloudinary.com/a\u0007b", "https://res.cloudinary.com/%zz",
            "ftp:/res.cloudinary.com/a", "https://[::1/a")) {
            assertThrows(ProductoInvalidoException.class, () -> p.setImagenUrl(invalida), invalida);
        }
        assertThrows(ProductoInvalidoException.class, () -> p.setImagenPublicId("a\nb"));
    }

    // ---------- ProductoDetalle ----------

    @Test
    void detalleDeProductoConstruyeSusEscalones() {
        Producto p = new Producto(1L, 2L, "Taza", BigDecimal.ONE, true);
        Descuento d1 = descuento(2L, 1, 5);
        Descuento d2 = descuento(1L, 6, null);

        assertTrue(new ProductoDetalle(p, null).escalones().isEmpty());
        assertEquals(List.of(d1), new ProductoDetalle(p, d1).escalones());
        assertNull(new ProductoDetalle(p, d1).clasificacion());
        assertTrue(new ProductoDetalle(p, null, null, null).escalones().isEmpty());

        ProductoDetalle conEscalones = ProductoDetalle.conEscalones(p, null, List.of(d1, d2));
        assertSame(d2, conEscalones.descuento());
        assertNull(ProductoDetalle.conEscalones(p, null, List.of()).descuento());
    }

    // ---------- FiltroProductos ----------

    @Test
    void filtroNormalizaTextoYValidaPrecios() {
        assertNull(FiltroProductos.SIN_FILTROS.texto());
        assertNull(new FiltroProductos(null, null, "  ", null).texto());
        assertNull(new FiltroProductos(null, null, null, null).texto());
        assertEquals("taza", new FiltroProductos(null, null, " taza ", null).texto());
        assertDoesNotThrow(() -> new FiltroProductos(null, null, null, null, BigDecimal.ONE, BigDecimal.TEN));
        assertDoesNotThrow(() -> new FiltroProductos(null, null, null, null, BigDecimal.ONE, null));
        assertDoesNotThrow(() -> new FiltroProductos(null, null, null, null, null, BigDecimal.ONE));

        BigDecimal negativo = new BigDecimal("-1");
        assertThrows(ProductoInvalidoException.class,
            () -> new FiltroProductos(null, null, null, null, negativo, null));
        assertThrows(ProductoInvalidoException.class,
            () -> new FiltroProductos(null, null, null, null, null, negativo));
        assertThrows(ProductoInvalidoException.class,
            () -> new FiltroProductos(null, null, null, null, BigDecimal.TEN, BigDecimal.ONE));
    }

    // ---------- Registros, enum y excepciones ----------

    @Test
    void asignacionesSonInmutablesYSePuedeCrearUnaVacia() {
        AsignacionesDescuento vacia = AsignacionesDescuento.vacia();
        AsignacionesDescuento con = new AsignacionesDescuento(Set.of(1L), Set.of(2L), Set.of(3L));

        assertTrue(vacia.productoIds().isEmpty() && vacia.categoriaIds().isEmpty() && vacia.areaIds().isEmpty());
        assertEquals(Set.of(1L), con.productoIds());
        assertEquals(Set.of(2L), con.categoriaIds());
        assertEquals(Set.of(3L), con.areaIds());
        assertEquals(3, DestinoDescuento.values().length);
        assertEquals(DestinoDescuento.AREA, DestinoDescuento.valueOf("AREA"));
    }

    @Test
    void registrosSimplesExponenSusDatos() {
        Descuento d = descuento(1L, 1, null);
        ClasificacionProducto clasificacion = new ClasificacionProducto(1L, "Área", 2L, "Categoría");
        DescuentoConAsignaciones conAsignaciones = new DescuentoConAsignaciones(d, AsignacionesDescuento.vacia());
        CatalogoContrato.EscalonDescuento escalon = new CatalogoContrato.EscalonDescuento(1, null, BigDecimal.ONE);
        CatalogoContrato.ProductoPedido pedido = new CatalogoContrato.ProductoPedido(BigDecimal.TEN, List.of(escalon));

        assertEquals("Categoría", clasificacion.categoriaNombre());
        assertEquals(1L, clasificacion.areaId());
        assertEquals("Área", clasificacion.areaNombre());
        assertEquals(2L, clasificacion.categoriaId());
        assertSame(d, conAsignaciones.descuento());
        assertTrue(conAsignaciones.asignaciones().areaIds().isEmpty());
        assertEquals(1, escalon.cantidadMinima());
        assertNull(escalon.cantidadMaxima());
        assertEquals(BigDecimal.ONE, escalon.porcentaje());
        assertEquals(BigDecimal.TEN, pedido.precio());
        assertEquals(1, pedido.escalones().size());
    }

    @Test
    void excepcionesDeCatalogoTienenSuMensaje() {
        DatosCatalogoInvalidosException invalidos = new DatosCatalogoInvalidosException(Map.of("nombre", "mal"));

        assertEquals("Revisa los datos del formulario.", invalidos.getMessage());
        assertEquals("mal", invalidos.getErrores().get("nombre"));
        assertEquals("m", new CatalogoNoEncontradoException("m").getMessage());
        assertEquals("No se encontró la categoría.", new CategoriaNoEncontradaException().getMessage());
        assertEquals("c", new ClasificacionConProductosException("c").getMessage());
        assertEquals("d", new DescuentoInvalidoException("d").getMessage());
        assertEquals("t", new DescuentoTraslapadoException("t").getMessage());
        assertEquals("DESCUENTO_TRASLAPADO", DescuentoTraslapadoException.CODIGO);
        assertEquals("No se encontró el negocio.", new NegocioNoEncontradoException().getMessage());
        assertEquals("p", new ProductoInvalidoException("p").getMessage());
        assertEquals("No se encontró el producto.", new ProductoNoEncontradoException().getMessage());
    }

    // ---------- Métodos por defecto de los puertos ----------

    @Test
    void negocioPortPuedeConsultarPorPropietarioOPorSucursal() {
        NegocioPort puerto = mock(NegocioPort.class, CALLS_REAL_METHODS);
        when(puerto.perteneceAUsuario(1L, 7L)).thenReturn(true);
        when(puerto.perteneceAUsuario(2L, 7L)).thenReturn(false);

        assertTrue(puerto.puedeConsultar(1L, null, 7L));
        assertFalse(puerto.puedeConsultar(2L, null, 7L));
        assertFalse(puerto.puedeConsultar(2L, 9L, 7L));
        assertTrue(puerto.negocioDeSucursal(9L, 7L).isEmpty());

        NegocioPort conSucursal = new NegocioPort() {
            @Override
            public boolean perteneceAUsuario(Long negocioId, Long usuarioId) {
                return false;
            }

            @Override
            public Optional<Long> negocioDeSucursal(Long sucursalId, Long usuarioId) {
                return Optional.of(2L);
            }
        };
        assertTrue(conSucursal.puedeConsultar(2L, 9L, 7L));
        assertFalse(conSucursal.puedeConsultar(3L, 9L, 7L));
    }

    @Test
    void productoRepositorioPortArmaElDetalleConClasificacion() {
        ProductoRepositorioPort puerto = mock(ProductoRepositorioPort.class, CALLS_REAL_METHODS);
        Producto p = new Producto(1L, 2L, "Taza", BigDecimal.ONE, true);
        ClasificacionProducto clasificacion = new ClasificacionProducto(1L, "A", 2L, "C");
        when(puerto.buscarPorId(1L, 7L)).thenReturn(Optional.of(p));
        when(puerto.buscarPorId(9L, 7L)).thenReturn(Optional.empty());
        when(puerto.buscarClasificacion(2L)).thenReturn(Optional.of(clasificacion));

        assertSame(clasificacion, puerto.buscarConClasificacion(1L, 7L).orElseThrow().clasificacion());
        assertTrue(puerto.buscarConClasificacion(9L, 7L).isEmpty());
        assertTrue(puerto.buscarConClasificacionEnNegocio(1L, 5L).isEmpty());

        when(puerto.buscarClasificacion(any())).thenReturn(Optional.empty());
        assertNull(puerto.buscarConClasificacion(1L, 7L).orElseThrow().clasificacion());
    }
}
