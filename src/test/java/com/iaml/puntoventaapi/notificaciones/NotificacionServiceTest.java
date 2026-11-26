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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.PartTree;

class NotificacionServiceTest {
    @Test
    void ordenaDelMasRecienteAlMasAntiguo() {
        var consulta = new PartTree("findByUsuario_IdOrderByFechaDescIdDesc", Notificacion.class);
        assertNull(consulta.getSort().getOrderFor("leida"));
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
    void soloConsultaAvisosDelUsuarioSolicitadoDeVeinteEnVeinte() {
        var repo = mock(NotificacionRepository.class);
        var service = new NotificacionService(repo, mock(EntityManager.class));
        var pedido = PageRequest.of(1, 20);
        when(repo.findByUsuario_IdOrderByFechaDescIdDesc(7L, pedido))
            .thenReturn(new PageImpl<>(List.of(), pedido, 25));
        var pagina = service.listar(7L, 1);
        assertTrue(pagina.contenido().isEmpty());
        assertEquals(1, pagina.pagina());
        assertEquals(25L, pagina.totalElementos());
        assertTrue(pagina.ultima());
        verify(repo).findByUsuario_IdOrderByFechaDescIdDesc(7L, pedido);
    }

    @Test
    void unaPaginaNegativaSeTrataComoLaPrimera() {
        var repo = mock(NotificacionRepository.class);
        var service = new NotificacionService(repo, mock(EntityManager.class));
        var primera = PageRequest.of(0, 20);
        when(repo.findByUsuario_IdOrderByFechaDescIdDesc(7L, primera))
            .thenReturn(new PageImpl<>(List.of(), primera, 100));
        assertFalse(service.listar(7L, -3).ultima());
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
