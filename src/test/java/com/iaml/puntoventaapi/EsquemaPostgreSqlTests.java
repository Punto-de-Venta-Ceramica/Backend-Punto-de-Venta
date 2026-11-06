package com.iaml.puntoventaapi;

import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.PermisoEntity;
import com.iaml.puntoventaapi.autenticacion.infrastructure.persistence.RolEntity;
import com.iaml.puntoventaapi.caja.domain.Caja;
import com.iaml.puntoventaapi.caja.domain.CorteCaja;
import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.AreaEntity;
import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.CategoriaEntity;
import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.DescuentoEntity;
import com.iaml.puntoventaapi.catalogo.infrastructure.persistence.ProductoEntity;
import com.iaml.puntoventaapi.clientes.domain.Cliente;
import com.iaml.puntoventaapi.negocios.infrastructure.persistence.NegocioEntity;
import com.iaml.puntoventaapi.pedidos.infrastructure.persistence.DetallePedido;
import com.iaml.puntoventaapi.pedidos.infrastructure.persistence.PagoPedido;
import com.iaml.puntoventaapi.pedidos.infrastructure.persistence.Pedido;
import com.iaml.puntoventaapi.sucursal.infrastructure.persistence.SucursalEntity;
import com.iaml.puntoventaapi.usuarios.infrastructure.persistence.UsuarioEntity;
import com.iaml.puntoventaapi.ventas.infrastructure.persistence.DetalleVenta;
import com.iaml.puntoventaapi.ventas.infrastructure.persistence.DetalleVentaId;
import com.iaml.puntoventaapi.ventas.infrastructure.persistence.Venta;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Run against a dedicated, empty PostgreSQL database, never the business database.
 * Without TEST_DB_URL these integration tests are explicitly skipped.
 */
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@SpringBootTest(properties = {
    "spring.config.import=",
    "jwt.secreto=clave-de-pruebas-locales-de-al-menos-32-bytes",
    "spring.datasource.url=${TEST_DB_URL}",
    "spring.datasource.username=${TEST_DB_USERNAME}",
    "spring.datasource.password=${TEST_DB_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=update"
})
@Transactional
class EsquemaPostgreSqlTests {

    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    private UsuarioEntity usuario;
    private NegocioEntity negocio;
    private SucursalEntity sucursal;
    private ProductoEntity producto;
    private Caja caja;
    private CorteCaja corte;

    @BeforeEach
    void crearCatalogos() {
        RolEntity rol = new RolEntity();
        rol.setNombre("Administrador");
        em.persist(rol);
        usuario = new UsuarioEntity();
        usuario.setRol(rol);
        usuario.setNombre("Prueba");
        usuario.setCorreo("prueba@example.test");
        usuario.setPasswordHash("hash-exclusivo-de-pruebas");
        em.persist(usuario);
        negocio = new NegocioEntity();
        negocio.setNombre("Cerámica");
        em.persist(negocio);
        sucursal = new SucursalEntity();
        sucursal.setNegocio(negocio);
        sucursal.setNombre("Centro");
        em.persist(sucursal);
        AreaEntity area = new AreaEntity();
        area.setNegocio(negocio);
        area.setNombre("Decoración");
        em.persist(area);
        CategoriaEntity categoria = new CategoriaEntity();
        categoria.setArea(area);
        categoria.setNombre("Jarrones");
        em.persist(categoria);
        producto = new ProductoEntity();
        producto.setCategoria(categoria);
        producto.setNombre("Jarrón");
        em.persist(producto);
        caja = new Caja();
        caja.setSucursal(sucursal);
        em.persist(caja);
        corte = new CorteCaja();
        corte.setCaja(caja);
        corte.setUsuario(usuario);
        em.persist(corte);
        em.flush();
    }

    @Test
    void creaLas23TablasYLasTresVistasDelScript() {
        assertThat(jdbc.queryForObject("""
            select count(*) from information_schema.tables
            where table_schema = 'barrocontrol' and table_type = 'BASE TABLE'
            """, Integer.class)).isEqualTo(23);
        assertThat(jdbc.queryForList("""
            select table_name from information_schema.views
            where table_schema = 'barrocontrol'
            """, String.class)).containsExactlyInAnyOrder("vw_venta", "vw_pedido", "vw_corte_caja");
        assertThat(corte.getFechaHoraApertura()).isNotNull();
        assertThat(jdbc.queryForObject("""
            select count(*) from pg_attribute a
            join pg_class t on t.oid = a.attrelid
            join pg_namespace n on n.oid = t.relnamespace
            where n.nspname = 'barrocontrol' and t.relkind = 'r' and a.attgenerated = 's'
            """, Integer.class)).isEqualTo(3);
    }

    @Test
    void persisteLasCuatroRelacionesMuchosAMuchos() {
        PermisoEntity permiso = new PermisoEntity();
        permiso.setNombre("VENDER");
        em.persist(permiso);
        usuario.getRol().getPermisos().add(permiso);
        negocio.getUsuarios().add(usuario);
        sucursal.getAreas().add(producto.getCategoria().getArea());
        DescuentoEntity descuento = new DescuentoEntity();
        descuento.setNegocio(negocio);
        descuento.setNombre("Mayoreo");
        descuento.setCantidadMinima(10);
        descuento.setDescuento(new BigDecimal("5.00"));
        descuento.getProductos().add(producto);
        em.persist(descuento);
        em.flush();
        for (String tabla : new String[]{"rol_permiso", "negocio_usuario", "sucursal_area", "descuento_producto"}) {
            assertThat(jdbc.queryForObject("select count(*) from barrocontrol." + tabla, Integer.class))
                .isEqualTo(1);
        }
    }

