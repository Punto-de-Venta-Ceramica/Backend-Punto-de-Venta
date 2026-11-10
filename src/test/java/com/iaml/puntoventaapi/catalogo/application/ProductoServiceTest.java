package com.iaml.puntoventaapi.catalogo.application;

import com.iaml.puntoventaapi.catalogo.domain.ClasificacionProducto;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.FiltroProductos;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.ProductoDetalle;
import com.iaml.puntoventaapi.catalogo.domain.exception.CategoriaNoEncontradaException;
import com.iaml.puntoventaapi.catalogo.domain.exception.NegocioNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoInvalidoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.Categoria;
import com.iaml.puntoventaapi.catalogo.domain.port.AlmacenImagenesPort;
import com.iaml.puntoventaapi.catalogo.domain.port.CategoriaRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.DescuentoRepositorioPort;
import com.iaml.puntoventaapi.catalogo.domain.port.ProductoRepositorioPort;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    private static final String URL_IMG_1 = "https://res.cloudinary.com/ecbb3tac/image/upload/v1/productos/jarron_1.jpg";
    private static final String URL_IMG_2 = "https://res.cloudinary.com/ecbb3tac/image/upload/v1/productos/jarron_2.jpg";

    private final FakeProductos productos = new FakeProductos();
    private final FakeDescuentos descuentos = new FakeDescuentos();
    private final FakeAlmacenImagenes almacenImagenes = new FakeAlmacenImagenes();
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
            inventarioRegistrado::put,
            almacenImagenes);
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
    void editarSinDatosDeDescuentoNoTocaElDescuentoNiElInventario() {
        ProductoDetalle creado = service.crear(comando(
            new DescuentoProductoCommand(6, null, new BigDecimal("10.00")), null), PROPIETARIO);
        Long productoId = creado.producto().getId();
        Long descuentoId = creado.descuento().getId();
        Map<Long, Integer> antes = Map.copyOf(inventarioRegistrado.get(productoId));

        ProductoDetalle editado = service.editar(new EditarProductoCommand(productoId, "Tarjeta XL",
            new BigDecimal("25.50"), CATEGORIA, null), PROPIETARIO);

        // Los descuentos se gestionan en su propio módulo: editar el producto sin descuento no los borra.
        assertEquals("Tarjeta XL", editado.producto().getNombre());
        assertEquals(descuentoId, editado.descuento().getId());
        assertEquals(descuentoId, descuentos.vinculos.get(productoId));
        assertTrue(descuentos.guardados.containsKey(descuentoId));
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
    void editarNoModificaUnDescuentoCompartidoSinoQueCreaUnoPropio() {
        Long a = service.crear(comando(null, null), PROPIETARIO).producto().getId();
        Long b = service.crear(comando(null, null), PROPIETARIO).producto().getId();
        Descuento compartido = descuentos.guardar(new Descuento(null, NEGOCIO, "Mayoreo", 6, null,
            new BigDecimal("10.00"), true));
        descuentos.vincularAProducto(compartido.getId(), a);
        descuentos.vinculos.put(b, compartido.getId());

        ProductoDetalle editado = service.editar(new EditarProductoCommand(a, "Tarjeta",
            new BigDecimal("25.50"), CATEGORIA, new DescuentoProductoCommand(20, null, new BigDecimal("30.00"))),
            PROPIETARIO);

        assertNotEquals(compartido.getId(), editado.descuento().getId());
        assertEquals(30, editado.descuento().getPorcentaje().intValue());
        Descuento intacto = descuentos.guardados.get(compartido.getId());
        assertEquals(6, intacto.getCantidadMinima());
        assertEquals(10, intacto.getPorcentaje().intValue());
        assertEquals(compartido.getId(), descuentos.vinculos.get(b));
    }

    @Test
    void editarSinDatosDeDescuentoConservaUnoAsignadoACategoriaOArea() {
        Long productoId = service.crear(comando(null, null), PROPIETARIO).producto().getId();
        Descuento deCategoria = descuentos.guardar(new Descuento(null, NEGOCIO, "Categoría", 1, null,
            new BigDecimal("5.00"), true));
        descuentos.vincularAProducto(deCategoria.getId(), productoId);
        descuentos.conOtrosDestinos.add(deCategoria.getId());

        ProductoDetalle editado = service.editar(new EditarProductoCommand(productoId, "Tarjeta",
            new BigDecimal("25.50"), CATEGORIA, null), PROPIETARIO);

        assertTrue(descuentos.guardados.containsKey(deCategoria.getId()));
        assertEquals(deCategoria.getId(), editado.descuento().getId());
    }

    @Test
    void sinDescuentoPropioElProductoMuestraElQueHeredaDeSuCategoria() {
        Descuento deCategoria = descuentos.guardar(new Descuento(null, NEGOCIO, "Categoría", 1, null,
            new BigDecimal("5.00"), true));
        Long productoId = service.crear(comando(null, null), PROPIETARIO).producto().getId();
        descuentos.heredados.put(productoId, deCategoria.getId());

        assertEquals(deCategoria.getId(), service.consultar(productoId, PROPIETARIO).descuento().getId());
    }

    @Test
    void guardarSinCambiosElDescuentoHeredadoNoCreaUnaCopiaPropia() {
        Descuento deCategoria = descuentos.guardar(new Descuento(null, NEGOCIO, "Categoría", 6, null,
            new BigDecimal("10.00"), true));
        Long productoId = service.crear(comando(null, null), PROPIETARIO).producto().getId();
        descuentos.heredados.put(productoId, deCategoria.getId());

        // El formulario reenvía tal cual el descuento heredado que mostró.
        ProductoDetalle editado = service.editar(new EditarProductoCommand(productoId, "Tarjeta",
            new BigDecimal("30.00"), CATEGORIA, new DescuentoProductoCommand(6, null, new BigDecimal("10.0"))),
            PROPIETARIO);

        assertEquals(deCategoria.getId(), editado.descuento().getId());
        assertFalse(descuentos.vinculos.containsKey(productoId));
        assertEquals(1, descuentos.guardados.size());
    }

    @Test
    void cambiarElDescuentoHeredadoCreaUnoPropioQueLoSustituye() {
        Descuento deCategoria = descuentos.guardar(new Descuento(null, NEGOCIO, "Categoría", 6, null,
            new BigDecimal("10.00"), true));
        Long productoId = service.crear(comando(null, null), PROPIETARIO).producto().getId();
        descuentos.heredados.put(productoId, deCategoria.getId());

        ProductoDetalle editado = service.editar(new EditarProductoCommand(productoId, "Tarjeta",
            new BigDecimal("30.00"), CATEGORIA, new DescuentoProductoCommand(6, null, new BigDecimal("25.00"))),
            PROPIETARIO);

        assertNotEquals(deCategoria.getId(), editado.descuento().getId());
        assertEquals(25, editado.descuento().getPorcentaje().intValue());
        assertEquals(10, descuentos.guardados.get(deCategoria.getId()).getPorcentaje().intValue());
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
    void listaSoloProductosActivosEnUnaPagina() {
        service.crear(comando(null, null), PROPIETARIO);
        Long baja = service.crear(comando(null, null), PROPIETARIO).producto().getId();
        service.crear(comando(null, null), PROPIETARIO);
        service.eliminar(baja, PROPIETARIO);

        Pagina<Producto> pagina = service.listar(NEGOCIO, PROPIETARIO, FiltroProductos.SIN_FILTROS, 0, 30);

        assertEquals(2, pagina.contenido().size());
        assertEquals(2, pagina.totalElementos());
        assertThrows(NegocioNoEncontradoException.class,
            () -> service.listar(NEGOCIO_AJENO, PROPIETARIO, FiltroProductos.SIN_FILTROS, 0, 30));
    }

    @Test
    void listaPaginaDeAcuerdoAlTamanioSolicitado() {
        for (int i = 0; i < 5; i++) {
            service.crear(comando(null, null), PROPIETARIO);
        }

        Pagina<Producto> segunda = service.listar(NEGOCIO, PROPIETARIO, FiltroProductos.SIN_FILTROS, 1, 2);

        assertEquals(2, segunda.contenido().size());
        assertEquals(5, segunda.totalElementos());
        assertEquals(3, segunda.totalPaginas());
        assertEquals(false, segunda.ultima());
    }

    @Test
    void creaYConsultaProductoConImagenDeCloudinary() {
        ProductoDetalle creado = service.crear(new CrearProductoCommand("Jarrón Barro",
            new BigDecimal("180.00"), CATEGORIA, URL_IMG_1, "productos/jarron_1", null, null), PROPIETARIO);

        assertEquals(URL_IMG_1, creado.producto().getImagenUrl());
        assertEquals("productos/jarron_1", creado.producto().getImagenPublicId());
        assertTrue(almacenImagenes.eliminadas.isEmpty());

        ProductoDetalle consultado = service.consultar(creado.producto().getId(), PROPIETARIO);
        assertEquals(URL_IMG_1, consultado.producto().getImagenUrl());
        assertEquals("productos/jarron_1", consultado.producto().getImagenPublicId());
    }

    @Test
    void rechazaImagenUrlQueNoSeaHttpsNiDeCloudinary() {
        assertThrows(ProductoInvalidoException.class, () -> service.crear(new CrearProductoCommand("Jarrón",
            BigDecimal.TEN, CATEGORIA, "http://res.cloudinary.com/ecbb3tac/image/upload/v1/img.jpg", "img",
            null, null), PROPIETARIO));
        assertThrows(ProductoInvalidoException.class, () -> service.crear(new CrearProductoCommand("Jarrón",
            BigDecimal.TEN, CATEGORIA, "https://mi-sitio.com/img.jpg", "img",
            null, null), PROPIETARIO));
        assertTrue(productos.guardados.isEmpty());
    }

    @Test
    void alEditarConUnaImagenDistintaBorraLaAnteriorEnCloudinary() {
        Long id = service.crear(new CrearProductoCommand("Jarrón", new BigDecimal("100.00"), CATEGORIA,
            URL_IMG_1, "productos/jarron_1", null, null), PROPIETARIO).producto().getId();

        ProductoDetalle editado = service.editar(new EditarProductoCommand(id, "Jarrón Editado",
            new BigDecimal("120.00"), CATEGORIA, URL_IMG_2, "productos/jarron_2", null), PROPIETARIO);

        assertEquals(URL_IMG_2, editado.producto().getImagenUrl());
        assertEquals("productos/jarron_2", editado.producto().getImagenPublicId());
        assertEquals(List.of("productos/jarron_1"), almacenImagenes.eliminadas);
    }

    @Test
    void alEditarConservandoLaMismaImagenNoLaBorraEnCloudinary() {
        Long id = service.crear(new CrearProductoCommand("Jarrón", new BigDecimal("100.00"), CATEGORIA,
            URL_IMG_1, "productos/jarron_1", null, null), PROPIETARIO).producto().getId();

        service.editar(new EditarProductoCommand(id, "Jarrón Renombrado",
            new BigDecimal("120.00"), CATEGORIA, URL_IMG_1, "productos/jarron_1", null), PROPIETARIO);

        assertTrue(almacenImagenes.eliminadas.isEmpty());
    }

    @Test
    void alEditarQuitandoLaImagenBorraLaAnteriorEnCloudinary() {
        Long id = service.crear(new CrearProductoCommand("Jarrón", new BigDecimal("100.00"), CATEGORIA,
            URL_IMG_1, "productos/jarron_1", null, null), PROPIETARIO).producto().getId();

        ProductoDetalle editado = service.editar(new EditarProductoCommand(id, "Jarrón Sin Imagen",
            new BigDecimal("120.00"), CATEGORIA, null, null, null), PROPIETARIO);

        assertNull(editado.producto().getImagenUrl());
        assertNull(editado.producto().getImagenPublicId());
        assertEquals(List.of("productos/jarron_1"), almacenImagenes.eliminadas);
    }

    @Test
    void alEliminarUnProductoBorraSuImagenEnCloudinary() {
        Long id = service.crear(new CrearProductoCommand("Jarrón", new BigDecimal("100.00"), CATEGORIA,
            URL_IMG_1, "productos/jarron_1", null, null), PROPIETARIO).producto().getId();

        service.eliminar(id, PROPIETARIO);

        assertFalse(productos.guardados.get(id).isActivo());
        assertEquals(List.of("productos/jarron_1"), almacenImagenes.eliminadas);
    }

    @Test
    void siCloudinaryFallaAlEditarOEliminarNoRompeLaOperacion() {
        Long id = service.crear(new CrearProductoCommand("Jarrón", new BigDecimal("100.00"), CATEGORIA,
            URL_IMG_1, "productos/jarron_1", null, null), PROPIETARIO).producto().getId();
        almacenImagenes.fallar = true;

        ProductoDetalle editado = service.editar(new EditarProductoCommand(id, "Jarrón 2",
            new BigDecimal("150.00"), CATEGORIA, URL_IMG_2, "productos/jarron_2", null), PROPIETARIO);
        assertEquals("Jarrón 2", editado.producto().getNombre());
        assertEquals("productos/jarron_2", editado.producto().getImagenPublicId());

        service.eliminar(id, PROPIETARIO);
        assertFalse(productos.guardados.get(id).isActivo());
    }

    /** Adaptador simulado de almacenamiento de imágenes (Cloudinary). */
    private static final class FakeAlmacenImagenes implements AlmacenImagenesPort {

        final List<String> eliminadas = new ArrayList<>();
        boolean fallar = false;

        @Override
        public void eliminar(String publicId) {
            if (fallar) {
                throw new RuntimeException("Fallo simulado de comunicación con Cloudinary");
            }
            eliminadas.add(publicId);
        }
    }

    /** Repositorio en memoria. Solo el propietario de prueba ve sus productos activos. */
    private static final class FakeProductos implements ProductoRepositorioPort {

        final Map<Long, Producto> guardados = new LinkedHashMap<>();
        private long siguienteId = 1;

        @Override
        public Producto guardar(Producto producto) {
            Long id = producto.getId() != null ? producto.getId() : siguienteId++;
            Producto persistido = new Producto(id, producto.getCategoriaId(), producto.getNombre(),
                producto.getPrecio(), producto.getImagenUrl(), producto.getImagenPublicId(), producto.isActivo());
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
                producto.getPrecio(), producto.getImagenUrl(), producto.getImagenPublicId(), true));
        }

        @Override
        public List<Producto> listarPorNegocio(Long negocioId) {
            return new ArrayList<>(guardados.values().stream().filter(Producto::isActivo).toList());
        }

        @Override
        public Pagina<Producto> listarPagina(Long negocioId, FiltroProductos filtro, int pagina, int tamanio) {
            List<Producto> activos = guardados.values().stream().filter(Producto::isActivo).toList();
            int desde = Math.min(pagina * tamanio, activos.size());
            int hasta = Math.min(desde + tamanio, activos.size());
            return new Pagina<>(activos.subList(desde, hasta), pagina, tamanio, activos.size());
        }

        @Override
        public Optional<ClasificacionProducto> buscarClasificacion(Long categoriaId) {
            return Optional.empty();
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

        /** Descuentos que además están asignados a una categoría o un área (no son propios de un producto). */
        final Set<Long> conOtrosDestinos = new HashSet<>();
        /** Descuentos que aplican a un producto por su categoría o área: producto -> descuento. */
        final Map<Long, Long> heredados = new HashMap<>();

        @Override
        public Optional<Descuento> buscarActivoPorProducto(Long productoId) {
            Long descuentoId = vinculos.getOrDefault(productoId, heredados.get(productoId));
            return descuentoId == null ? Optional.empty() : Optional.of(copia(guardados.get(descuentoId)));
        }

        @Override
        public Map<Long, Descuento> buscarActivosPorProductos(Collection<Long> productoIds) {
            Map<Long, Descuento> resultado = new HashMap<>();
            productoIds.forEach(id -> buscarActivoPorProducto(id).ifPresent(d -> resultado.put(id, d)));
            return resultado;
        }

        @Override
        public Optional<Descuento> buscarPropioActivoDeProducto(Long productoId) {
            Long descuentoId = vinculos.get(productoId);
            return descuentoId != null && esPropio(descuentoId)
                ? Optional.of(copia(guardados.get(descuentoId))) : Optional.empty();
        }

        private boolean esPropio(Long descuentoId) {
            return !conOtrosDestinos.contains(descuentoId)
                && vinculos.values().stream().filter(descuentoId::equals).count() == 1;
        }

        private static Descuento copia(Descuento d) {
            return new Descuento(d.getId(), d.getNegocioId(), d.getNombre(), d.getCantidadMinima(),
                d.getCantidadMaxima(), d.getPorcentaje(), d.isActivo());
        }
    }
}
