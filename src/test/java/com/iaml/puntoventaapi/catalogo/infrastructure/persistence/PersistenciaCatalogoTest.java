package com.iaml.puntoventaapi.catalogo.infrastructure.persistence;

import com.iaml.puntoventaapi.catalogo.domain.Area;
import com.iaml.puntoventaapi.catalogo.domain.AsignacionesDescuento;
import com.iaml.puntoventaapi.catalogo.domain.Categoria;
import com.iaml.puntoventaapi.catalogo.domain.ClasificacionProducto;
import com.iaml.puntoventaapi.catalogo.domain.Descuento;
import com.iaml.puntoventaapi.catalogo.domain.DestinoDescuento;
import com.iaml.puntoventaapi.catalogo.domain.FiltroProductos;
import com.iaml.puntoventaapi.catalogo.domain.Producto;
import com.iaml.puntoventaapi.catalogo.domain.exception.CatalogoNoEncontradoException;
import com.iaml.puntoventaapi.catalogo.domain.exception.ProductoNoEncontradoException;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import com.iaml.puntoventaapi.shared.domain.Pagina;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersistenciaCatalogoTest {

    private static final Instant FECHA = Instant.parse("2026-01-01T00:00:00Z");

    private EntityManager em;
    private NegocioEntity negocio;
    private AreaEntity area;
    private CategoriaEntity categoria;

    @BeforeEach
    void preparar() {
        em = mock(EntityManager.class);
        negocio = new NegocioEntity();
        ReflectionTestUtils.setField(negocio, "id", 1L);
        area = areaEntity(2L, "Cocina", true, null);
        categoria = categoriaEntity(3L, "Café", true, null);
        when(em.getReference(eq(NegocioEntity.class), anyLong())).thenReturn(negocio);
        when(em.getReference(eq(AreaEntity.class), anyLong())).thenReturn(area);
        when(em.getReference(eq(CategoriaEntity.class), anyLong())).thenReturn(categoria);
        when(em.getReference(eq(ProductoEntity.class), anyLong())).thenReturn(productoEntity(5L, true));
    }

    // ---------- Fábricas de entidades ----------

    private AreaEntity areaEntity(Long id, String nombre, boolean activo, Instant baja) {
        AreaEntity entity = new AreaEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setNegocio(negocio);
        entity.setNombre(nombre);
        entity.setDescripcion("d");
        entity.setActivo(activo);
        entity.setFechaEliminacion(baja);
        return entity;
    }

    private CategoriaEntity categoriaEntity(Long id, String nombre, boolean activo, Instant baja) {
        CategoriaEntity entity = new CategoriaEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setArea(area == null ? areaEntity(2L, "Cocina", true, null) : area);
        entity.setNombre(nombre);
        entity.setDescripcion("d");
        entity.setActivo(activo);
        entity.setFechaEliminacion(baja);
        return entity;
    }

    private ProductoEntity productoEntity(Long id, boolean activo) {
        ProductoEntity entity = new ProductoEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setCategoria(categoria == null ? categoriaEntity(3L, "Café", true, null) : categoria);
        entity.setNombre("Taza");
        entity.setPrecio(BigDecimal.TEN);
        entity.setActivo(activo);
        entity.setImagenUrl("https://res.cloudinary.com/demo/a.jpg");
        entity.setImagenPublicId("pid");
        return entity;
    }

    private DescuentoEntity descuentoEntity(Long id, int minima, boolean activo) {
        DescuentoEntity entity = new DescuentoEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setNegocio(negocio);
        entity.setNombre("Mayoreo " + id);
        entity.setCantidadMinima(minima);
        entity.setCantidadMaxima(null);
        entity.setDescuento(new BigDecimal("10.00"));
        entity.setActivo(activo);
        return entity;
    }

    // ---------- Áreas ----------

    @Test
    void areaMapperYEntidad() {
        Area dominio = AreaMapper.toDomain(areaEntity(2L, "Cocina", false, FECHA));
        AreaEntity entity = AreaMapper.toEntity(new Area(null, 1L, "Barra", "x"), negocio);

        assertFalse(dominio.isActivo());
        assertEquals(FECHA, dominio.getFechaEliminacion());
        assertEquals("Barra", entity.getNombre());
        assertEquals("x", entity.getDescripcion());
        assertTrue(entity.isActivo());
        assertNull(entity.getFechaEliminacion());
        assertSame(negocio, entity.getNegocio());
    }

    @Test
    void areaAdapter() {
        AreaJpaRepository jpa = mock(AreaJpaRepository.class);
        AreaPersistenceAdapter adapter = new AreaPersistenceAdapter(jpa, em);
        when(jpa.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(jpa.buscarActivaPorId(2L, 1L)).thenReturn(Optional.of(area));
        when(jpa.buscarActivaPorId(9L, 1L)).thenReturn(Optional.empty());
        when(jpa.bloquearActivaPorId(2L, 1L)).thenReturn(Optional.of(area));
        when(jpa.listarActivasPorNegocio(1L)).thenReturn(List.of(area));

        assertEquals("Nueva", adapter.guardar(new Area(null, 1L, "Nueva", null)).getNombre());
        assertEquals("Editada", adapter.guardar(new Area(2L, 1L, "Editada", null)).getNombre());
        assertThrows(CatalogoNoEncontradoException.class, () -> adapter.guardar(new Area(9L, 1L, "X", null)));
        assertEquals(2L, adapter.buscarPorId(2L, 1L).orElseThrow().getId());
        assertEquals(2L, adapter.buscarPorIdParaActualizar(2L, 1L).orElseThrow().getId());
        assertEquals(1, adapter.listarPorNegocio(1L).size());
    }

    // ---------- Categorías ----------

    @Test
    void categoriaMapperYEntidad() {
        Categoria dominio = CategoriaMapper.toDomain(categoriaEntity(3L, "Café", false, FECHA));
        CategoriaEntity entity = CategoriaMapper.toEntity(new Categoria(null, 2L, "Té", "x"), area);

        assertFalse(dominio.isActivo());
        assertEquals(2L, dominio.getAreaId());
        assertEquals("Té", entity.getNombre());
        assertTrue(entity.isActivo());
        assertSame(area, entity.getArea());
        assertNull(entity.getFechaEliminacion());
        assertEquals("x", entity.getDescripcion());
    }

    @Test
    void categoriaAdapter() {
        CategoriaJpaRepository jpa = mock(CategoriaJpaRepository.class);
        AreaJpaRepository areas = mock(AreaJpaRepository.class);
        CategoriaPersistenceAdapter adapter = new CategoriaPersistenceAdapter(jpa, areas, em);
        when(jpa.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(jpa.buscarNegocioId(3L)).thenReturn(Optional.of(1L));
        when(jpa.buscarActivaPorId(3L, 2L, 1L)).thenReturn(Optional.of(categoria));
        when(jpa.buscarActivaPorId(9L, 2L, 1L)).thenReturn(Optional.empty());
        when(jpa.listarActivasPorArea(2L, 1L)).thenReturn(List.of(categoria));
        when(jpa.listarActivasPorNegocio(1L)).thenReturn(List.of(categoria));

        assertEquals(Optional.of(1L), adapter.buscarNegocioId(3L));
        assertEquals("Nueva", adapter.guardar(new Categoria(null, 2L, "Nueva", null), 1L).getNombre());
        assertEquals("Editada", adapter.guardar(new Categoria(3L, 2L, "Editada", null), 1L).getNombre());
        assertThrows(CatalogoNoEncontradoException.class,
            () -> adapter.guardar(new Categoria(9L, 2L, "X", null), 1L));
        assertTrue(adapter.buscarPorId(3L, 2L, 1L).isPresent());
        assertEquals(1, adapter.listarPorArea(2L, 1L).size());
        assertEquals(1, adapter.listarPorNegocio(1L).size());
    }

    @Test
    void categoriaBuscarNegocioParaActualizarEncadenaLosPasos() {
        CategoriaJpaRepository jpa = mock(CategoriaJpaRepository.class);
        AreaJpaRepository areas = mock(AreaJpaRepository.class);
        CategoriaPersistenceAdapter adapter = new CategoriaPersistenceAdapter(jpa, areas, em);
        when(jpa.buscarAreaId(3L)).thenReturn(Optional.of(2L));
        when(areas.bloquearActivaPorId(2L)).thenReturn(Optional.of(area));
        when(jpa.buscarNegocioId(3L)).thenReturn(Optional.of(1L));
        when(jpa.buscarAreaId(4L)).thenReturn(Optional.empty());
        when(jpa.buscarAreaId(5L)).thenReturn(Optional.of(8L));
        when(areas.bloquearActivaPorId(8L)).thenReturn(Optional.empty());

        assertEquals(Optional.of(1L), adapter.buscarNegocioIdParaActualizar(3L));
        assertTrue(adapter.buscarNegocioIdParaActualizar(4L).isEmpty());
        assertTrue(adapter.buscarNegocioIdParaActualizar(5L).isEmpty());
    }

    // ---------- Productos ----------

    @Test
    void productoMapperYEntidad() {
        Producto dominio = ProductoMapper.toDomain(productoEntity(5L, false));
        ProductoEntity entity = ProductoMapper.toEntity(new Producto(null, 3L, "Plato", BigDecimal.ONE,
            "https://res.cloudinary.com/demo/b.jpg", "p2", true), categoria);

        assertFalse(dominio.isActivo());
        assertEquals("pid", dominio.getImagenPublicId());
        assertEquals("Plato", entity.getNombre());
        assertEquals("p2", entity.getImagenPublicId());
        assertTrue(entity.isActivo());
        assertSame(categoria, entity.getCategoria());
        assertEquals(BigDecimal.ZERO, new ProductoEntity().getPrecio());
        assertTrue(new ProductoEntity().isActivo());
    }

    @Test
    void productoAdapterGuardaYConsulta() {
        ProductoJpaRepository jpa = mock(ProductoJpaRepository.class);
        ProductoPersistenceAdapter adapter = new ProductoPersistenceAdapter(jpa, em);
        ProductoEntity existente = productoEntity(5L, true);
        when(jpa.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(jpa.findById(5L)).thenReturn(Optional.of(existente));
        when(jpa.findById(9L)).thenReturn(Optional.empty());
        when(jpa.buscarActivoDelUsuario(5L, 7L)).thenReturn(Optional.of(existente));
        when(jpa.buscarActivoConClasificacion(5L, 7L)).thenReturn(Optional.of(existente));
        when(jpa.buscarActivoConClasificacionEnNegocio(5L, 1L)).thenReturn(Optional.of(existente));
        when(jpa.listarActivosPorNegocio(1L)).thenReturn(List.of(existente));
        when(jpa.existsByImagenPublicIdAndActivoTrueAndIdNot("pid", 5L)).thenReturn(true);
        when(jpa.existsByCategoria_Id(3L)).thenReturn(true);
        when(jpa.existsByCategoria_Area_Id(2L)).thenReturn(true);

        assertEquals("Nuevo", adapter.guardar(new Producto(null, 3L, "Nuevo", BigDecimal.ONE, true)).getNombre());
        assertEquals("Editado", adapter.guardar(new Producto(5L, 3L, "Editado", BigDecimal.ONE, true)).getNombre());
        assertThrows(ProductoNoEncontradoException.class,
            () -> adapter.guardar(new Producto(9L, 3L, "X", BigDecimal.ONE, true)));
        assertTrue(adapter.buscarPorId(5L, 7L).isPresent());
        ClasificacionProducto clasificacion = adapter.buscarConClasificacion(5L, 7L).orElseThrow().clasificacion();
        assertEquals("Cocina", clasificacion.areaNombre());
        assertEquals("Café", clasificacion.categoriaNombre());
        assertTrue(adapter.buscarConClasificacionEnNegocio(5L, 1L).isPresent());
        assertEquals(1, adapter.listarPorNegocio(1L).size());
        assertTrue(adapter.imagenUsadaPorOtroProducto("pid", 5L));
        assertTrue(adapter.existenPorCategoria(3L));
        assertTrue(adapter.existenPorArea(2L));
        verify(jpa, never()).findById(null);
    }

    @Test
    void productoAdapterBuscaClasificacionYPagina() {
        ProductoJpaRepository jpa = mock(ProductoJpaRepository.class);
        ProductoPersistenceAdapter adapter = new ProductoPersistenceAdapter(jpa, em);
        when(jpa.clasificacionDeCategoria(3L)).thenReturn(List.<Object[]>of(new Object[]{3L, "Café", 2L, "Cocina"}));
        when(jpa.clasificacionDeCategoria(4L)).thenReturn(List.of());
        Page<ProductoEntity> pagina = new PageImpl<>(List.of(productoEntity(5L, true)));
        when(jpa.buscarActivosPorNegocio(anyLong(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(pagina);

        ClasificacionProducto clasificacion = adapter.buscarClasificacion(3L).orElseThrow();
        assertEquals(2L, clasificacion.areaId());
        assertEquals(3L, clasificacion.categoriaId());
        assertTrue(adapter.buscarClasificacion(4L).isEmpty());

        Pagina<Producto> sinTexto = adapter.listarPagina(1L, FiltroProductos.SIN_FILTROS, 0, 20);
        assertEquals(1, sinTexto.contenido().size());
        assertEquals(1, sinTexto.totalElementos());
        adapter.listarPagina(1L, new FiltroProductos(null, null, "50%_a\\B", null), 1, 10);

        ArgumentCaptor<String> patron = ArgumentCaptor.forClass(String.class);
        verify(jpa, org.mockito.Mockito.times(2)).buscarActivosPorNegocio(anyLong(), any(), any(), patron.capture(),
            any(), any(), any(), any(Pageable.class));
        assertEquals("%", patron.getAllValues().get(0));
        assertEquals("%50\\%\\_a\\\\b%", patron.getAllValues().get(1));
    }

    // ---------- Descuentos ----------

    @Test
    void descuentoMapperYEntidad() {
        DescuentoEntity entity = descuentoEntity(8L, 2, true);
        entity.setActivo(null);
        assertFalse(DescuentoMapper.toDomain(entity).isActivo());
        entity.setActivo(true);
        assertTrue(DescuentoMapper.toDomain(entity).isActivo());

        DescuentoEntity nueva = DescuentoMapper.toEntity(
            new Descuento(null, 1L, "Nuevo", 1, 4, new BigDecimal("5.50"), false), negocio);
        assertEquals(4, nueva.getCantidadMaxima());
        assertEquals(new BigDecimal("5.50"), nueva.getDescuento());
        assertFalse(nueva.getActivo());
        assertSame(negocio, nueva.getNegocio());
        assertTrue(nueva.getProductos().isEmpty() && nueva.getCategorias().isEmpty() && nueva.getAreas().isEmpty());
        assertTrue(new DescuentoEntity().getActivo());
    }

    @Test
    void descuentoAdapterGuardaYAsigna() {
        DescuentoJpaRepository jpa = mock(DescuentoJpaRepository.class);
        DescuentoPersistenceAdapter adapter = new DescuentoPersistenceAdapter(jpa, em);
        DescuentoEntity existente = descuentoEntity(8L, 2, true);
        when(jpa.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(jpa.findById(8L)).thenReturn(Optional.of(existente));
        when(jpa.findById(9L)).thenReturn(Optional.empty());

        assertEquals("Nuevo", adapter.guardar(new Descuento(null, 1L, "Nuevo", 1, null, BigDecimal.ONE, true))
            .getNombre());
        assertEquals("Editado", adapter.guardar(new Descuento(8L, 1L, "Editado", 1, null, BigDecimal.ONE, true))
            .getNombre());
        assertThrows(IllegalStateException.class,
            () -> adapter.guardar(new Descuento(9L, 1L, "X", 1, null, BigDecimal.ONE, true)));

        adapter.vincularAProducto(8L, 5L);
        adapter.asignar(8L, DestinoDescuento.CATEGORIA, 3L);
        adapter.asignar(8L, DestinoDescuento.AREA, 2L);
        assertEquals(1, existente.getProductos().size());
        assertEquals(1, existente.getCategorias().size());
        assertEquals(1, existente.getAreas().size());
        assertThrows(IllegalStateException.class, () -> adapter.asignar(9L, DestinoDescuento.AREA, 2L));

        adapter.quitar(8L, DestinoDescuento.PRODUCTO, 5L);
        adapter.quitar(8L, DestinoDescuento.CATEGORIA, 3L);
        adapter.quitar(8L, DestinoDescuento.AREA, 2L);
        assertTrue(existente.getProductos().isEmpty());
        assertTrue(existente.getCategorias().isEmpty());
        assertTrue(existente.getAreas().isEmpty());
    }

    @Test
    void descuentoAdapterConsultaPorProductoYEscalones() {
        DescuentoJpaRepository jpa = mock(DescuentoJpaRepository.class);
        DescuentoPersistenceAdapter adapter = new DescuentoPersistenceAdapter(jpa, em);
        DescuentoEntity d1 = descuentoEntity(1L, 5, true);
        DescuentoEntity d2 = descuentoEntity(2L, 1, true);
        DescuentoEntity d3 = descuentoEntity(3L, 9, true);
        when(jpa.activosDirectosPorProductos(anyCollection())).thenReturn(List.of());
        when(jpa.activosPorCategoriaDeProductos(anyCollection())).thenReturn(List.of());
        when(jpa.activosPorAreaDeProductos(anyCollection())).thenReturn(List.of());

        // Sin descuentos en ningún nivel.
        assertTrue(adapter.buscarEscalonesActivosPorProducto(5L).isEmpty());
        assertTrue(adapter.buscarActivoPorProducto(5L).isEmpty());
        assertTrue(adapter.buscarActivosPorProductos(List.of()).isEmpty());

        // Nivel de área.
        when(jpa.activosPorAreaDeProductos(anyCollection())).thenReturn(List.<Object[]>of(new Object[]{5L, d3}));
        assertEquals(3L, adapter.buscarEscalonesActivosPorProducto(5L).get(0).getId());

        // Nivel de categoría.
        when(jpa.activosPorCategoriaDeProductos(anyCollection())).thenReturn(List.<Object[]>of(new Object[]{5L, d1}));
        assertEquals(1L, adapter.buscarEscalonesActivosPorProducto(5L).get(0).getId());

        // Nivel directo: tiene prioridad y se ordena por cantidad mínima.
        when(jpa.activosDirectosPorProductos(anyCollection())).thenReturn(
            List.<Object[]>of(new Object[]{5L, d1}, new Object[]{5L, d2}));
        List<Descuento> escalones = adapter.buscarEscalonesActivosPorProducto(5L);
        assertEquals(List.of(2L, 1L), escalones.stream().map(Descuento::getId).toList());

        // El directo gana sobre categoría y área al buscar el descuento activo del producto.
        assertEquals(1L, adapter.buscarActivoPorProducto(5L).orElseThrow().getId());
        Map<Long, Descuento> activos = adapter.buscarActivosPorProductos(List.of(5L));
        assertEquals(1, activos.size());
    }

    @Test
    void descuentoAdapterPropioListadoYAsignaciones() {
        DescuentoJpaRepository jpa = mock(DescuentoJpaRepository.class);
        DescuentoPersistenceAdapter adapter = new DescuentoPersistenceAdapter(jpa, em);
        DescuentoEntity activo = descuentoEntity(1L, 1, true);
        DescuentoEntity inactivo = descuentoEntity(2L, 1, false);
        when(jpa.propiosDeProducto(5L)).thenReturn(List.of(inactivo, activo));
        when(jpa.propiosDeProducto(6L)).thenReturn(List.of(inactivo));
        when(jpa.findByIdAndNegocioId(1L, 1L)).thenReturn(Optional.of(activo));
        when(jpa.findByNegocioIdOrderById(1L)).thenReturn(List.of(activo, inactivo));
        when(jpa.asignadosAProducto(1L, 5L)).thenReturn(List.of(activo));
        when(jpa.productosAsignados(anyCollection())).thenReturn(
            List.<Object[]>of(new Object[]{1L, 5L}, new Object[]{1L, 6L}));
        when(jpa.categoriasAsignadas(anyCollection())).thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));
        when(jpa.areasAsignadas(anyCollection())).thenReturn(List.of());

        assertEquals(1L, adapter.buscarPropioActivoDeProducto(5L).orElseThrow().getId());
        assertTrue(adapter.buscarPropioActivoDeProducto(6L).isEmpty());
        assertTrue(adapter.buscarPorId(1L, 1L).isPresent());
        assertEquals(2, adapter.listarPorNegocio(1L).size());
        assertEquals(1, adapter.listarPorProducto(1L, 5L).size());

        adapter.eliminar(1L);
        verify(jpa).deleteById(1L);
        verify(jpa).flush();

        AsignacionesDescuento asignaciones = adapter.asignaciones(1L);
        assertEquals(Set.of(5L, 6L), asignaciones.productoIds());
        assertEquals(Set.of(3L), asignaciones.categoriaIds());
        assertTrue(asignaciones.areaIds().isEmpty());
        assertTrue(adapter.asignacionesDe(List.of()).isEmpty());
        assertTrue(adapter.asignacionesDe(List.of(1L, 7L)).get(7L).productoIds().isEmpty());
    }

    @Test
    void descuentoAdapterValidaDestinosYAsignadosPorDestino() {
        DescuentoJpaRepository jpa = mock(DescuentoJpaRepository.class);
        DescuentoPersistenceAdapter adapter = new DescuentoPersistenceAdapter(jpa, em);
        DescuentoEntity activo = descuentoEntity(1L, 1, true);
        when(jpa.productoActivoDelNegocio(5L, 1L)).thenReturn(true);
        when(jpa.categoriaActivaDelNegocio(3L, 1L)).thenReturn(true);
        when(jpa.areaActivaDelNegocio(2L, 1L)).thenReturn(false);
        when(jpa.activosAsignadosAProductos(anyCollection())).thenReturn(
            List.<Object[]>of(new Object[]{5L, activo}, new Object[]{5L, descuentoEntity(2L, 3, true)}));
        when(jpa.activosAsignadosACategorias(anyCollection())).thenReturn(List.<Object[]>of(new Object[]{3L, activo}));
        when(jpa.activosAsignadosAAreas(anyCollection())).thenReturn(List.of());

        assertTrue(adapter.destinoPerteneceAlNegocio(DestinoDescuento.PRODUCTO, 5L, 1L));
        assertTrue(adapter.destinoPerteneceAlNegocio(DestinoDescuento.CATEGORIA, 3L, 1L));
        assertFalse(adapter.destinoPerteneceAlNegocio(DestinoDescuento.AREA, 2L, 1L));

        assertTrue(adapter.activosAsignadosA(DestinoDescuento.PRODUCTO, List.of()).isEmpty());
        assertEquals(2, adapter.activosAsignadosA(DestinoDescuento.PRODUCTO, List.of(5L)).get(5L).size());
        assertEquals(1, adapter.activosAsignadosA(DestinoDescuento.CATEGORIA, List.of(3L)).get(3L).size());
        assertTrue(adapter.activosAsignadosA(DestinoDescuento.AREA, List.of(2L)).isEmpty());
    }
}
