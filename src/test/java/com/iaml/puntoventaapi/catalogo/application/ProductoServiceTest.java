package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.ProductoDetalle;
import com.iaml.puntoventaapi.catalogo.domain.exception.CategoriaNoEncontradaException;
import com.iaml.puntoventaapi.catalogo.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.Categoria;
import com.iaml.puntoventaapi.catalogo.domain.port.CategoriaRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductoServiceTest {

    private static final Long PROPIETARIO = 7L;
    private static final Long NEGOCIO = 100L;
    private static final Long NEGOCIO_AJENO = 200L;
    private static final Long CATEGORIA = 1L;
    private static final Long CATEGORIA_OTRO_NEGOCIO_PROPIO = 2L;
    private static final Long CATEGORIA_AJENA = 3L;

    private final FakeProductos productos = new FakeProductos();
    private final FakeDescuentos descuentos = new FakeDescuentos();
    private final Map<Long, Map<Long, Integer>> inventarioRegistrado = new LinkedHashMap<>();
    private List<Long> sucursalesDelNegocio;
    private ProductoService service;

    @BeforeEach
    void preparar() {
        sucursalesDelNegocio = List.of(10L);
        // Puertos sin estado propio sustituidos por lambdas.
        service = new ProductoService(productos,
            new FakeCategorias(),
            descuentos,
            (negocioId, usuarioId) -> PROPIETARIO.equals(usuarioId) && (NEGOCIO.equals(negocioId) || negocioId == 101L),
            (negocioId, usuarioId) -> sucursalesDelNegocio,
            inventarioRegistrado::put);
    }

    private CrearProductoCommand comando(DescuentoProductoCommand descuento, List<ExistenciaInicialCommand> existencias) {
        return new CrearProductoCommand("Tarjeta", new BigDecimal("25.50"), CATEGORIA, descuento, existencias);
    }

    @Test
    void conUnaSolaSucursalElProductoSeAsignaAEllaConExistenciaCero() {
        ProductoDetalle detalle = service.crear(comando(null, null), PROPIETARIO);

        assertNotNull(detalle.producto().getId());
        assertNull(detalle.descuento());
        assertEquals(Map.of(10L, 0), inventarioRegistrado.get(detalle.producto().getId()));
    }

    @Test
    void conVariasSucursalesExigeElegirEnCuales() {
        sucursalesDelNegocio = List.of(10L, 11L);

        assertThrows(ProductoInvalidoException.class, () -> service.crear(comando(null, null), PROPIETARIO));
        assertTrue(productos.guardados.isEmpty());
    }

    @Test
    void registraExistenciaInicialSoloEnLasSucursalesElegidas() {
        sucursalesDelNegocio = List.of(10L, 11L, 12L);

        ProductoDetalle detalle = service.crear(comando(null,
            List.of(new ExistenciaInicialCommand(10L, 5), new ExistenciaInicialCommand(12L, 8))), PROPIETARIO);

        assertEquals(Map.of(10L, 5, 12L, 8), inventarioRegistrado.get(detalle.producto().getId()));
    }

    @Test
    void rechazaSucursalAjenaRepetidaOExistenciaNegativa() {
        sucursalesDelNegocio = List.of(10L, 11L);

        assertThrows(ProductoInvalidoException.class, () -> service.crear(comando(null,
            List.of(new ExistenciaInicialCommand(99L, 1))), PROPIETARIO));
        assertThrows(ProductoInvalidoException.class, () -> service.crear(comando(null,
            List.of(new ExistenciaInicialCommand(10L, 1), new ExistenciaInicialCommand(10L, 2))), PROPIETARIO));
        assertThrows(ProductoInvalidoException.class, () -> service.crear(comando(null,
            List.of(new ExistenciaInicialCommand(10L, -1))), PROPIETARIO));
    }

    @Test
    void rechazaNegocioSinSucursales() {
        sucursalesDelNegocio = List.of();

        assertThrows(ProductoInvalidoException.class, () -> service.crear(comando(null, null), PROPIETARIO));
    }

    @Test
    void categoriaAjenaOInexistenteSeTrataComoNoEncontrada() {
        CrearProductoCommand ajena = new CrearProductoCommand("A", BigDecimal.TEN, CATEGORIA_AJENA, null, null);
        CrearProductoCommand inexistente = new CrearProductoCommand("A", BigDecimal.TEN, 999L, null, null);

        assertThrows(CategoriaNoEncontradaException.class, () -> service.crear(ajena, PROPIETARIO));
        assertThrows(CategoriaNoEncontradaException.class, () -> service.crear(inexistente, PROPIETARIO));
        assertTrue(productos.guardados.isEmpty());
    }

    @Test
    void creaYVinculaElDescuentoDelProducto() {
        ProductoDetalle detalle = service.crear(comando(
            new DescuentoProductoCommand(6, null, new BigDecimal("10.00")), null), PROPIETARIO);

        assertNotNull(detalle.descuento());
        assertEquals(6, detalle.descuento().getCantidadMinima());
        assertEquals(detalle.descuento().getId(), descuentos.vinculos.get(detalle.producto().getId()));
    }

    @Test
    void elNombreDelDescuentoNuncaSuperaElLimiteDeLaTabla() {
        CrearProductoCommand largo = new CrearProductoCommand("N".repeat(150), BigDecimal.TEN, CATEGORIA,
            new DescuentoProductoCommand(2, null, BigDecimal.TEN), null);

        ProductoDetalle detalle = service.crear(largo, PROPIETARIO);

        assertEquals(120, detalle.descuento().getNombre().length());
    }

    @Test
    void editaDatosYActualizaElDescuentoEnSitio() {
        ProductoDetalle creado = service.crear(comando(
            new DescuentoProductoCommand(6, null, new BigDecimal("10.00")), null), PROPIETARIO);
        Long productoId = creado.producto().getId();
        Long descuentoId = creado.descuento().getId();

        ProductoDetalle editado = service.editar(new EditarProductoCommand(productoId, "Tarjeta XL",
            new BigDecimal("30.00"), CATEGORIA, new DescuentoProductoCommand(12, 20, new BigDecimal("15.00"))),
            PROPIETARIO);

        assertEquals("Tarjeta XL", editado.producto().getNombre());
        assertEquals(new BigDecimal("30.00"), editado.producto().getPrecio());
        assertEquals(descuentoId, editado.descuento().getId());
        assertEquals(12, editado.descuento().getCantidadMinima());
        assertEquals(20, editado.descuento().getCantidadMaxima());
    }

    @Test
    void editarSinDescuentoLoQuitaYNoTocaElInventario() {
        ProductoDetalle creado = service.crear(comando(
            new DescuentoProductoCommand(6, null, new BigDecimal("10.00")), null), PROPIETARIO);
        Long productoId = creado.producto().getId();
        Map<Long, Integer> antes = Map.copyOf(inventarioRegistrado.get(productoId));

        ProductoDetalle editado = service.editar(new EditarProductoCommand(productoId, "Tarjeta",
            new BigDecimal("25.50"), CATEGORIA, null), PROPIETARIO);

        assertNull(editado.descuento());
        assertFalse(descuentos.vinculos.containsKey(productoId));
        assertEquals(antes, inventarioRegistrado.get(productoId));
        assertEquals(1, inventarioRegistrado.size());
    }

    @Test
    void editarCreaElDescuentoSiElProductoNoTeniaUno() {
        Long productoId = service.crear(comando(null, null), PROPIETARIO).producto().getId();

        ProductoDetalle editado = service.editar(new EditarProductoCommand(productoId, "Tarjeta",
            new BigDecimal("25.50"), CATEGORIA, new DescuentoProductoCommand(3, null, new BigDecimal("5.00"))),
            PROPIETARIO);

        assertNotNull(editado.descuento());
        assertEquals(editado.descuento().getId(), descuentos.vinculos.get(productoId));
    }

    @Test
    void noPermiteMoverElProductoACategoriaDeOtroNegocio() {
        Long productoId = service.crear(comando(null, null), PROPIETARIO).producto().getId();

        assertThrows(ProductoInvalidoException.class, () -> service.editar(new EditarProductoCommand(productoId,
            "Tarjeta", BigDecimal.TEN, CATEGORIA_OTRO_NEGOCIO_PROPIO, null), PROPIETARIO));
        assertThrows(CategoriaNoEncontradaException.class, () -> service.editar(new EditarProductoCommand(productoId,
            "Tarjeta", BigDecimal.TEN, CATEGORIA_AJENA, null), PROPIETARIO));
        assertEquals(CATEGORIA, productos.guardados.get(productoId).getCategoriaId());
    }

    @Test
    void eliminarEsBajaLogicaYElProductoDejaDeConsultarse() {
        Long productoId = service.crear(comando(null, null), PROPIETARIO).producto().getId();

        service.eliminar(productoId, PROPIETARIO);

        assertFalse(productos.guardados.get(productoId).isActivo());
        assertThrows(ProductoNoEncontradoException.class, () -> service.consultar(productoId, PROPIETARIO));
        assertThrows(ProductoNoEncontradoException.class, () -> service.eliminar(productoId, PROPIETARIO));
    }

    @Test
    void usuarioAjenoNoPuedeEditarNiEliminar() {
        Long productoId = service.crear(comando(null, null), PROPIETARIO).producto().getId();

        assertThrows(ProductoNoEncontradoException.class, () -> service.eliminar(productoId, 999L));
        assertThrows(ProductoNoEncontradoException.class, () -> service.editar(new EditarProductoCommand(productoId,
            "X", BigDecimal.TEN, CATEGORIA, null), 999L));
    }

    @Test
    void listaSoloProductosActivosConSuDescuento() {
        Long conDescuento = service.crear(comando(
            new DescuentoProductoCommand(6, null, new BigDecimal("10.00")), null), PROPIETARIO).producto().getId();
        Long baja = service.crear(comando(null, null), PROPIETARIO).producto().getId();
        service.crear(comando(null, null), PROPIETARIO);
        service.eliminar(baja, PROPIETARIO);

        List<ProductoDetalle> lista = service.listar(NEGOCIO, PROPIETARIO);

        assertEquals(2, lista.size());
        assertNotNull(lista.stream().filter(d -> d.producto().getId().equals(conDescuento)).findFirst()
            .orElseThrow().descuento());
        assertThrows(NegocioNoEncontradoException.class, () -> service.listar(NEGOCIO_AJENO, PROPIETARIO));
    }

    /** Repositorio en memoria. Solo el propietario de prueba ve sus productos activos. */
    private static final class FakeProductos implements ProductoRepositorioPort {

        final Map<Long, Producto> guardados = new LinkedHashMap<>();
        private long siguienteId = 1;

        @Override
        public Producto guardar(Producto producto) {
            Long id = producto.getId() != null ? producto.getId() : siguienteId++;
            Producto persistido = new Producto(id, producto.getCategoriaId(), producto.getNombre(),
                producto.getPrecio(), producto.isActivo());
            guardados.put(id, persistido);
            return persistido;
        }

        @Override
        public Optional<Producto> buscarPorId(Long id, Long usuarioId) {
            Producto producto = guardados.get(id);
            if (producto == null || !producto.isActivo() || !PROPIETARIO.equals(usuarioId)) {
                return Optional.empty();
            }
            return Optional.of(new Producto(producto.getId(), producto.getCategoriaId(), producto.getNombre(),
                producto.getPrecio(), true));
        }

        @Override
        public List<Producto> listarPorNegocio(Long negocioId) {
            return new ArrayList<>(guardados.values().stream().filter(Producto::isActivo).toList());
        }

        @Override
        public boolean existenPorCategoria(Long categoriaId) {
            return guardados.values().stream().anyMatch(p -> p.getCategoriaId().equals(categoriaId));
        }

        @Override
        public boolean existenPorArea(Long areaId) {
            return false;
        }
    }

    /** Categorías de prueba: solo resuelven el negocio de cada una. */
    private static final class FakeCategorias implements CategoriaRepositorioPort {

        private static final Map<Long, Long> NEGOCIO_POR_CATEGORIA = Map.of(CATEGORIA, NEGOCIO,
            CATEGORIA_OTRO_NEGOCIO_PROPIO, 101L, CATEGORIA_AJENA, NEGOCIO_AJENO);

        @Override
        public Optional<Long> buscarNegocioId(Long categoriaId) {
            return Optional.ofNullable(NEGOCIO_POR_CATEGORIA.get(categoriaId));
        }

        @Override
        public Optional<Long> buscarNegocioIdParaActualizar(Long categoriaId) {
            return buscarNegocioId(categoriaId);
        }

        @Override
        public Categoria guardar(Categoria categoria, Long negocioId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Categoria> buscarPorId(Long categoriaId, Long areaId, Long negocioId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Categoria> listarPorArea(Long areaId, Long negocioId) {
            throw new UnsupportedOperationException();
        }
    }

    /** Repositorio en memoria de descuentos y de la tabla de enlace producto -> descuento. */
    private static final class FakeDescuentos implements DescuentoRepositorioPort {

        final Map<Long, Descuento> guardados = new HashMap<>();
        final Map<Long, Long> vinculos = new HashMap<>();
        private long siguienteId = 1;

        @Override
        public Descuento guardar(Descuento descuento) {
            Long id = descuento.getId() != null ? descuento.getId() : siguienteId++;
            Descuento persistido = new Descuento(id, descuento.getNegocioId(), descuento.getNombre(),
                descuento.getCantidadMinima(), descuento.getCantidadMaxima(), descuento.getPorcentaje(),
                descuento.isActivo());
            guardados.put(id, persistido);
            return persistido;
        }

        @Override
        public void vincularAProducto(Long descuentoId, Long productoId) {
            vinculos.put(productoId, descuentoId);
        }

        @Override
        public Optional<Descuento> buscarActivoPorProducto(Long productoId) {
            Long descuentoId = vinculos.get(productoId);
            return descuentoId == null ? Optional.empty() : Optional.of(copia(guardados.get(descuentoId)));
        }

        @Override
        public Map<Long, Descuento> buscarActivosPorProductos(Collection<Long> productoIds) {
            Map<Long, Descuento> resultado = new HashMap<>();
            productoIds.forEach(id -> buscarActivoPorProducto(id).ifPresent(d -> resultado.put(id, d)));
            return resultado;
        }

        @Override
        public void quitarDeProducto(Long productoId) {
            Long descuentoId = vinculos.remove(productoId);
            if (descuentoId != null && !vinculos.containsValue(descuentoId)) {
                guardados.remove(descuentoId);
            }
        }

        private static Descuento copia(Descuento d) {
            return new Descuento(d.getId(), d.getNegocioId(), d.getNombre(), d.getCantidadMinima(),
                d.getCantidadMaxima(), d.getPorcentaje(), d.isActivo());
        }
    }
}
