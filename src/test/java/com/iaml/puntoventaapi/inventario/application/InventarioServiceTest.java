package com.iaml.puntoventaapi.inventario.application;

import com.iaml.puntoventaapi.inventario.domain.ExistenciaProducto;
import com.iaml.puntoventaapi.inventario.domain.Inventario;
import com.iaml.puntoventaapi.inventario.domain.exception.ExistenciaInsuficienteException;
import com.iaml.puntoventaapi.inventario.domain.exception.InventarioNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.exception.InventarioYaExisteException;
import com.iaml.puntoventaapi.inventario.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.exception.ProductoOSucursalNoEncontradoException;
import com.iaml.puntoventaapi.inventario.domain.port.InventarioRepositorioPort;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventarioServiceTest {

    private static final Long PROPIETARIO = 7L;
    private static final Long NEGOCIO = 100L;

    private final FakeInventarios repositorio = new FakeInventarios();
    // Puerto de negocio sustituido por lambda: solo el negocio 100 es del usuario 7.
    private final InventarioService service = new InventarioService(repositorio,
        (negocioId, usuarioId) -> NEGOCIO.equals(negocioId) && PROPIETARIO.equals(usuarioId));

    @Test
    void ajustaSumandoYRestando() {
        repositorio.registrar(new Inventario(1L, 10L, 20L, 5));

        assertEquals(8, service.ajustar(1L, 3, PROPIETARIO).getExistencia());
        assertEquals(2, service.ajustar(1L, -6, PROPIETARIO).getExistencia());
        assertEquals(2, repositorio.guardados.get(1L).getExistencia());
    }

    @Test
    void noGuardaSiLaExistenciaQuedariaNegativa() {
        repositorio.registrar(new Inventario(1L, 10L, 20L, 2));

        assertThrows(ExistenciaInsuficienteException.class, () -> service.ajustar(1L, -3, PROPIETARIO));
        assertEquals(2, repositorio.guardados.get(1L).getExistencia());
    }

    @Test
    void registroDeOtroUsuarioSeTrataComoInexistente() {
        repositorio.registrar(new Inventario(1L, 10L, 20L, 5));

        assertThrows(InventarioNoEncontradoException.class, () -> service.ajustar(1L, 1, 999L));
        assertThrows(InventarioNoEncontradoException.class, () -> service.ajustar(2L, 1, PROPIETARIO));
    }

    @Test
    void panelRechazaNegocioAjenoYDevuelveFilasDelPropio() {
        assertThrows(NegocioNoEncontradoException.class, () -> service.consultarPanel(55L, null, PROPIETARIO));

        List<ExistenciaProducto> panel = service.consultarPanel(NEGOCIO, null, PROPIETARIO);

        assertEquals(1, panel.size());
        assertEquals("Tarjeta", panel.get(0).productoNombre());
    }

    @Test
    void asignaUnProductoAUnaSucursalDelMismoNegocio() {
        repositorio.registrar(new Inventario(1L, 10L, 20L, 5));

        Inventario nuevo = service.asignar(20L, 11L, 4, PROPIETARIO);

        assertEquals(11L, nuevo.getSucursalId());
        assertEquals(4, nuevo.getExistencia());
    }

    @Test
    void noAsignaDuplicadosNiRecursosAjenos() {
        repositorio.registrar(new Inventario(1L, 10L, 20L, 5));

        assertThrows(InventarioYaExisteException.class, () -> service.asignar(20L, 10L, 0, PROPIETARIO));
        assertThrows(ProductoOSucursalNoEncontradoException.class, () -> service.asignar(20L, 90L, 0, PROPIETARIO));
        assertThrows(ProductoOSucursalNoEncontradoException.class, () -> service.asignar(20L, 11L, 0, 999L));
        assertThrows(ProductoOSucursalNoEncontradoException.class, () -> service.asignar(77L, 11L, 0, PROPIETARIO));
    }

    /** Repositorio en memoria; "buscarParaAjuste" solo entrega registros del propietario de prueba. */
    private static final class FakeInventarios implements InventarioRepositorioPort {

        final Map<Long, Inventario> guardados = new HashMap<>();

        void registrar(Inventario inventario) {
            guardados.put(inventario.getId(), inventario);
        }

        @Override
        public Inventario guardar(Inventario inventario) {
            guardados.put(inventario.getId(), inventario);
            return inventario;
        }

        @Override
        public Optional<Inventario> buscarParaAjuste(Long inventarioId, Long usuarioId) {
            Inventario original = guardados.get(inventarioId);
            if (original == null || !PROPIETARIO.equals(usuarioId)) {
                return Optional.empty();
            }
            // Copia: el dominio muta la copia y solo "guardar" confirma el cambio, como con la BD.
            return Optional.of(new Inventario(original.getId(), original.getSucursalId(),
                original.getProductoId(), original.getExistencia()));
        }

        @Override
        public Optional<Long> negocioDeProducto(Long productoId, Long usuarioId) {
            return PROPIETARIO.equals(usuarioId) && productoId == 20L ? Optional.of(NEGOCIO) : Optional.empty();
        }

        @Override
        public Optional<Long> negocioDeSucursal(Long sucursalId, Long usuarioId) {
            if (!PROPIETARIO.equals(usuarioId)) {
                return Optional.empty();
            }
            if (sucursalId == 10L || sucursalId == 11L) {
                return Optional.of(NEGOCIO);
            }
            return sucursalId == 90L ? Optional.of(999L) : Optional.empty();
        }

        @Override
        public boolean existe(Long sucursalId, Long productoId) {
            return guardados.values().stream()
                .anyMatch(i -> i.getSucursalId().equals(sucursalId) && i.getProductoId().equals(productoId));
        }

        @Override
        public List<ExistenciaProducto> listarPanel(Long negocioId, Long sucursalId, Long usuarioId) {
            return List.of(new ExistenciaProducto(1L, 20L, "Tarjeta", 10L, "Centro", 5));
        }
    }
}
