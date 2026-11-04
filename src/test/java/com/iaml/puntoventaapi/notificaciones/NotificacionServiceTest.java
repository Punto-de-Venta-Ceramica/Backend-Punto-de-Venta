package com.iaml.puntoventaapi.notificaciones;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iaml.puntoventaapi.notificaciones.application.NotificacionService;
import com.iaml.puntoventaapi.notificaciones.domain.Notificacion;
import com.iaml.puntoventaapi.notificaciones.infrastructure.persistence.NotificacionRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.PartTree;

class NotificacionServiceTest {
    @Test
    void priorizaNoLeidasAntesDeAplicarElLimite() {
        var consulta = new PartTree("findTop100ByUsuario_IdOrderByLeidaAscFechaDescIdDesc", Notificacion.class);
        assertEquals(100, consulta.getMaxResults());
        assertEquals(Sort.Direction.ASC, consulta.getSort().getOrderFor("leida").getDirection());
        assertEquals(Sort.Direction.DESC, consulta.getSort().getOrderFor("fecha").getDirection());
        assertEquals(Sort.Direction.DESC, consulta.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void cuentaTodasLasNoLeidasDelUsuarioSinLimiteDeLista() {
        var repo = mock(NotificacionRepository.class);
        var service = new NotificacionService(repo, mock(EntityManager.class));
        when(repo.countByUsuario_IdAndLeidaFalse(7L)).thenReturn(143L);
        assertEquals(143L, service.contarNoLeidas(7L).cantidad());
        verify(repo).countByUsuario_IdAndLeidaFalse(7L);
    }
    @Test
    void soloConsultaAvisosDelUsuarioSolicitado() {
        var repo = mock(NotificacionRepository.class);
        var service = new NotificacionService(repo, mock(EntityManager.class));
        when(repo.findTop100ByUsuario_IdOrderByLeidaAscFechaDescIdDesc(7L)).thenReturn(List.of());
        assertTrue(service.listar(7L).isEmpty());
        verify(repo).findTop100ByUsuario_IdOrderByLeidaAscFechaDescIdDesc(7L);
    }

    @Test
    void noMarcaComoLeidoUnAvisoQueNoPerteneceAlUsuario() {
        var repo = mock(NotificacionRepository.class);
        var service = new NotificacionService(repo, mock(EntityManager.class));
        when(repo.findByIdAndUsuario_Id(12L, 7L)).thenReturn(Optional.empty());
        assertFalse(service.marcarLeida(7L, 12L));
        verify(repo).findByIdAndUsuario_Id(12L, 7L);
        verify(repo, never()).save(any());
    }

    @Test
    void marcaLeidoElAvisoQuePerteneceAlUsuario() {
        var repo = mock(NotificacionRepository.class);
        var aviso = new Notificacion();
        var service = new NotificacionService(repo, mock(EntityManager.class));
        when(repo.findByIdAndUsuario_Id(12L, 7L)).thenReturn(Optional.of(aviso));
        assertTrue(service.marcarLeida(7L, 12L));
        assertTrue(aviso.getLeida());
    }
}
