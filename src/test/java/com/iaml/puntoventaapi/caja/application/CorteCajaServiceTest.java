package com.iaml.puntoventaapi.caja.application;

import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolEntity;
import com.iaml.puntoventaapi.caja.api.dto.*;
import com.iaml.puntoventaapi.caja.domain.Caja;
import com.iaml.puntoventaapi.caja.domain.CorteCaja;
import com.iaml.puntoventaapi.caja.domain.exception.*;
import com.iaml.puntoventaapi.caja.infrastructure.persistence.CajaRepository;
import com.iaml.puntoventaapi.caja.infrastructure.persistence.CorteCajaRepository;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalEntity;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalJpaRepository;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CorteCajaServiceTest {

    private CajaRepository cajaRepository;
    private CorteCajaRepository corteCajaRepository;
    private SucursalJpaRepository sucursalJpaRepository;
    private UsuarioJpaRepository usuarioJpaRepository;
    private SucursalContrato sucursalContrato;
    private CalculadoraCorte calculadoraCorte;

    private CorteCajaService service;

    private UsuarioEntity empleado;
    private UsuarioEntity propietario;
    private SucursalEntity sucursal;
    private Caja caja;

    @BeforeEach
    void setUp() {
        cajaRepository = mock(CajaRepository.class);
        corteCajaRepository = mock(CorteCajaRepository.class);
        sucursalJpaRepository = mock(SucursalJpaRepository.class);
        usuarioJpaRepository = mock(UsuarioJpaRepository.class);
        sucursalContrato = mock(SucursalContrato.class);
        calculadoraCorte = mock(CalculadoraCorte.class);

        service = new CorteCajaService(
            cajaRepository,
            corteCajaRepository,
            sucursalJpaRepository,
            usuarioJpaRepository,
            sucursalContrato,
            calculadoraCorte
        );

        RolEntity rolEmpleado = new RolEntity();
        rolEmpleado.setNombre("EMPLEADO");

        RolEntity rolPropietario = new RolEntity();
        rolPropietario.setNombre("PROPIETARIO");

        sucursal = new SucursalEntity();
        ReflectionTestUtils.setField(sucursal, "id", 10L);
        sucursal.setNombre("Sucursal Centro");

        caja = new Caja();
        ReflectionTestUtils.setField(caja, "id", 100L);
        caja.setSucursal(sucursal);

        empleado = new UsuarioEntity();
        ReflectionTestUtils.setField(empleado, "id", 1L);
        empleado.setNombre("Empleado Juan");
        empleado.setRol(rolEmpleado);
        empleado.setSucursal(sucursal);

        propietario = new UsuarioEntity();
        ReflectionTestUtils.setField(propietario, "id", 2L);
        propietario.setNombre("Propietario Don Pedro");
        propietario.setRol(rolPropietario);

        when(cajaRepository.findBySucursal_Id(10L)).thenReturn(Optional.of(caja));
        when(calculadoraCorte.calcular(any(), any())).thenReturn(new CalculadoraCorte.ResultadoCalculo(
            BigDecimal.ZERO.setScale(2),
            BigDecimal.ZERO.setScale(2),
            BigDecimal.ZERO.setScale(2),
            BigDecimal.ZERO.setScale(2),
            new DesgloseMetodoPagoDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
            new DesgloseMetodoPagoDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
            new SeccionCancelacionesDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of()),
            BigDecimal.ZERO.setScale(2),
            BigDecimal.ZERO.setScale(2),
            BigDecimal.ZERO.setScale(2),
            List.of()
        ));
    }

    @Test
    @DisplayName("Empleado abre corte de caja en su sucursal asignada")
    void abrirCorteEmpleadoExitoso() {
        when(usuarioJpaRepository.findById(1L)).thenReturn(Optional.of(empleado));
        when(corteCajaRepository.existsByCaja_Sucursal_IdAndFechaHoraCierreIsNull(10L)).thenReturn(false);
        when(corteCajaRepository.save(any(CorteCaja.class))).thenAnswer(invocation -> {
            CorteCaja c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 500L);
            return c;
        });

        var request = new AbrirCorteRequest(null, new BigDecimal("300.00"));
        CorteCajaResponse response = service.abrirCorte(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.montoInicial()).isEqualByComparingTo("300.00");
        assertThat(response.cerrado()).isFalse();
        verify(corteCajaRepository).save(any(CorteCaja.class));
    }

    @Test
    @DisplayName("Propietario abre corte de caja especificando sucursal autorizada")
    void abrirCortePropietarioExitoso() {
        when(usuarioJpaRepository.findById(2L)).thenReturn(Optional.of(propietario));
        when(sucursalContrato.negocioAutorizado(10L, 2L)).thenReturn(Optional.of(1L));
        when(corteCajaRepository.existsByCaja_Sucursal_IdAndFechaHoraCierreIsNull(10L)).thenReturn(false);
        when(corteCajaRepository.save(any(CorteCaja.class))).thenAnswer(invocation -> {
            CorteCaja c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 501L);
            return c;
        });

        var request = new AbrirCorteRequest(10L, new BigDecimal("500.00"));
        CorteCajaResponse response = service.abrirCorte(request, 2L);

        assertThat(response.id()).isEqualTo(501L);
        assertThat(response.montoInicial()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Impide abrir corte si ya existe uno abierto en la sucursal")
    void impideAbrirCorteDuplicado() {
        when(usuarioJpaRepository.findById(1L)).thenReturn(Optional.of(empleado));
        when(corteCajaRepository.existsByCaja_Sucursal_IdAndFechaHoraCierreIsNull(10L)).thenReturn(true);

        var request = new AbrirCorteRequest(null, new BigDecimal("200.00"));

        assertThatThrownBy(() -> service.abrirCorte(request, 1L))
            .isInstanceOf(CorteCajaYaAbiertoException.class)
            .hasMessageContaining("Ya existe un corte de caja abierto");
    }

    @Test
    @DisplayName("Empleado no puede acceder a una sucursal no asignada")
    void empleadoRechazadoEnOtraSucursal() {
        when(usuarioJpaRepository.findById(1L)).thenReturn(Optional.of(empleado));

        var request = new AbrirCorteRequest(999L, new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.abrirCorte(request, 1L))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("No tienes permiso");
    }

    @Test
    @DisplayName("Cerrar corte calcula totales y registra total real")
    void cerrarCorteExitoso() {
        when(usuarioJpaRepository.findById(1L)).thenReturn(Optional.of(empleado));

        CorteCaja corteAbierto = new CorteCaja();
        ReflectionTestUtils.setField(corteAbierto, "id", 500L);
        corteAbierto.setCaja(caja);
        corteAbierto.setUsuario(empleado);
        corteAbierto.setMontoInicial(new BigDecimal("200.00"));
        corteAbierto.setFechaHoraApertura(Instant.now().minusSeconds(3600));

        when(corteCajaRepository.findById(500L)).thenReturn(Optional.of(corteAbierto));
        when(corteCajaRepository.save(any(CorteCaja.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CerrarCorteRequest(new BigDecimal("750.00"));
        CorteCajaResponse response = service.cerrarCorte(500L, request, 1L);

        assertThat(response.cerrado()).isTrue();
        assertThat(response.totalReal()).isEqualByComparingTo("750.00");
        assertThat(corteAbierto.getFechaHoraCierre()).isNotNull();
    }

    @Test
    @DisplayName("Impide cerrar un corte que ya está cerrado")
    void impideCerrarCorteYaCerrado() {
        when(usuarioJpaRepository.findById(1L)).thenReturn(Optional.of(empleado));

        CorteCaja corteCerrado = new CorteCaja();
        ReflectionTestUtils.setField(corteCerrado, "id", 500L);
        corteCerrado.setCaja(caja);
        corteCerrado.setFechaHoraCierre(Instant.now());

        when(corteCajaRepository.findById(500L)).thenReturn(Optional.of(corteCerrado));

        var request = new CerrarCorteRequest(new BigDecimal("500.00"));

        assertThatThrownBy(() -> service.cerrarCorte(500L, request, 1L))
            .isInstanceOf(CorteCajaYaCerradoException.class)
            .hasMessageContaining("ya se encuentra cerrado");
    }

    @Test
    @DisplayName("Lanza excepción si no hay corte activo al consultar")
    void lanzaExcepcionSiNoHayCorteActivo() {
        when(usuarioJpaRepository.findById(1L)).thenReturn(Optional.of(empleado));
        when(corteCajaRepository.findByCaja_Sucursal_IdAndFechaHoraCierreIsNull(10L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerCorteActivo(null, 1L))
            .isInstanceOf(CorteCajaNoAbiertoException.class)
            .hasMessageContaining("No hay un corte de caja abierto");
    }
}
