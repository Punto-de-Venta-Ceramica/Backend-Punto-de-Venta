package com.iaml.puntoventaapi;

import com.iaml.puntoventaapi.usuarios.domain.Usuario;
import com.iaml.puntoventaapi.usuarios.domain.exception.CorreoYaRegistradoException;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.*;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolEntity;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorreoConcurrenteTest {
    @Test void indiceUnicoSeTraduceAExcepcionDeNegocioSinOcultarOtrosErrores() {
        var repository = mock(UsuarioJpaRepository.class);
        var em = mock(EntityManager.class);
        when(em.getReference(RolEntity.class, 1L)).thenReturn(new RolEntity());
        var adapter = new UsuarioPersistenceAdapter(repository, em);
        var user = new Usuario(null, 1L, "Ana", "Pérez", "", "a@ejemplo.com", "4181234567", "hash");
        var duplicate = new DataIntegrityViolationException("duplicate", new ConstraintViolationException(
            "duplicate", new SQLException("duplicate", "23505"), "uq_usuario_correo"));
        when(repository.saveAndFlush(any())).thenThrow(duplicate);
        assertSame(duplicate, assertThrows(CorreoYaRegistradoException.class, () -> adapter.guardar(user)).getCause());
        var other = new DataIntegrityViolationException("other constraint");
        doThrow(other).when(repository).saveAndFlush(any());
        assertSame(other, assertThrows(DataIntegrityViolationException.class, () -> adapter.guardar(user)));
    }
}
