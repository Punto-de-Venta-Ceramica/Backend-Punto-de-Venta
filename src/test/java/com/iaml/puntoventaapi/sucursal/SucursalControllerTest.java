package com.iaml.puntoventaapi.sucursal;

import com.iaml.puntoventaapi.shared.api.ManejadorGlobalExcepciones;
import com.iaml.puntoventaapi.sucursal.api.SucursalController;
import com.iaml.puntoventaapi.sucursal.application.EditarSucursalCommand;
import com.iaml.puntoventaapi.sucursal.application.SucursalService;
import com.iaml.puntoventaapi.sucursal.domain.Sucursal;
import com.iaml.puntoventaapi.sucursal.domain.exception.SucursalConDependenciasException;
import com.iaml.puntoventaapi.sucursal.domain.exception.SucursalNoEncontradaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SucursalControllerTest {

    private SucursalService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        service = mock(SucursalService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SucursalController(service))
            .setControllerAdvice(new ManejadorGlobalExcepciones()).build();
    }

    @Test
    void consultaPostPermiteNavegadoresYConservaValidacionDelBody() throws Exception {
        when(service.consultarPorNegocio(2L)).thenReturn(List.of(
            new Sucursal(5L, 2L, "Centro", null, null, null, null)));
        mvc.perform(post("/api/sucursales/consultar").contentType("application/json")
                .content("{\"negocioId\":2}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(5));
        verify(service).consultarPorNegocio(2L);
        mvc.perform(post("/api/sucursales/consultar").contentType("application/json"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/sucursales/consultar").contentType("application/json").content("{\"negocioId\":0}"))
            .andExpect(status().isBadRequest());
        verifyNoMoreInteractions(service);
    }

    @Test
    void crearDevuelve201YUnDtoSinExponerRelacionesJpa() throws Exception {
        when(service.crear(any())).thenReturn(new Sucursal(5L, 2L, "Centro", null, null, null, null));
        mvc.perform(post("/api/sucursales").contentType("application/json")
                .content("{\"negocioId\":2,\"nombre\":\"Centro\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.negocioId").value(2))
            .andExpect(jsonPath("$.negocio").doesNotExist())
            .andExpect(jsonPath("$.areas").doesNotExist());
    }

    @Test
    void crearValidaIdsYNombreAntesDeLlamarAlServicio() throws Exception {
        mvc.perform(post("/api/sucursales").contentType("application/json")
                .content("{\"negocioId\":0,\"nombre\":\" \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.negocioId").exists())
            .andExpect(jsonPath("$.errores.nombre").exists());
        verifyNoInteractions(service);
    }

    @Test
    void patchConUnSoloCampoEditableUsaElIdDelBody() throws Exception {
        when(service.editar(any())).thenReturn(new Sucursal(5L, 2L, "Norte", "Anterior", null, null, null));
        mvc.perform(patch("/api/sucursales").contentType("application/json")
                .content("{\"id\":5,\"nombre\":\"Norte\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.descripcion").value("Anterior"));
        verify(service).editar(new EditarSucursalCommand(5L, "Norte", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"id\":5}", "{\"id\":5,\"nombre\":\" \"}", "{\"id\":5,\"nombre\":\"\"}",
        "{\"id\":0,\"nombre\":\"Norte\"}", "{\"nombre\":\"Norte\"}",
        "{\"id\":5,\"nombre\":null,\"descripcion\":null}"
    })
    void patchRechazaCuerposInvalidos(String body) throws Exception {
        mvc.perform(patch("/api/sucursales").contentType("application/json").content(body))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"negocioId", "ciudad", "calle", "codigoPostal", "areas"})
    void patchRechazaCamposNoEditables(String campo) throws Exception {
        mvc.perform(patch("/api/sucursales").contentType("application/json")
                .content("{\"id\":5,\"nombre\":\"Norte\",\"" + campo + "\":\"otro\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void eliminarRecibeElIdEnElBodyYDevuelve204() throws Exception {
        mvc.perform(delete("/api/sucursales").contentType("application/json").content("{\"id\":5}"))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).eliminar(5L);
    }

    @Test
    void erroresDeDominioSeTraducenA404Y409() throws Exception {
        doThrow(new SucursalNoEncontradaException(99L)).when(service).eliminar(99L);
        mvc.perform(delete("/api/sucursales").contentType("application/json").content("{\"id\":99}"))
            .andExpect(status().isNotFound());
        doThrow(new SucursalConDependenciasException(new RuntimeException("detalle interno")))
            .when(service).eliminar(5L);
        mvc.perform(delete("/api/sucursales").contentType("application/json").content("{\"id\":5}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("No se puede eliminar la sucursal porque tiene registros relacionados."));
    }

    @Test
    void eliminacionExigeBodyConIdValido() throws Exception {
        mvc.perform(delete("/api/sucursales").contentType("application/json").content("{\"id\":-1}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.id").exists());
        verifyNoInteractions(service);
    }

    @Test
    void getConsultaElNegocioRecibidoEnElBody() throws Exception {
        when(service.consultarPorNegocio(2L)).thenReturn(
            List.of(new Sucursal(5L, 2L, "Centro", null, null, null, null)));
        mvc.perform(get("/api/sucursales").contentType("application/json").content("{\"negocioId\":2}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].negocioId").value(2))
            .andExpect(jsonPath("$[0].nombre").value("Centro"));
        verify(service).consultarPorNegocio(2L);
    }
}

