package com.iaml.puntoventaapi.catalogo.api;

import com.iaml.puntoventaapi.catalogo.api.dto.request.CrearProductoRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.request.DescuentoProductoRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.request.EditarProductoRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.request.ExistenciaInicialRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.request.GuardarAreaRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.request.GuardarCategoriaRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.request.GuardarDescuentoRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.request.GuardarDescuentosProductoRequest;
import com.iaml.puntoventaapi.catalogo.api.dto.response.PaginaResponse;
import com.iaml.puntoventaapi.catalogo.api.dto.response.ProductoResponse;
import com.iaml.puntoventaapi.catalogo.application.CrearProductoCommand;
import com.iaml.puntoventaapi.catalogo.application.EditarProductoCommand;
import com.iaml.puntoventaapi.catalogo.application.LineaDescuentoCommand;
import com.iaml.puntoventaapi.catalogo.domain.ClasificacionProducto;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.ProductoDetalle;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtosCatalogoTest {

    private static final DescuentoProductoRequest DESCUENTO =
        new DescuentoProductoRequest(1, 5, new BigDecimal("10"));

    @Test
    void crearProductoConvierteACommandConYSinOpcionales() {
        CrearProductoRequest completo = new CrearProductoRequest("Taza", BigDecimal.TEN, 1L, "https://x", "pid",
            DESCUENTO, List.of(new ExistenciaInicialRequest(2L, 3)));
        CrearProductoCommand command = completo.toCommand();

        assertEquals("pid", command.imagenPublicId());
        assertEquals(1, command.descuento().cantidadMinima());
        assertEquals(3, command.existencias().get(0).existencia());
        assertEquals(2L, command.existencias().get(0).sucursalId());

        CrearProductoCommand minimo = new CrearProductoRequest("Taza", BigDecimal.TEN, 1L, null, null).toCommand();
        assertNull(minimo.descuento());
        assertNull(minimo.existencias());
        assertNull(minimo.imagenUrl());
    }

    @Test
    void editarProductoConvierteACommand() {
        EditarProductoCommand conDescuento = new EditarProductoRequest("T", BigDecimal.ONE, 1L, "u", "p", DESCUENTO)
            .toCommand(9L);
        EditarProductoCommand sinDescuento = new EditarProductoRequest("T", BigDecimal.ONE, 1L, null).toCommand(9L);

        assertEquals(9L, conDescuento.id());
        assertEquals("u", conDescuento.imagenUrl());
        assertNotNull(conDescuento.descuento());
        assertNull(sinDescuento.descuento());
        assertNull(sinDescuento.imagenPublicId());
    }

    @Test
    void validacionDeRangoDeLosDescuentos() {
        assertTrue(new DescuentoProductoRequest(null, 1, BigDecimal.ONE).isRangoValido());
        assertTrue(new DescuentoProductoRequest(1, null, BigDecimal.ONE).isRangoValido());
        assertTrue(new DescuentoProductoRequest(1, 1, BigDecimal.ONE).isRangoValido());
        assertFalse(new DescuentoProductoRequest(5, 1, BigDecimal.ONE).isRangoValido());
        assertEquals(5, DESCUENTO.toCommand().cantidadMaxima());
        assertEquals(new BigDecimal("10"), DESCUENTO.toCommand().porcentaje());

        assertTrue(new GuardarDescuentoRequest("N", null, 1, BigDecimal.ONE).isRangoValido());
        assertTrue(new GuardarDescuentoRequest("N", 1, null, BigDecimal.ONE).isRangoValido());
        assertTrue(new GuardarDescuentoRequest("N", 1, 2, BigDecimal.ONE).isRangoValido());
        assertFalse(new GuardarDescuentoRequest("N", 3, 2, BigDecimal.ONE).isRangoValido());
        assertEquals("N", new GuardarDescuentoRequest("N", 1, 2, BigDecimal.ONE).toCommand().nombre());

        GuardarDescuentosProductoRequest.Linea baja = new GuardarDescuentosProductoRequest.Linea(
            null, "N", 3, 2, BigDecimal.ONE, null);
        assertFalse(baja.isRangoValido());
        assertTrue(new GuardarDescuentosProductoRequest.Linea(null, "N", null, 2, BigDecimal.ONE, null).isRangoValido());
        assertTrue(new GuardarDescuentosProductoRequest.Linea(null, "N", 1, null, BigDecimal.ONE, null).isRangoValido());
        assertTrue(new GuardarDescuentosProductoRequest.Linea(null, "N", 1, 1, BigDecimal.ONE, null).isRangoValido());
    }

    @Test
    void guardarDescuentosDeProductoConvierteLineas() {
        GuardarDescuentosProductoRequest vacio = new GuardarDescuentosProductoRequest(null, null);
        assertTrue(vacio.idsAEliminar().isEmpty());
        assertTrue(vacio.toCommands().isEmpty());

        GuardarDescuentosProductoRequest request = new GuardarDescuentosProductoRequest(List.of(4L), List.of(
            new GuardarDescuentosProductoRequest.Linea(1L, "A", 1, 2, BigDecimal.ONE, null),
            new GuardarDescuentosProductoRequest.Linea(null, "B", 3, null, BigDecimal.TEN, false),
            new GuardarDescuentosProductoRequest.Linea(null, "C", 3, null, BigDecimal.TEN, true)));
        List<LineaDescuentoCommand> lineas = request.toCommands();

        assertEquals(List.of(4L), request.idsAEliminar());
        assertTrue(lineas.get(0).activo());   // activo nulo => activa
        assertFalse(lineas.get(1).activo());
        assertTrue(lineas.get(2).activo());
        assertEquals(1L, lineas.get(0).id());
    }

    @Test
    void requestsSimplesConvierten() {
        assertEquals("N", new GuardarAreaRequest("N", "d").toCommand().nombre());
        assertEquals("d", new GuardarAreaRequest("N", "d").toCommand().descripcion());
        assertEquals("N", new GuardarCategoriaRequest("N", "d").toCommand().nombre());
        assertEquals("d", new GuardarCategoriaRequest("N", "d").toCommand().descripcion());
        assertEquals(4, new ExistenciaInicialRequest(1L, 4).toCommand().existencia());
    }

    @Test
    void respuestasDeProductoYPagina() {
        Producto producto = new Producto(5L, 1L, "Taza", BigDecimal.TEN, true);
        Descuento escalon = new Descuento(2L, 1L, "E", 1, 5, BigDecimal.ONE, true);

        ProductoResponse sinClasificacion = ProductoResponse.from(new ProductoDetalle(producto, null));
        assertNull(sinClasificacion.categoriaNombre());
        assertNull(sinClasificacion.areaId());
        assertNull(sinClasificacion.areaNombre());
        assertTrue(sinClasificacion.descuentos().isEmpty());

        ProductoResponse completa = ProductoResponse.from(new ProductoDetalle(producto, escalon,
            new ClasificacionProducto(1L, "Área", 2L, "Cat")));
        assertEquals("Cat", completa.categoriaNombre());
        assertEquals("Área", completa.areaNombre());
        assertEquals(1L, completa.areaId());
        assertEquals(5, completa.descuentos().get(0).cantidadMaxima());
        assertEquals(1, completa.descuentos().get(0).cantidadMinima());
        assertEquals(BigDecimal.ONE, completa.descuentos().get(0).porcentaje());
        assertNull(completa.imagenUrl());
        assertNull(completa.imagenPublicId());

        PaginaResponse<String> pagina = PaginaResponse.from(new Pagina<>(List.of("a"), 0, 10, 1));
        assertTrue(pagina.ultima());
        assertEquals(1, pagina.totalElementos());
        assertEquals(0, pagina.pagina());
        assertEquals(List.of("a"), pagina.contenido());
    }
}
