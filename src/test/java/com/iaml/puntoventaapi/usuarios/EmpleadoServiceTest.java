package com.iaml.puntoventaapi.usuarios;

import com.iaml.puntoventaapi.autenticacion.AutenticacionContrato;
import com.iaml.puntoventaapi.usuarios.application.*;
import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import com.iaml.puntoventaapi.usuarios.domain.exception.*;
import com.iaml.puntoventaapi.usuarios.domain.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmpleadoServiceTest {

    private final EmpleadoRepositorioPort empleados = mock(EmpleadoRepositorioPort.class);
    private final NegocioPort negocios = mock(NegocioPort.class);
    private final SucursalPort sucursales = mock(SucursalPort.class);
    private final UsuarioAutenticadoPort usuarioAutenticado = mock(UsuarioAutenticadoPort.class);
    private final AutenticacionContrato autenticacion = mock(AutenticacionContrato.class);
    private final GeneradorPasswordPort generadorPassword = mock(GeneradorPasswordPort.class);

    private final EmpleadoService service = new EmpleadoService(
        empleados, negocios, sucursales, usuarioAutenticado, autenticacion, generadorPassword
    );

    private final Long propietarioId = 1L;
    private final Long negocioId = 10L;
    private final Long sucursalId = 20L;
    private final Long empleadoId = 100L;

    @BeforeEach
    void setUp() {
        when(usuarioAutenticado.obtenerId()).thenReturn(propietarioId);
        when(negocios.perteneceAUsuario(negocioId, propietarioId)).thenReturn(true);
        when(autenticacion.buscarRolIdPorNombre("EMPLEADO")).thenReturn(Optional.of(2L));
        when(autenticacion.hashearPassword(any())).thenReturn("hashed_password");
        when(generadorPassword.generar()).thenReturn("Pass1234!#");
    }

    @Test
    void registrarEmpleadoExitosamente() {
        when(empleados.existePorCorreo("empleado@test.com")).thenReturn(false);
        when(sucursales.perteneceANegocio(sucursalId, negocioId)).thenReturn(true);
        when(sucursales.obtenerNombrePorId(sucursalId)).thenReturn(Optional.of("Sucursal Centro"));
        when(empleados.guardar(any())).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return new Usuario(empleadoId, u.getRolId(), u.getNegocioId(), u.getSucursalIds(),
                u.getNombre(), u.getApellidoPaterno(), u.getApellidoMaterno(), u.getCorreo(), u.getTelefono(), u.getPasswordHash());
        });

        RegistrarEmpleadoCommand command = new RegistrarEmpleadoCommand(
            negocioId, "Carlos", "López", "Gómez", "empleado@test.com", sucursalId
        );

        EmpleadoCreado resultado = service.registrar(command);

        assertThat(resultado.usuario().getId()).isEqualTo(empleadoId);
        assertThat(resultado.usuario().getNombre()).isEqualTo("Carlos");
        assertThat(resultado.usuario().getCorreo()).isEqualTo("empleado@test.com");
        assertThat(resultado.passwordGenerada()).isEqualTo("Pass1234!#");
        assertThat(resultado.sucursalNombre()).isEqualTo("Sucursal Centro");
        verify(empleados).guardar(any());
    }

    @Test
    void registrarEmpleadoRechazaNegocioAjeno() {
        when(negocios.perteneceAUsuario(negocioId, propietarioId)).thenReturn(false);

        RegistrarEmpleadoCommand command = new RegistrarEmpleadoCommand(
            negocioId, "Carlos", "López", null, "carlos@test.com", null
        );

        assertThatThrownBy(() -> service.registrar(command))
            .isInstanceOf(NegocioNoEncontradoException.class);
        verify(empleados, never()).guardar(any());
    }

    @Test
    void registrarEmpleadoRechazaCorreoDuplicado() {
        when(empleados.existePorCorreo("duplicado@test.com")).thenReturn(true);

        RegistrarEmpleadoCommand command = new RegistrarEmpleadoCommand(
            negocioId, "Carlos", null, null, "duplicado@test.com", null
        );

        assertThatThrownBy(() -> service.registrar(command))
            .isInstanceOf(CorreoYaRegistradoException.class);
        verify(empleados, never()).guardar(any());
    }

    @Test
    void registrarEmpleadoRechazaSucursalAjenaAlNegocio() {
        when(empleados.existePorCorreo("carlos@test.com")).thenReturn(false);
        when(sucursales.perteneceANegocio(sucursalId, negocioId)).thenReturn(false);

        RegistrarEmpleadoCommand command = new RegistrarEmpleadoCommand(
            negocioId, "Carlos", null, null, "carlos@test.com", sucursalId
        );

        assertThatThrownBy(() -> service.registrar(command))
            .isInstanceOf(SucursalNoEncontradaException.class);
        verify(empleados, never()).guardar(any());
    }

    @Test
    void listarEmpleadosConYSinAsignacion() {
        Usuario emp1 = new Usuario(101L, 2L, negocioId, List.of(sucursalId), "Ana", "Ruiz", null, "ana@test.com", null, "h");
        Usuario emp2 = new Usuario(102L, 2L, negocioId, List.of(), "Beto", "Diaz", null, "beto@test.com", null, "h");
        when(empleados.listarPorNegocio(negocioId)).thenReturn(List.of(emp1, emp2));
        when(sucursales.obtenerNombrePorId(sucursalId)).thenReturn(Optional.of("Sucursal Norte"));

        // Sin asignación
        List<EmpleadoDetalle> sinAsignacion = service.listarPorNegocio(negocioId, false);
        assertThat(sinAsignacion).hasSize(2);
        assertThat(sinAsignacion.get(0).asignado()).isNull();
        assertThat(sinAsignacion.get(0).sucursales()).isEmpty();

        // Con asignación
        List<EmpleadoDetalle> conAsignacion = service.listarPorNegocio(negocioId, true);
        assertThat(conAsignacion).hasSize(2);
        assertThat(conAsignacion.get(0).asignado()).isTrue();
        assertThat(conAsignacion.get(0).sucursales()).extracting(SucursalDetalle::nombre).containsExactly("Sucursal Norte");
        assertThat(conAsignacion.get(1).asignado()).isFalse();
        assertThat(conAsignacion.get(1).sucursales()).isEmpty();
    }

    @Test
    void asignarSucursalAEmpleadoSinSucursal() {
        Usuario empleadoSinSucursal = new Usuario(empleadoId, 2L, negocioId, List.of(), "Carlos", null, null, "c@test.com", null, "h");
        when(empleados.buscarPorIdYNegocio(empleadoId, negocioId)).thenReturn(Optional.of(empleadoSinSucursal));
        when(sucursales.perteneceANegocio(sucursalId, negocioId)).thenReturn(true);
        when(sucursales.obtenerNombrePorId(sucursalId)).thenReturn(Optional.of("Sucursal Sur"));

        EmpleadoDetalle resultado = service.asignarSucursal(new AsignarSucursalCommand(negocioId, empleadoId, sucursalId));

        assertThat(resultado.usuario().getSucursalIds()).containsExactly(sucursalId);
        assertThat(resultado.asignado()).isTrue();
        assertThat(resultado.sucursales()).extracting(SucursalDetalle::nombre).containsExactly("Sucursal Sur");
        verify(empleados).actualizarSucursales(empleadoId, List.of(sucursalId));
    }

    @Test
    void asignarSucursalConservaLasQueYaTenia() {
        Long otraSucursal = 99L;
        Usuario empleadoConSucursal = new Usuario(empleadoId, 2L, negocioId, List.of(otraSucursal), "Carlos", null, null, "c@test.com", null, "h");
        when(empleados.buscarPorIdYNegocio(empleadoId, negocioId)).thenReturn(Optional.of(empleadoConSucursal));
        when(sucursales.perteneceANegocio(sucursalId, negocioId)).thenReturn(true);

        EmpleadoDetalle resultado = service.asignarSucursal(new AsignarSucursalCommand(negocioId, empleadoId, sucursalId));

        assertThat(resultado.usuario().getSucursalIds()).containsExactly(otraSucursal, sucursalId);
        verify(empleados).actualizarSucursales(empleadoId, List.of(otraSucursal, sucursalId));
    }

    @Test
    void actualizarSucursalesAsignaYDesasignaVarias() {
        Usuario emp = new Usuario(empleadoId, 2L, negocioId, List.of(sucursalId), "Carlos", null, null, "c@test.com", null, "h");
        when(empleados.buscarPorIdYNegocio(empleadoId, negocioId)).thenReturn(Optional.of(emp));
        when(sucursales.perteneceANegocio(30L, negocioId)).thenReturn(true);
        when(sucursales.perteneceANegocio(31L, negocioId)).thenReturn(true);

        EmpleadoDetalle resultado = service.actualizarSucursales(
            new ActualizarSucursalesEmpleadoCommand(negocioId, empleadoId, List.of(30L, 31L, 30L)));

        assertThat(resultado.usuario().getSucursalIds()).containsExactly(30L, 31L);
        assertThat(resultado.asignado()).isTrue();
        verify(empleados).actualizarSucursales(empleadoId, List.of(30L, 31L));
    }

    @Test
    void actualizarSucursalesRechazaSucursalAjenaAlNegocio() {
        Usuario emp = new Usuario(empleadoId, 2L, negocioId, List.of(), "Carlos", null, null, "c@test.com", null, "h");
        when(empleados.buscarPorIdYNegocio(empleadoId, negocioId)).thenReturn(Optional.of(emp));
        when(sucursales.perteneceANegocio(77L, negocioId)).thenReturn(false);

        assertThatThrownBy(() -> service.actualizarSucursales(
            new ActualizarSucursalesEmpleadoCommand(negocioId, empleadoId, List.of(77L))))
            .isInstanceOf(SucursalNoEncontradaException.class);
        verify(empleados, never()).actualizarSucursales(any(), any());
    }

    @Test
    void listarPorSucursalSoloDevuelveSusEmpleados() {
        Usuario emp1 = new Usuario(101L, 2L, negocioId, List.of(sucursalId, 30L), "Ana", "Ruiz", null, "ana@test.com", null, "h");
        Usuario emp2 = new Usuario(102L, 2L, negocioId, List.of(30L), "Beto", "Diaz", null, "beto@test.com", null, "h");
        when(empleados.listarPorNegocio(negocioId)).thenReturn(List.of(emp1, emp2));
        when(sucursales.perteneceANegocio(sucursalId, negocioId)).thenReturn(true);
        when(sucursales.obtenerNombrePorId(any())).thenReturn(Optional.of("S"));

        List<EmpleadoDetalle> lista = service.listarPorNegocio(negocioId, true, sucursalId);

        assertThat(lista).extracting(d -> d.usuario().getId()).containsExactly(101L);
        assertThat(lista.get(0).sucursales()).hasSize(2);
    }

    @Test
    void desasignarSucursalPasandoNull() {
        Usuario empleadoConSucursal = new Usuario(empleadoId, 2L, negocioId, List.of(sucursalId), "Carlos", null, null, "c@test.com", null, "h");
        when(empleados.buscarPorIdYNegocio(empleadoId, negocioId)).thenReturn(Optional.of(empleadoConSucursal));

        EmpleadoDetalle resultado = service.asignarSucursal(new AsignarSucursalCommand(negocioId, empleadoId, null));

        assertThat(resultado.usuario().getSucursalIds()).isEmpty();
        assertThat(resultado.asignado()).isFalse();
        assertThat(resultado.sucursales()).isEmpty();
        verify(empleados).actualizarSucursales(empleadoId, List.of());
    }

    @Test
    void eliminarEmpleadoExitoso() {
        Usuario emp = new Usuario(empleadoId, 2L, negocioId, List.of(sucursalId), "Carlos", null, null, "c@test.com", null, "h");
        when(empleados.buscarPorIdYNegocio(empleadoId, negocioId)).thenReturn(Optional.of(emp));

        service.eliminar(new EliminarEmpleadoCommand(negocioId, empleadoId));

        verify(empleados).eliminar(empleadoId);
    }

    @Test
    void eliminarEmpleadoAjenoFalla() {
        when(empleados.buscarPorIdYNegocio(empleadoId, negocioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(new EliminarEmpleadoCommand(negocioId, empleadoId)))
            .isInstanceOf(EmpleadoNoEncontradoException.class);
        verify(empleados, never()).eliminar(any());
    }

    @Test
    void editarDatosEmpleadoExitoso() {
        Usuario emp = new Usuario(empleadoId, 2L, negocioId, List.of(sucursalId), "Carlos", "Lopez", "Gomez", "c@test.com", null, "h");
        when(empleados.buscarPorIdYNegocio(empleadoId, negocioId)).thenReturn(Optional.of(emp));

        EditarEmpleadoCommand command = new EditarEmpleadoCommand(negocioId, empleadoId, "Carlos Alberto", "López", "Gómez");
        EmpleadoDetalle resultado = service.editar(command);

        assertThat(resultado.usuario().getNombre()).isEqualTo("Carlos Alberto");
        verify(empleados).actualizarDatos(empleadoId, "Carlos Alberto", "López", "Gómez");
    }
}

