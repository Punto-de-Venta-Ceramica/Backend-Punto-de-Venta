package com.iaml.puntoventaapi.sucursal;

import com.iaml.puntoventaapi.sucursal.application.CrearSucursalCommand;
import com.iaml.puntoventaapi.sucursal.application.EditarSucursalCommand;
import com.iaml.puntoventaapi.sucursal.application.SucursalService;
import com.iaml.puntoventaapi.sucursal.domain.Sucursal;
import com.iaml.puntoventaapi.sucursal.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.sucursal.domain.exception.SucursalNoEncontradaException;
import com.iaml.puntoventaapi.sucursal.domain.port.NegocioPort;
import com.iaml.puntoventaapi.sucursal.domain.port.SucursalRepositorioPort;
import com.iaml.puntoventaapi.sucursal.domain.port.UsuarioAutenticadoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SucursalServiceTest {

    private final SucursalRepositorioPort sucursales = mock(SucursalRepositorioPort.class);
    private final NegocioPort negocios = mock(NegocioPort.class);
    private final UsuarioAutenticadoPort usuario = mock(UsuarioAutenticadoPort.class);
    private final SucursalService service = new SucursalService(sucursales, negocios, usuario);
    private Sucursal original;

    @BeforeEach
    void preparar() {
        when(usuario.obtenerId()).thenReturn(7L);
        original = new Sucursal(5L, 2L, "Centro", "Original", "Dolores", "Principal", "37800");
    }

    @Test
    void crearVerificaPertenenciaYNormalizaNombre() {
        when(negocios.perteneceAUsuario(2L, 7L)).thenReturn(true);
        when(sucursales.guardar(any(), eq(7L))).thenAnswer(invocation -> invocation.getArgument(0));
        Sucursal creada = service.crear(new CrearSucursalCommand(2L, "  Centro  ", null, null, null, null));
        assertThat(creada.getNombre()).isEqualTo("Centro");
        assertThat(creada.getNegocioId()).isEqualTo(2L);
        assertThat(creada.getId()).isNull();
        verify(negocios).perteneceAUsuario(2L, 7L);
    }

    @Test
    void crearRechazaUnNegocioAjenoOInexistenteSinGuardar() {
        assertThatThrownBy(() -> service.crear(new CrearSucursalCommand(99L, "Centro", null, null, null, null)))
            .isInstanceOf(NegocioNoEncontradoException.class);
        verifyNoInteractions(sucursales);
    }

    @Test
    void patchSoloCambiaNombreYConservaDescripcionNegocioYDomicilio() {
        when(sucursales.buscarPorId(5L, 7L)).thenReturn(Optional.of(original));
        when(sucursales.guardar(any(), eq(7L))).thenAnswer(invocation -> invocation.getArgument(0));
        Sucursal editada = service.editar(new EditarSucursalCommand(5L, " Norte ", null));
        assertThat(editada.getNombre()).isEqualTo("Norte");
        assertThat(editada.getDescripcion()).isEqualTo("Original");
        assertThat(editada.getNegocioId()).isEqualTo(2L);
        assertThat(editada.getCiudad()).isEqualTo("Dolores");
        assertThat(editada.getCalle()).isEqualTo("Principal");
        assertThat(editada.getCodigoPostal()).isEqualTo("37800");
        verify(sucursales).buscarPorId(5L, 7L);
    }

    @Test
    void patchPuedeVaciarLaDescripcionSinCambiarNombre() {
        when(sucursales.buscarPorId(5L, 7L)).thenReturn(Optional.of(original));
        when(sucursales.guardar(any(), eq(7L))).thenAnswer(invocation -> invocation.getArgument(0));
        Sucursal editada = service.editar(new EditarSucursalCommand(5L, null, ""));
        assertThat(editada.getNombre()).isEqualTo("Centro");
        assertThat(editada.getDescripcion()).isEmpty();
    }

    @Test
    void patchRechazaUnNombreEnBlancoOUnCuerpoSinCambios() {
        assertThatThrownBy(() -> original.editar("  ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> original.editar(null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sucursalAjenaONoExistenteProduceElMismoError() {
        when(sucursales.buscarPorId(99L, 7L)).thenReturn(Optional.empty());
        doThrow(new SucursalNoEncontradaException(99L)).when(sucursales).eliminar(99L, 7L);
        assertThatThrownBy(() -> service.editar(new EditarSucursalCommand(99L, "Norte", null)))
            .isInstanceOf(SucursalNoEncontradaException.class);
        assertThatThrownBy(() -> service.eliminar(99L)).isInstanceOf(SucursalNoEncontradaException.class);
        verify(sucursales, never()).guardar(any(), any());
    }

    @Test
    void eliminarSiempreIncluyeElUsuarioDelJwt() {
        service.eliminar(5L);
        verify(sucursales).eliminar(5L, 7L);
    }

    @Test
    void consultaPorNegocioYUsuarioPermiteListaVacia() {
        when(negocios.perteneceAUsuario(2L, 7L)).thenReturn(true);
        when(sucursales.buscarPorNegocioId(2L, 7L)).thenReturn(List.of(original), List.of());
        assertThat(service.consultarPorNegocio(2L)).containsExactly(original);
        assertThat(service.consultarPorNegocio(2L)).isEmpty();
    }

    @Test
    void consultarUnNegocioAjenoNoConsultaSusSucursales() {
        assertThatThrownBy(() -> service.consultarPorNegocio(99L))
            .isInstanceOf(NegocioNoEncontradoException.class);
        verifyNoInteractions(sucursales);
    }

    @Test
    void sinUsuarioAutenticadoNingunaOperacionAccedeALaBaseDeDatos() {
        when(usuario.obtenerId()).thenThrow(new AuthenticationCredentialsNotFoundException("Sin JWT"));
        List<Runnable> operaciones = List.of(
            () -> service.crear(new CrearSucursalCommand(2L, "Centro", null, null, null, null)),
            () -> service.editar(new EditarSucursalCommand(5L, "Norte", null)),
            () -> service.eliminar(5L),
            () -> service.consultarPorNegocio(2L));
        for (Runnable operacion : operaciones) {
            assertThatThrownBy(operacion::run).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        }
        verifyNoInteractions(sucursales, negocios);
    }

    @Test
    void cambiarDeUsuarioNoReutilizaElAccesoDelUsuarioAnterior() {
        when(usuario.obtenerId()).thenReturn(7L, 8L);
        when(negocios.perteneceAUsuario(2L, 7L)).thenReturn(true);
        when(sucursales.buscarPorNegocioId(2L, 7L)).thenReturn(List.of(original));
        assertThat(service.consultarPorNegocio(2L)).containsExactly(original);
        assertThatThrownBy(() -> service.consultarPorNegocio(2L)).isInstanceOf(NegocioNoEncontradoException.class);
        verify(sucursales, never()).buscarPorNegocioId(2L, 8L);
    }
}

