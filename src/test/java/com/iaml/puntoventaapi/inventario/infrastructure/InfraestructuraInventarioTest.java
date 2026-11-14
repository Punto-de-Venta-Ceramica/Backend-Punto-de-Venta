package com.iaml.puntoventaapi.inventario.infrastructure;

import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.ProductoEntity;
import com.iaml.puntoventaapi.inventario.application.InventarioContratoService;
import com.iaml.puntoventaapi.inventario.domain.ExistenciaProducto;
import com.iaml.puntoventaapi.inventario.domain.Inventario;
import com.iaml.puntoventaapi.inventario.domain.exception.InventarioNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.port.InventarioRepositorioPort;
import com.iaml.puntoventaapi.inventario.infrastructure.adapters.NegocioAdapter;
import com.iaml.puntoventaapi.inventario.infrastructure.persistence.InventarioEntity;
import com.iaml.puntoventaapi.inventario.infrastructure.persistence.InventarioJpaRepository;
import com.iaml.puntoventaapi.inventario.infrastructure.persistence.InventarioMapper;
import com.iaml.puntoventaapi.inventario.infrastructure.persistence.InventarioPersistenceAdapter;
import com.iaml.puntoventaapi.negocios.NegociosContrato;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InfraestructuraInventarioTest {

    private InventarioJpaRepository jpa;
    private EntityManager em;
    private InventarioPersistenceAdapter adapter;
    private SucursalEntity sucursal;
    private ProductoEntity producto;

    @BeforeEach
    void preparar() {
        jpa = mock(InventarioJpaRepository.class);
        em = mock(EntityManager.class);
        adapter = new InventarioPersistenceAdapter(jpa, em);
        sucursal = new SucursalEntity();
        ReflectionTestUtils.setField(sucursal, "id", 3L);
        producto = new ProductoEntity();
        ReflectionTestUtils.setField(producto, "id", 2L);
    }

    private InventarioEntity entidad(Long id, int existencia, boolean activo) {
        InventarioEntity entity = new InventarioEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setSucursal(sucursal);
        entity.setProducto(producto);
        entity.setExistencia(existencia);
        entity.setActivo(activo);
        return entity;
    }

    @Test
    void guardarInsertaSinIdUsandoReferencias() {
        when(em.getReference(SucursalEntity.class, 3L)).thenReturn(sucursal);
        when(em.getReference(ProductoEntity.class, 2L)).thenReturn(producto);
        when(jpa.saveAndFlush(any())).thenAnswer(i -> {
            InventarioEntity e = i.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 10L);
            return e;
        });

        Inventario guardado = adapter.guardar(new Inventario(null, 3L, 2L, 5));

        assertEquals(10L, guardado.getId());
        assertEquals(5, guardado.getExistencia());
        verify(jpa, never()).findById(any());
    }

    @Test
    void guardarActualizaLaExistenciaYElEstado() {
        InventarioEntity existente = entidad(10L, 5, true);
        when(jpa.findById(10L)).thenReturn(Optional.of(existente));
        when(jpa.saveAndFlush(existente)).thenReturn(existente);

        Inventario guardado = adapter.guardar(new Inventario(10L, 3L, 2L, 8, false));

        assertEquals(8, existente.getExistencia());
        assertFalse(existente.isActivo());
        assertEquals(8, guardado.getExistencia());
    }

    @Test
    void guardarFallaSiElRegistroNoExiste() {
        when(jpa.findById(10L)).thenReturn(Optional.empty());

        Inventario inventario = new Inventario(10L, 3L, 2L, 1);
        assertThrows(InventarioNoEncontradoException.class, () -> adapter.guardar(inventario));
    }

    @Test
    void buscarParaAjusteYParaQuitarExigenAcceso() {
        InventarioEntity entity = entidad(10L, 5, true);
        when(jpa.esAccesibleParaUsuario(10L, 7L)).thenReturn(true);
        when(jpa.esAccesibleParaUsuario(11L, 7L)).thenReturn(false);
        when(jpa.esDelPropietario(10L, 7L)).thenReturn(true);
        when(jpa.esDelPropietario(11L, 7L)).thenReturn(false);
        when(jpa.buscarConBloqueo(10L)).thenReturn(Optional.of(entity));

        assertEquals(10L, adapter.buscarParaAjuste(10L, 7L).orElseThrow().getId());
        assertTrue(adapter.buscarParaAjuste(11L, 7L).isEmpty());
        assertEquals(10L, adapter.buscarParaQuitar(10L, 7L).orElseThrow().getId());
        assertTrue(adapter.buscarParaQuitar(11L, 7L).isEmpty());
    }

    @Test
    void consultasSimplesDelegan() {
        when(jpa.negocioDeProducto(2L, 7L)).thenReturn(Optional.of(5L));
        when(jpa.negocioDeSucursal(3L, 7L)).thenReturn(Optional.of(5L));
        when(jpa.findBySucursalIdAndProductoId(3L, 2L)).thenReturn(Optional.of(entidad(10L, 1, true)));
        when(jpa.contarActivosDeProducto(2L)).thenReturn(4L);

        assertEquals(Optional.of(5L), adapter.negocioDeProducto(2L, 7L));
        assertEquals(Optional.of(5L), adapter.negocioDeSucursal(3L, 7L));
        assertTrue(adapter.buscarPorSucursalYProducto(3L, 2L).isPresent());
        assertEquals(4L, adapter.contarActivosDeProducto(2L));
    }

    @Test
    void listaElPanelPorNegocioPorSucursalYPorProducto() {
        Object[] fila = {1, 2L, "Taza", 3L, "Centro", 9};
        when(jpa.panelPorNegocio(5L, null, 7L)).thenReturn(List.<Object[]>of(fila));
        when(jpa.panelPorSucursal(5L, 3L, null, 7L)).thenReturn(List.<Object[]>of(fila));
        when(jpa.existenciasDeProducto(2L, 7L)).thenReturn(List.<Object[]>of(fila));

        List<ExistenciaProducto> negocio = adapter.listarPanel(5L, null, null, 7L);
        List<ExistenciaProducto> porSucursal = adapter.listarPanel(5L, 3L, null, 7L);
        List<ExistenciaProducto> porProducto = adapter.listarPorProducto(2L, 7L);

        assertEquals(9, negocio.get(0).existencia());
        assertEquals("Centro", porSucursal.get(0).sucursalNombre());
        assertEquals(1L, porProducto.get(0).inventarioId());
    }

    @Test
    void mapperConvierteEntreDominioYEntidad() {
        Inventario dominio = InventarioMapper.toDomain(entidad(10L, 6, false));
        InventarioEntity entity = InventarioMapper.toEntity(new Inventario(null, 3L, 2L, 4, true), sucursal, producto);

        assertEquals(6, dominio.getExistencia());
        assertFalse(dominio.isActivo());
        assertEquals(4, entity.getExistencia());
        assertTrue(entity.isActivo());
        assertSame(sucursal, entity.getSucursal());
        assertSame(producto, entity.getProducto());
        assertEquals(0, new InventarioEntity().getExistencia());
    }

    @Test
    void negocioAdapterDelega() {
        NegociosContrato negocios = mock(NegociosContrato.class);
        SucursalContrato sucursales = mock(SucursalContrato.class);
        when(negocios.perteneceAUsuario(5L, 7L)).thenReturn(true);
        when(sucursales.negocioAutorizado(3L, 7L)).thenReturn(Optional.of(5L));
        when(sucursales.negocioAutorizado(4L, 7L)).thenReturn(Optional.of(9L));
        when(sucursales.negocioAutorizado(8L, 7L)).thenReturn(Optional.empty());
        NegocioAdapter negocioAdapter = new NegocioAdapter(negocios, sucursales);

        assertTrue(negocioAdapter.perteneceAUsuario(5L, 7L));
        assertTrue(negocioAdapter.puedeOperarSucursal(5L, 3L, 7L));
        assertFalse(negocioAdapter.puedeOperarSucursal(5L, 4L, 7L));
        assertFalse(negocioAdapter.puedeOperarSucursal(5L, 8L, 7L));
    }

    @Test
    void contratoRegistraLasExistenciasIniciales() {
        InventarioRepositorioPort puerto = mock(InventarioRepositorioPort.class);

        new InventarioContratoService(puerto).registrarExistenciasIniciales(2L, Map.of(3L, 5));

        ArgumentCaptor<Inventario> captor = ArgumentCaptor.forClass(Inventario.class);
        verify(puerto).guardar(captor.capture());
        assertEquals(3L, captor.getValue().getSucursalId());
        assertEquals(2L, captor.getValue().getProductoId());
        assertEquals(5, captor.getValue().getExistencia());
    }
}