    @Test
    void calculaSubtotalesEnPostgresYActualizaLasVistasDeVentaYCorte() {
        Venta venta = crearVenta();
        DetalleVenta detalle = new DetalleVenta();
        detalle.setVenta(venta);
        detalle.setProducto(producto);
        detalle.setCantidad(3);
        detalle.setPrecioUnitario(new BigDecimal("19.99"));
        detalle.setDescAplicado(new BigDecimal("12.50"));
        em.persist(detalle);
        em.flush();
        assertThat(detalle.getId()).isEqualTo(new DetalleVentaId(venta.getId(), producto.getId()));
        assertThat(detalle.getSubtotal()).isEqualByComparingTo("52.47");
        detalle.setCantidad(4);
        em.flush();
        assertThat(detalle.getSubtotal()).isEqualByComparingTo("69.97");
        assertThat(jdbc.queryForObject("select total from barrocontrol.vw_venta where id = ?",
            BigDecimal.class, venta.getId())).isEqualByComparingTo("69.97");
        assertThat(jdbc.queryForObject("select total_ventas from barrocontrol.vw_corte_caja where id = ?",
            BigDecimal.class, corte.getId())).isEqualByComparingTo("69.97");
    }

    @Test
    void calculaSaldoDelPedidoSinSumarSusPagosALasVentas() {
        Pedido pedido = crearPedido();
        DetallePedido detalle = new DetallePedido();
        detalle.setPedido(pedido);
        detalle.setProducto(producto);
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(new BigDecimal("100.00"));
        detalle.setDescAplicado(new BigDecimal("10.00"));
        em.persist(detalle);
        for (String importe : new String[]{"50.00", "30.00"}) {
            PagoPedido pago = new PagoPedido();
            pago.setPedido(pedido);
            pago.setCorteCaja(corte);
            pago.setMonto(new BigDecimal(importe));
            pago.setMetodoPago("EFECTIVO");
            pago.setTipoPago("ANTICIPO");
            em.persist(pago);
        }
        em.flush();
        assertThat(detalle.getSubtotal()).isEqualByComparingTo("180.00");
        assertThat(jdbc.queryForObject("select saldo from barrocontrol.vw_pedido where id = ?",
            BigDecimal.class, pedido.getId())).isEqualByComparingTo("100.00");
        assertThat(jdbc.queryForObject("select total_ventas from barrocontrol.vw_corte_caja where id = ?",
            BigDecimal.class, corte.getId())).isEqualByComparingTo("0.00");
    }

    @Test
    void calculaLaDiferenciaDelCorteEnLaBaseDeDatos() {
        corte.setTotalEstimado(new BigDecimal("500.00"));
        corte.setTotalReal(new BigDecimal("480.00"));
        em.flush();
        assertThat(corte.getDiferencia()).isEqualByComparingTo("-20.00");
    }

    @Test
    void rechazaCorreosDuplicadosAunqueCambienMayusculasYEspacios() {
        assertThatThrownBy(() -> jdbc.update("""
            insert into barrocontrol.usuario (id_rol, nombre, correo, password_hash)
            values (?, 'Otro', ' PRUEBA@EXAMPLE.TEST ', 'hash-prueba')
            """, usuario.getRol().getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaVentasConUnaCajaDistintaALaDelCorte() {
        SucursalEntity otraSucursal = new SucursalEntity();
        otraSucursal.setNegocio(negocio);
        otraSucursal.setNombre("Otra");
        em.persist(otraSucursal);
        Caja otraCaja = new Caja();
        otraCaja.setSucursal(otraSucursal);
        em.persist(otraCaja);
        em.flush();
        assertThatThrownBy(() -> jdbc.update("""
            insert into barrocontrol.venta (id_usuario, id_caja, id_corte_caja, metodo_pago)
            values (?, ?, ?, 'EFECTIVO')
            """, usuario.getId(), otraCaja.getId(), corte.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaInventarioNegativo() {
        assertThatThrownBy(() -> jdbc.update("""
            insert into barrocontrol.inventario (id_sucursal, id_producto, existencia)
            values (?, ?, -1)
            """, sucursal.getId(), producto.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaNotificacionesConDosOrigenes() {
        Venta venta = crearVenta();
        Pedido pedido = crearPedido();
        em.flush();
        assertThatThrownBy(() -> jdbc.update("""
            insert into barrocontrol.notificacion (id_usuario, id_venta, id_pedido, titulo, mensaje)
            values (?, ?, ?, 'Aviso', 'Mensaje')
            """, usuario.getId(), venta.getId(), pedido.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaNotificacionesSinOrigen() {
        assertThatThrownBy(() -> jdbc.update("""
            insert into barrocontrol.notificacion (id_usuario, titulo, mensaje)
            values (?, 'Aviso', 'Mensaje')
            """, usuario.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void impideCerrarElCorteSinArqueo() {
        assertThatThrownBy(() -> jdbc.update("""
            update barrocontrol.corte_caja set fecha_hora_cierre = current_timestamp where id = ?
            """, corte.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Venta crearVenta() {
        Venta venta = new Venta();
        venta.setUsuario(usuario);
        venta.setCaja(caja);
        venta.setCorteCaja(corte);
        venta.setMetodoPago("EFECTIVO");
        em.persist(venta);
        return venta;
    }

    private Pedido crearPedido() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente");
        em.persist(cliente);
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setSucursal(sucursal);
        pedido.setUsuario(usuario);
        pedido.setFolio("P-001");
        pedido.setTipoEntrega("RECOGER");
        pedido.setEstado("PENDIENTE");
        em.persist(pedido);
        return pedido;
    }
}
