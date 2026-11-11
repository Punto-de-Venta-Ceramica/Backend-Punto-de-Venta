package com.iaml.puntoventaapi.caja.api;

import com.iaml.puntoventaapi.caja.api.dto.*;
import com.iaml.puntoventaapi.caja.application.CorteCajaService;
import com.iaml.puntoventaapi.caja.domain.exception.CorteCajaNoAbiertoException;
import com.iaml.puntoventaapi.caja.domain.exception.CorteCajaYaAbiertoException;
import com.iaml.puntoventaapi.shared.api.ManejadorGlobalExcepciones;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CorteCajaControllerTest {

    private CorteCajaService service;
    private UsuarioActual usuarioActual;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CorteCajaService.class);
        usuarioActual = mock(UsuarioActual.class);
        when(usuarioActual.id()).thenReturn(1L);

        mvc = MockMvcBuilders.standaloneSetup(new CorteCajaController(service, usuarioActual))
            .setControllerAdvice(new CorteCajaExceptionHandler(), new ManejadorGlobalExcepciones())
            .build();
    }

    private CorteCajaResponse respuestaEjemplo(boolean cerrado) {
        return new CorteCajaResponse(
            10L,
            1L,
            2L,
            "Sucursal Principal",
            1L,
            "Juan Empleado",
            Instant.now(),
            cerrado ? Instant.now() : null,
            cerrado,
            new BigDecimal("500.00"),
            new BigDecimal("1200.00"),
            new BigDecimal("200.00"),
            new BigDecimal("150.00"),
            new BigDecimal("100.00"),
            new DesgloseMetodoPagoDto(
                new BigDecimal("800.00"),
                new BigDecimal("150.00"),
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("950.00"),
                new BigDecimal("500.00"),
                new BigDecimal("1450.00")
            ),
            new DesgloseMetodoPagoDto(
                new BigDecimal("400.00"),
                new BigDecimal("50.00"),
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                new BigDecimal("500.00")
            ),
            new SeccionCancelacionesDto(
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                List.of(new CancelacionPedidoDto(100L, "PED-01", new BigDecimal("100.00"), "EFECTIVO", "ANTICIPO", Instant.now()))
            ),
            new BigDecimal("1450.00"),
            new BigDecimal("500.00"),
            new BigDecimal("1950.00"),
            cerrado ? new BigDecimal("1950.00") : null,
            cerrado ? BigDecimal.ZERO.setScale(2) : null,
            List.of(new ProductoVendidoDto(5L, "Jarrón Barro", 4, new BigDecimal("300.00"), new BigDecimal("1200.00")))
        );
    }

    @Test
    @DisplayName("POST /api/cortes-caja/abrir responde 201 Created con resumen inicial")
    void abrirCorteResponde201() throws Exception {
        when(service.abrirCorte(any(), eq(1L))).thenReturn(respuestaEjemplo(false));

        mvc.perform(post("/api/cortes-caja/abrir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "sucursalId": 2,
                        "montoInicial": 500.00
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.montoInicial").value(500.00))
            .andExpect(jsonPath("$.cerrado").value(false));

        verify(service).abrirCorte(any(), eq(1L));
    }

    @Test
    @DisplayName("POST /api/cortes-caja/abrir valida montoInicial no negativo")
    void abrirCorteRechazaMontoNegativo() throws Exception {
        mvc.perform(post("/api/cortes-caja/abrir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "montoInicial": -50.00
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /api/cortes-caja/abrir maneja conflicto si ya está abierto")
    void abrirCorteManejaConflicto() throws Exception {
        when(service.abrirCorte(any(), eq(1L)))
            .thenThrow(new CorteCajaYaAbiertoException("Ya existe un corte de caja abierto para esta sucursal."));

        mvc.perform(post("/api/cortes-caja/abrir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "montoInicial": 100.00
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("CORTE_CAJA_YA_ABIERTO"));
    }

    @Test
    @DisplayName("GET /api/cortes-caja/activo responde 200 con el corte activo")
    void obtenerCorteActivoResponde200() throws Exception {
        when(service.obtenerCorteActivo(eq(2L), eq(1L))).thenReturn(respuestaEjemplo(false));

        mvc.perform(get("/api/cortes-caja/activo?sucursalId=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.totalVentas").value(1200.00))
            .andExpect(jsonPath("$.ventasEfectivo.totalVentas").value(800.00))
            .andExpect(jsonPath("$.ventasTarjeta.totalVentas").value(400.00))
            .andExpect(jsonPath("$.cancelacionesPedidos.totalCancelaciones").value(100.00))
            .andExpect(jsonPath("$.productos[0].nombre").value("Jarrón Barro"));
    }

    @Test
    @DisplayName("GET /api/cortes-caja/activo responde 404 si no hay corte abierto")
    void obtenerCorteActivoResponde404SiNoExiste() throws Exception {
        when(service.obtenerCorteActivo(any(), eq(1L)))
            .thenThrow(new CorteCajaNoAbiertoException("No hay un corte de caja abierto actualmente en esta sucursal."));

        mvc.perform(get("/api/cortes-caja/activo"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("CORTE_CAJA_NO_ABIERTO"));
    }

    @Test
    @DisplayName("POST /api/cortes-caja/{id}/cerrar responde 200 con corte cerrado y arqueo")
    void cerrarCorteResponde200() throws Exception {
        when(service.cerrarCorte(eq(10L), any(), eq(1L))).thenReturn(respuestaEjemplo(true));

        mvc.perform(post("/api/cortes-caja/10/cerrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "totalReal": 1950.00
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.cerrado").value(true))
            .andExpect(jsonPath("$.totalReal").value(1950.00))
            .andExpect(jsonPath("$.diferencia").value(0.00));
    }

    @Test
    @DisplayName("GET /api/cortes-caja responde 200 con lista de historial")
    void listarHistorialResponde200() throws Exception {
        var resumen = new ResumenCorteCajaDto(
            10L, 1L, 2L, "Sucursal Principal", 1L, "Juan",
            Instant.now(), Instant.now(), true,
            new BigDecimal("500.00"), new BigDecimal("1200.00"),
            new BigDecimal("1700.00"), new BigDecimal("1700.00"), BigDecimal.ZERO
        );

        when(service.listarHistorial(eq(2L), eq(1L))).thenReturn(List.of(resumen));

        mvc.perform(get("/api/cortes-caja?sucursalId=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10))
            .andExpect(jsonPath("$[0].cerrado").value(true));
    }
}

