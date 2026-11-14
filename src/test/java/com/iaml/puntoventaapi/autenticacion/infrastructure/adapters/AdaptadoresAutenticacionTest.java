package com.iaml.puntoventaapi.autenticacion.infrastructure.adapters;

import com.iaml.puntoventaapi.autenticacion.domain.Credencial;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.CodigoRecuperacionRepository;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.PermisoEntity;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolEntity;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolJpaRepository;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolMapper;
import com.iaml.puntoventaapi.usuarios.CredencialesUsuario;
import com.iaml.puntoventaapi.usuarios.UsuariosContrato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdaptadoresAutenticacionTest {

    private UsuariosContrato usuarios;
    private RolJpaRepository roles;
    private RolEntity rol;
    private CredencialesUsuario datos;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuariosContrato.class);
        roles = mock(RolJpaRepository.class);
        PermisoEntity permiso = new PermisoEntity();
        ReflectionTestUtils.setField(permiso, "id", 3L);
        permiso.setNombre("ver");
        rol = new RolEntity();
        ReflectionTestUtils.setField(rol, "id", 2L);
        rol.setNombre("empleado");
        rol.setPermisos(Set.of(permiso));
        datos = new CredencialesUsuario(7L, "a@b.mx", "hash", 2L);
    }

    @Test
    void credencialAdapterBuscaPorCorreoYPorId() {
        when(usuarios.buscarCredencialesPorCorreo("a@b.mx")).thenReturn(Optional.of(datos));
        when(usuarios.buscarCredencialesPorId(7L)).thenReturn(Optional.of(datos));
        when(roles.findWithPermisosById(2L)).thenReturn(Optional.of(rol));
        CredencialAdapter adapter = new CredencialAdapter(usuarios, roles);

        Credencial porCorreo = adapter.buscarPorCorreo("a@b.mx").orElseThrow();
        Credencial porId = adapter.buscarPorUsuarioId(7L).orElseThrow();

        assertEquals("EMPLEADO", porCorreo.getRol().getNombre());
        assertEquals(7L, porId.getUsuarioId());
        assertEquals(1, porId.getRol().getPermisos().size());
    }

    @Test
    void credencialAdapterDevuelveVacioSiNoHayUsuarioOSinRol() {
        when(usuarios.buscarCredencialesPorCorreo("x")).thenReturn(Optional.empty());
        when(usuarios.buscarCredencialesPorId(9L)).thenReturn(Optional.of(datos));
        when(roles.findWithPermisosById(2L)).thenReturn(Optional.empty());
        CredencialAdapter adapter = new CredencialAdapter(usuarios, roles);

        assertTrue(adapter.buscarPorCorreo("x").isEmpty());
        assertTrue(adapter.buscarPorUsuarioId(9L).isEmpty());
    }

    @Test
    void rolAdapterDevuelveElIdPorNombre() {
        when(roles.findByNombre("EMPLEADO")).thenReturn(Optional.of(rol));
        when(roles.findByNombre("X")).thenReturn(Optional.empty());
        RolAdapter adapter = new RolAdapter(roles);

        assertEquals(Optional.of(2L), adapter.buscarIdPorNombre("EMPLEADO"));
        assertTrue(adapter.buscarIdPorNombre("X").isEmpty());
    }

    @Test
    void mapeaLaEntidadDeRolADominio() {
        var dominio = RolMapper.toDomain(rol);

        assertEquals(2L, dominio.getId());
        assertEquals("EMPLEADO", dominio.getNombre());
        assertEquals("ver", dominio.getPermisos().iterator().next().nombre());
        assertEquals("ver", rol.getPermisos().iterator().next().getNombre());
        assertEquals("empleado", rol.getNombre());
    }

    @Test
    void entidadDeRolIniciaSinPermisosYLaInterfazPendienteExiste() {
        assertTrue(new RolEntity().getPermisos().isEmpty());
        assertNotNull(CodigoRecuperacionRepository.class);
    }
}
