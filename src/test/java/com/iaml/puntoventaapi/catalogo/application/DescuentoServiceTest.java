package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.AsignacionesDescuento;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.DescuentoConAsignaciones;
import com.iaml.puntoventaapi.catalogo.domain.DestinoDescuento;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.DescuentoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoGestionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DescuentoServiceTest {

    private static final Long PROPIETARIO = 7L;
    private static final Long NEGOCIO = 100L;
    private static final Long NEGOCIO_AJENO = 200L;
    private static final Long PRODUCTO = 11L;
    private static final Long CATEGORIA = 21L;
    private static final Long AREA = 31L;
    private static final Long PRODUCTO_AJENO = 12L;

    private final FakeGestion gestion = new FakeGestion();
    private DescuentoService service;

    @BeforeEach
    void preparar() {
        service = new DescuentoService(gestion, (negocioId, usuarioId) -> NEGOCIO.equals(negocioId)
            && PROPIETARIO.equals(usuarioId));
    }

    private GuardarDescuentoCommand comando(String nombre) {
        return new GuardarDescuentoCommand(nombre, 6, null, new BigDecimal("10.00"));
    }

    private Long crearDescuento() {
        return service.crear(NEGOCIO, comando("Mayoreo"), PROPIETARIO).descuento().getId();
    }

    @Test
    void creaUnDescuentoActivoYSinAsignaciones() {
        DescuentoConAsignaciones creado = service.crear(NEGOCIO, comando("  Mayoreo "), PROPIETARIO);

        assertEquals("Mayoreo", creado.descuento().getNombre());
        assertTrue(creado.descuento().isActivo());
        assertTrue(creado.asignaciones().productoIds().isEmpty());
        assertTrue(creado.asignaciones().categoriaIds().isEmpty());
        assertTrue(creado.asignaciones().areaIds().isEmpty());
    }

    @Test
    void datosInvalidosDelDominioSeTraducenAUnErrorDePeticionInvalida() {
        assertThrows(DescuentoInvalidoException.class, () -> service.crear(NEGOCIO,
            new GuardarDescuentoCommand("X", 10, 5, BigDecimal.TEN), PROPIETARIO));
        Long id = crearDescuento();
        assertThrows(DescuentoInvalidoException.class, () -> service.editar(NEGOCIO, id,
            new GuardarDescuentoCommand(" ", 1, null, BigDecimal.TEN), PROPIETARIO));
    }

    @Test
    void unNegocioAjenoSeTrataComoNoEncontrado() {
        assertThrows(CatalogoNoEncontradoException.class, () -> service.crear(NEGOCIO_AJENO, comando("A"), PROPIETARIO));
        assertThrows(CatalogoNoEncontradoException.class, () -> service.listar(NEGOCIO_AJENO, PROPIETARIO));
        assertThrows(CatalogoNoEncontradoException.class, () -> service.crear(NEGOCIO, comando("A"), 99L));
    }

    @Test
    void unDescuentoDeOtroNegocioNoSePuedeConsultarEditarNiEliminar() {
        Long id = crearDescuento();
        gestion.negocioDe.put(id, NEGOCIO_AJENO);

        assertThrows(CatalogoNoEncontradoException.class, () -> service.consultar(NEGOCIO, id, PROPIETARIO));
        assertThrows(CatalogoNoEncontradoException.class, () -> service.editar(NEGOCIO, id, comando("B"), PROPIETARIO));
        assertThrows(CatalogoNoEncontradoException.class, () -> service.eliminar(NEGOCIO, id, PROPIETARIO));
    }

    @Test
    void listaTodosLosDescuentosIncluidosLosInactivosConSusAsignaciones() {
        Long a = crearDescuento();
        Long b = crearDescuento();
        service.cambiarEstado(NEGOCIO, b, false, PROPIETARIO);
        service.asignar(NEGOCIO, a, DestinoDescuento.CATEGORIA, CATEGORIA, PROPIETARIO);

        List<DescuentoConAsignaciones> lista = service.listar(NEGOCIO, PROPIETARIO);

        assertEquals(2, lista.size());
        assertEquals(Set.of(CATEGORIA), lista.get(0).asignaciones().categoriaIds());
        assertFalse(lista.get(1).descuento().isActivo());
    }

    @Test
    void editaNombreRangoYPorcentajeSinTocarLasAsignaciones() {
        Long id = crearDescuento();
        service.asignar(NEGOCIO, id, DestinoDescuento.PRODUCTO, PRODUCTO, PROPIETARIO);

        DescuentoConAsignaciones editado = service.editar(NEGOCIO, id,
            new GuardarDescuentoCommand("Nuevo", 12, 20, new BigDecimal("15.50")), PROPIETARIO);

        assertEquals("Nuevo", editado.descuento().getNombre());
        assertEquals(12, editado.descuento().getCantidadMinima());
        assertEquals(20, editado.descuento().getCantidadMaxima());
        assertEquals(Set.of(PRODUCTO), editado.asignaciones().productoIds());
    }

    @Test
    void activaYDesactivaElDescuento() {
        Long id = crearDescuento();

        assertFalse(service.cambiarEstado(NEGOCIO, id, false, PROPIETARIO).descuento().isActivo());
        assertTrue(service.cambiarEstado(NEGOCIO, id, true, PROPIETARIO).descuento().isActivo());
    }

    @Test
    void eliminaElDescuentoYSusAsignaciones() {
        Long id = crearDescuento();
        service.asignar(NEGOCIO, id, DestinoDescuento.AREA, AREA, PROPIETARIO);

        service.eliminar(NEGOCIO, id, PROPIETARIO);

        assertTrue(service.listar(NEGOCIO, PROPIETARIO).isEmpty());
        assertFalse(gestion.asignaciones.containsKey(id));
    }

    @Test
    void asignaYQuitaProductoCategoriaYArea() {
        Long id = crearDescuento();

        service.asignar(NEGOCIO, id, DestinoDescuento.PRODUCTO, PRODUCTO, PROPIETARIO);
        service.asignar(NEGOCIO, id, DestinoDescuento.CATEGORIA, CATEGORIA, PROPIETARIO);
        AsignacionesDescuento todas = service.asignar(NEGOCIO, id, DestinoDescuento.AREA, AREA, PROPIETARIO)
            .asignaciones();
        assertEquals(Set.of(PRODUCTO), todas.productoIds());
        assertEquals(Set.of(CATEGORIA), todas.categoriaIds());
        assertEquals(Set.of(AREA), todas.areaIds());

        service.quitar(NEGOCIO, id, DestinoDescuento.PRODUCTO, PRODUCTO, PROPIETARIO);
        service.quitar(NEGOCIO, id, DestinoDescuento.CATEGORIA, CATEGORIA, PROPIETARIO);
        AsignacionesDescuento restante = service.quitar(NEGOCIO, id, DestinoDescuento.AREA, AREA, PROPIETARIO)
            .asignaciones();
        assertTrue(restante.productoIds().isEmpty() && restante.categoriaIds().isEmpty()
            && restante.areaIds().isEmpty());
    }

    @Test
    void asignarDosVecesOQuitarLoQueNoEstabaEsIdempotente() {
        Long id = crearDescuento();

        service.asignar(NEGOCIO, id, DestinoDescuento.PRODUCTO, PRODUCTO, PROPIETARIO);
        AsignacionesDescuento repetida = service.asignar(NEGOCIO, id, DestinoDescuento.PRODUCTO, PRODUCTO, PROPIETARIO)
            .asignaciones();

        assertEquals(Set.of(PRODUCTO), repetida.productoIds());
        service.quitar(NEGOCIO, id, DestinoDescuento.CATEGORIA, CATEGORIA, PROPIETARIO);
    }

    @Test
    void noAsignaProductosCategoriasOAreasDeOtroNegocio() {
        Long id = crearDescuento();

        assertThrows(CatalogoNoEncontradoException.class,
            () -> service.asignar(NEGOCIO, id, DestinoDescuento.PRODUCTO, PRODUCTO_AJENO, PROPIETARIO));
        assertThrows(CatalogoNoEncontradoException.class,
            () -> service.asignar(NEGOCIO, id, DestinoDescuento.CATEGORIA, 999L, PROPIETARIO));
        assertThrows(CatalogoNoEncontradoException.class,
            () -> service.asignar(NEGOCIO, id, DestinoDescuento.AREA, 999L, PROPIETARIO));
        assertTrue(service.consultar(NEGOCIO, id, PROPIETARIO).asignaciones().productoIds().isEmpty());
    }

    /** Gestión de descuentos en memoria. Los destinos válidos del NEGOCIO son PRODUCTO, CATEGORIA y AREA. */
    private static final class FakeGestion implements DescuentoGestionPort {

        final Map<Long, Descuento> guardados = new HashMap<>();
        final Map<Long, Long> negocioDe = new HashMap<>();
        final Map<Long, Map<DestinoDescuento, Set<Long>>> asignaciones = new HashMap<>();
        private long siguienteId = 1;

        @Override
        public Descuento guardar(Descuento descuento) {
            Long id = descuento.getId() != null ? descuento.getId() : siguienteId++;
            Descuento persistido = new Descuento(id, descuento.getNegocioId(), descuento.getNombre(),
                descuento.getCantidadMinima(), descuento.getCantidadMaxima(), descuento.getPorcentaje(),
                descuento.isActivo());
            guardados.put(id, persistido);
            negocioDe.putIfAbsent(id, descuento.getNegocioId());
            return persistido;
        }

        @Override
        public Optional<Descuento> buscarPorId(Long id, Long negocioId) {
            Descuento d = guardados.get(id);
            return d != null && negocioId.equals(negocioDe.get(id)) ? Optional.of(copia(d)) : Optional.empty();
        }

        @Override
        public List<Descuento> listarPorNegocio(Long negocioId) {
            return guardados.values().stream().filter(d -> negocioId.equals(negocioDe.get(d.getId())))
                .sorted((x, y) -> x.getId().compareTo(y.getId())).map(FakeGestion::copia).toList();
        }

        @Override
        public void eliminar(Long descuentoId) {
            guardados.remove(descuentoId);
            asignaciones.remove(descuentoId);
        }

        @Override
        public AsignacionesDescuento asignaciones(Long descuentoId) {
            Map<DestinoDescuento, Set<Long>> a = asignaciones.getOrDefault(descuentoId, Map.of());
            return new AsignacionesDescuento(a.getOrDefault(DestinoDescuento.PRODUCTO, Set.of()),
                a.getOrDefault(DestinoDescuento.CATEGORIA, Set.of()), a.getOrDefault(DestinoDescuento.AREA, Set.of()));
        }

        @Override
        public Map<Long, AsignacionesDescuento> asignacionesDe(Collection<Long> descuentoIds) {
            Map<Long, AsignacionesDescuento> resultado = new HashMap<>();
            descuentoIds.forEach(id -> resultado.put(id, asignaciones(id)));
            return resultado;
        }

        @Override
        public boolean destinoPerteneceAlNegocio(DestinoDescuento destino, Long destinoId, Long negocioId) {
            return NEGOCIO.equals(negocioId) && switch (destino) {
                case PRODUCTO -> PRODUCTO.equals(destinoId);
                case CATEGORIA -> CATEGORIA.equals(destinoId);
                case AREA -> AREA.equals(destinoId);
            };
        }

        @Override
        public void asignar(Long descuentoId, DestinoDescuento destino, Long destinoId) {
            asignaciones.computeIfAbsent(descuentoId, k -> new HashMap<>())
                .computeIfAbsent(destino, k -> new HashSet<>()).add(destinoId);
        }

        @Override
        public void quitar(Long descuentoId, DestinoDescuento destino, Long destinoId) {
            Map<DestinoDescuento, Set<Long>> a = asignaciones.get(descuentoId);
            if (a != null && a.containsKey(destino)) {
                a.get(destino).remove(destinoId);
            }
        }

        private static Descuento copia(Descuento d) {
            return new Descuento(d.getId(), d.getNegocioId(), d.getNombre(), d.getCantidadMinima(),
                d.getCantidadMaxima(), d.getPorcentaje(), d.isActivo());
        }
    }
}
