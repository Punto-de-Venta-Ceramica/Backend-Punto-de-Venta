package com.iaml.puntoventaapi.catalogo.api;

import com.iaml.puntoventaapi.catalogo.application.AreaService;
import com.iaml.puntoventaapi.catalogo.application.CategoriaService;
import com.iaml.puntoventaapi.catalogo.application.DescuentoService;
import com.iaml.puntoventaapi.catalogo.application.ProductoService;
import com.iaml.puntoventaapi.catalogo.domain.Area;
import com.iaml.puntoventaapi.catalogo.domain.AsignacionesDescuento;
import com.iaml.puntoventaapi.catalogo.domain.Categoria;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.DescuentoConAsignaciones;
import com.iaml.puntoventaapi.catalogo.domain.DestinoDescuento;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.ProductoDetalle;
import com.iaml.puntoventaapi.catalogo.domain.ClasificacionProducto;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.CategoriaNoEncontradaException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ClasificacionConProductosException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DatosCatalogoInvalidosException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DescuentoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DescuentoTraslapadoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoNoEncontradoException;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import com.iaml.puntoventaapi.shared.security.UsuarioNoAutenticadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControladoresCatalogoTest {

    private static final String JSON = "application/json";

    private UsuarioActual usuario;
    private AreaService areaService;
    private CategoriaService categoriaService;
    private DescuentoService descuentoService;
    private ProductoService productoService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        usuario = mock(UsuarioActual.class);
        when(usuario.id()).thenReturn(7L);
        areaService = mock(AreaService.class);
        categoriaService = mock(CategoriaService.class);
        descuentoService = mock(DescuentoService.class);
        productoService = mock(ProductoService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new AreaController(areaService, usuario),
                new CategoriaController(categoriaService, usuario),
                new CategoriasNegocioController(categoriaService, usuario),
                new DescuentoController(descuentoService, usuario),
                new ProductoController(productoService, usuario))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setControllerAdvice(new CatalogoExceptionHandler()).build();
    }

    private static DescuentoConAsignaciones descuento() {
        return new DescuentoConAsignaciones(new Descuento(3L, 1L, "Mayoreo", 5, 10, BigDecimal.TEN, true),
            new AsignacionesDescuento(Set.of(2L, 1L), Set.of(4L), Set.of(5L)));
    }

    // ---------- Áreas ----------

    @Test
    void areas() throws Exception {
        Area area = new Area(2L, 1L, "Cocina", "d");
        when(areaService.crear(eq(1L), any(), eq(7L))).thenReturn(area);
        when(areaService.listar(1L, 7L, null)).thenReturn(List.of(area));
        when(areaService.consultar(1L, 2L, 7L)).thenReturn(area);
        when(areaService.editar(eq(1L), eq(2L), any(), eq(7L))).thenReturn(area);

        mvc.perform(post("/api/negocios/1/areas").contentType(JSON).content("{\"nombre\":\"Cocina\"}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/negocios/1/areas/2"));
        mvc.perform(get("/api/negocios/1/areas")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("Cocina"));
        mvc.perform(get("/api/negocios/1/areas/2")).andExpect(status().isOk());
        mvc.perform(put("/api/negocios/1/areas/2").contentType(JSON).content("{\"nombre\":\"Cocina\"}"))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/negocios/1/areas/2")).andExpect(status().isNoContent());
        mvc.perform(post("/api/negocios/1/areas").contentType(JSON).content("{\"nombre\":\" \"}"))
            .andExpect(status().isBadRequest());
        verify(areaService).eliminar(1L, 2L, 7L);
    }

    // ---------- Categorías ----------

    @Test
    void categorias() throws Exception {
        Categoria categoria = new Categoria(3L, 2L, "Café", null);
        when(categoriaService.crear(eq(1L), eq(2L), any(), eq(7L))).thenReturn(categoria);
        when(categoriaService.listar(1L, 2L, 7L, null)).thenReturn(List.of(categoria));
        when(categoriaService.consultar(1L, 2L, 3L, 7L)).thenReturn(categoria);
        when(categoriaService.editar(eq(1L), eq(2L), eq(3L), any(), eq(7L))).thenReturn(categoria);
        when(categoriaService.listarPorNegocio(1L, 7L, null)).thenReturn(List.of(categoria));

        mvc.perform(post("/api/negocios/1/areas/2/categorias").contentType(JSON).content("{\"nombre\":\"Café\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/negocios/1/areas/2/categorias/3"));
        mvc.perform(get("/api/negocios/1/areas/2/categorias")).andExpect(status().isOk());
        mvc.perform(get("/api/negocios/1/areas/2/categorias/3")).andExpect(status().isOk());
        mvc.perform(put("/api/negocios/1/areas/2/categorias/3").contentType(JSON).content("{\"nombre\":\"Café\"}"))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/negocios/1/areas/2/categorias/3")).andExpect(status().isNoContent());
        mvc.perform(get("/api/negocios/1/categorias")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].areaId").value(2));
        verify(categoriaService).eliminar(1L, 2L, 3L, 7L);
    }

    // ---------- Descuentos ----------

    @Test
    void descuentos() throws Exception {
        String cuerpo = "{\"nombre\":\"Mayoreo\",\"cantidadMinima\":5,\"cantidadMaxima\":10,\"porcentaje\":10}";
        when(descuentoService.crear(eq(1L), any(), eq(7L))).thenReturn(descuento());
        when(descuentoService.listar(1L, null, 7L)).thenReturn(List.of(descuento()));
        when(descuentoService.consultar(1L, 3L, 7L)).thenReturn(descuento());
        when(descuentoService.editar(eq(1L), eq(3L), any(), eq(7L))).thenReturn(descuento());
        when(descuentoService.cambiarEstado(anyLong(), anyLong(), anyBoolean(), anyLong())).thenReturn(descuento());
        when(descuentoService.asignar(anyLong(), anyLong(), any(), anyLong(), anyLong())).thenReturn(descuento());
        when(descuentoService.quitar(anyLong(), anyLong(), any(), anyLong(), anyLong())).thenReturn(descuento());
        when(descuentoService.guardarDeProducto(eq(1L), eq(8L), anyCollection(), any(), eq(7L)))
            .thenReturn(List.of(descuento()));

        mvc.perform(post("/api/negocios/1/descuentos").contentType(JSON).content(cuerpo))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/negocios/1/descuentos/3"))
            .andExpect(jsonPath("$.productoIds[0]").value(1));
        mvc.perform(get("/api/negocios/1/descuentos")).andExpect(status().isOk());
        mvc.perform(get("/api/negocios/1/descuentos/3")).andExpect(status().isOk());
        mvc.perform(put("/api/negocios/1/descuentos/3").contentType(JSON).content(cuerpo)).andExpect(status().isOk());
        mvc.perform(delete("/api/negocios/1/descuentos/3")).andExpect(status().isNoContent());
        mvc.perform(patch("/api/negocios/1/descuentos/3/activar")).andExpect(status().isOk());
        mvc.perform(patch("/api/negocios/1/descuentos/3/desactivar")).andExpect(status().isOk());
        for (String destino : List.of("productos/8", "categorias/8", "areas/8")) {
            mvc.perform(put("/api/negocios/1/descuentos/3/" + destino)).andExpect(status().isOk());
            mvc.perform(delete("/api/negocios/1/descuentos/3/" + destino)).andExpect(status().isOk());
        }
        mvc.perform(put("/api/negocios/1/descuentos/productos/8").contentType(JSON)
                .content("{\"eliminar\":[1],\"lineas\":[{\"nombre\":\"A\",\"cantidadMinima\":1,\"porcentaje\":5}]}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/negocios/1/descuentos").contentType(JSON)
                .content("{\"nombre\":\"A\",\"cantidadMinima\":9,\"cantidadMaxima\":2,\"porcentaje\":5}"))
            .andExpect(status().isBadRequest());

        verify(descuentoService).eliminar(1L, 3L, 7L);
        verify(descuentoService).asignar(1L, 3L, DestinoDescuento.AREA, 8L, 7L);
        verify(descuentoService).quitar(1L, 3L, DestinoDescuento.CATEGORIA, 8L, 7L);
    }

    // ---------- Productos ----------

    @Test
    void productos() throws Exception {
        Producto producto = new Producto(5L, 1L, "Taza", BigDecimal.TEN, true);
        ProductoDetalle detalle = new ProductoDetalle(producto, null, new ClasificacionProducto(1L, "A", 1L, "C"));
        when(productoService.crear(any(), eq(7L))).thenReturn(detalle);
        when(productoService.consultar(5L, 7L, null)).thenReturn(detalle);
        when(productoService.editar(any(), eq(7L))).thenReturn(detalle);
        when(productoService.catalogo(1L, 7L)).thenReturn(List.of(producto));
        when(productoService.listar(eq(1L), eq(7L), any(), eq(0), eq(20)))
            .thenReturn(new Pagina<>(List.of(producto), 0, 20, 1));

        mvc.perform(post("/api/productos").contentType(JSON)
                .content("{\"nombre\":\"Taza\",\"precio\":10,\"categoriaId\":1}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/productos/5"));
        mvc.perform(get("/api/productos").param("negocioId", "1").param("buscar", "taza")
                .param("precioMinimo", "1").param("precioMaximo", "20"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].nombre").value("Taza"));
        mvc.perform(get("/api/productos/catalogo").param("negocioId", "1")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].categoriaId").value(1));
        mvc.perform(get("/api/productos/5")).andExpect(status().isOk())
            .andExpect(jsonPath("$.categoriaNombre").value("C"));
        mvc.perform(put("/api/productos/5").contentType(JSON)
                .content("{\"nombre\":\"Taza\",\"precio\":10,\"categoriaId\":1}"))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/productos/5")).andExpect(status().isNoContent());
        mvc.perform(post("/api/productos").contentType(JSON).content("{\"precio\":10}"))
            .andExpect(status().isBadRequest());
        verify(productoService).eliminar(5L, 7L);
    }

    // ---------- Manejador de excepciones ----------

    @Test
    void elManejadorTraduceCadaExcepcion() {
        CatalogoExceptionHandler handler = new CatalogoExceptionHandler();

        assertEquals(409, handler.clasificacionConProductos(new ClasificacionConProductosException("x")).getStatus());
        assertEquals(404, handler.noEncontrado(new ProductoNoEncontradoException()).getStatus());
        assertEquals(404, handler.noEncontrado(new CategoriaNoEncontradaException()).getStatus());
        assertEquals(404, handler.noEncontrado(new NegocioNoEncontradoException()).getStatus());
        assertEquals(404, handler.noEncontrado(new CatalogoNoEncontradoException("x")).getStatus());
        ProblemDetail traslape = handler.descuentoTraslapado(new DescuentoTraslapadoException("cruce"));
        assertEquals(409, traslape.getStatus());
        assertEquals(DescuentoTraslapadoException.CODIGO, traslape.getProperties().get("codigo"));
        assertEquals(400, handler.productoInvalido(new ProductoInvalidoException("x")).getStatus());
        assertEquals(400, handler.productoInvalido(new DescuentoInvalidoException("x")).getStatus());
        ProblemDetail datos = handler.datosInvalidos(new DatosCatalogoInvalidosException(Map.of("nombre", "mal")));
        assertEquals(400, datos.getStatus());
        assertNotNull(datos.getProperties().get("errores"));
        assertEquals(401, handler.usuarioNoAutenticado(new UsuarioNoAutenticadoException()).getStatus());
    }
}
