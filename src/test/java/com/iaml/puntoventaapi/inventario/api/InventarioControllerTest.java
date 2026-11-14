package com.iaml.puntoventaapi.inventario.api;

import com.iaml.puntoventaapi.inventario.api.dto.AjustarExistenciaRequest;
import com.iaml.puntoventaapi.inventario.api.dto.AsignarProductoRequest;
import com.iaml.puntoventaapi.inventario.api.dto.ExistenciaResponse;
import com.iaml.puntoventaapi.inventario.api.dto.InventarioPanelResponse;
import com.iaml.puntoventaapi.inventario.application.InventarioService;
import com.iaml.puntoventaapi.inventario.domain.ExistenciaProducto;
import com.iaml.puntoventaapi.inventario.domain.Inventario;
import com.iaml.puntoventaapi.inventario.domain.exception.ExistenciaInsuficienteException;
import com.iaml.puntoventaapi.inventario.domain.exception.InventarioNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.exception.InventarioYaExisteException;
import com.iaml.puntoventaapi.inventario.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.exception.ProductoOSucursalNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.exception.UltimaSucursalException;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import com.iaml.puntoventaapi.shared.security.UsuarioNoAutenticadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventarioControllerTest {

    private static final ExistenciaProducto FILA = new ExistenciaProducto(1L, 2L, "Taza", 3L, "Centro", 9);

    private InventarioService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(InventarioService.class);
        UsuarioActual usuario = mock(UsuarioActual.class);
        when(usuario.id()).thenReturn(7L);
        mvc = MockMvcBuilders.standaloneSetup(new InventarioController(service, usuario))
            .setControllerAdvice(new InventarioExceptionHandler()).build();
    }

    @Test
    void panelDelNegocioYPorProducto() throws Exception {
        when(service.consultarPanel(5L, null, null, 7L)).thenReturn(List.of(FILA));
        when(service.consultarPorProducto(2L, 7L)).thenReturn(List.of(FILA));

        mvc.perform(get("/api/inventario").param("negocioId", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].productoNombre").value("Taza"))
            .andExpect(jsonPath("$[0].existencia").value(9));
        mvc.perform(get("/api/inventario").param("productoId", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sucursalNombre").value("Centro"));
    }

    @Test
    void asignarDevuelve201() throws Exception {
        when(service.asignar(2L, 3L, 4, 7L)).thenReturn(new Inventario(1L, 3L, 2L, 4));

        mvc.perform(post("/api/inventario").contentType("application/json")
                .content("{\"productoId\":2,\"sucursalId\":3,\"existencia\":4}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.existencia").value(4));
        mvc.perform(post("/api/inventario").contentType("application/json").content("{\"sucursalId\":3}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void quitarDevuelve204() throws Exception {
        mvc.perform(delete("/api/inventario/4")).andExpect(status().isNoContent());

        verify(service).quitar(4L, 7L);
    }

    @Test
    void ajustarDevuelveLaNuevaExistenciaYValidaLaCantidad() throws Exception {
        when(service.ajustar(4L, -2, 7L)).thenReturn(new Inventario(4L, 3L, 2L, 1));

        mvc.perform(patch("/api/inventario/4/existencia").contentType("application/json")
                .content("{\"cantidad\":-2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.existencia").value(1));
        mvc.perform(patch("/api/inventario/4/existencia").contentType("application/json")
                .content("{\"cantidad\":0}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void traduceLasExcepcionesDeDominio() throws Exception {
        doThrow(new InventarioNoEncontradoException()).when(service).quitar(1L, 7L);
        doThrow(new UltimaSucursalException()).when(service).quitar(2L, 7L);
        doThrow(new UsuarioNoAutenticadoException()).when(service).quitar(3L, 7L);
        when(service.consultarPanel(anyLong(), any(), any(), anyLong())).thenThrow(new NegocioNoEncontradoException());
        when(service.asignar(anyLong(), anyLong(), any(), anyLong())).thenThrow(new InventarioYaExisteException());
        when(service.ajustar(anyLong(), anyInt(), anyLong())).thenThrow(new ExistenciaInsuficienteException());
        when(service.consultarPorProducto(anyLong(), anyLong()))
            .thenThrow(new ProductoOSucursalNoEncontradoException());

        mvc.perform(delete("/api/inventario/1")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/inventario/2")).andExpect(status().isConflict());
        mvc.perform(delete("/api/inventario/3")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/inventario").param("negocioId", "5")).andExpect(status().isNotFound());
        mvc.perform(get("/api/inventario").param("productoId", "5")).andExpect(status().isNotFound());
        mvc.perform(post("/api/inventario").contentType("application/json")
                .content("{\"productoId\":2,\"sucursalId\":3}"))
            .andExpect(status().isConflict());
        mvc.perform(patch("/api/inventario/4/existencia").contentType("application/json")
                .content("{\"cantidad\":-9}"))
            .andExpect(status().isConflict());
    }

    @Test
    void dtosConvierten() {
        InventarioPanelResponse panel = InventarioPanelResponse.from(FILA);
        ExistenciaResponse existencia = ExistenciaResponse.from(new Inventario(1L, 3L, 2L, 5));

        assertEquals("Taza", panel.productoNombre());
        assertEquals(3L, existencia.sucursalId());
        assertEquals(2L, existencia.productoId());
        assertEquals(1L, existencia.id());
    }

    @Test
    void laValidacionDelAjusteAceptaNuloYRechazaCero() {
        assertTrue(new AjustarExistenciaRequest(null).isCantidadDistintaDeCero());
        assertTrue(new AjustarExistenciaRequest(3).isCantidadDistintaDeCero());
        assertFalse(new AjustarExistenciaRequest(0).isCantidadDistintaDeCero());
        assertEquals(null, new AsignarProductoRequest(1L, 2L, null).existencia());
    }
}
