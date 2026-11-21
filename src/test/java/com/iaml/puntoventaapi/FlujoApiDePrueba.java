package com.iaml.puntoventaapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Flujos completos por HTTP contra el PostgreSQL de prueba: controlador, servicio, adaptadores y SQL reales.
 * Siembra un negocio con propietario, empleado y una sucursal con dos productos en existencia, más otro
 * negocio ajeno para probar el acceso. La sesión se simula con un JWT en el contexto de seguridad.
 */
@AutoConfigureMockMvc(addFilters = false)
public abstract class FlujoApiDePrueba extends PostgresDePrueba {

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;

    protected long negocio;
    protected long sucursal;
    protected long propietario;
    protected long empleado;
    protected long ajeno;
    protected long sucursalAjena;
    /** Jarrón: $100.00, 10 en existencia, descuento 10 % de 2 a 4 piezas y 15 % desde 5. */
    protected long jarron;
    /** Maceta: $50.00, 5 en existencia, sin descuento. */
    protected long maceta;

    @BeforeEach
    void sembrar() {
        long rolPropietario = rol("PROPIETARIO");
        long rolEmpleado = rol("EMPLEADO");
        negocio = id("insert into barrocontrol.negocio (nombre, eslogan) values ('Cerámica', 'Hecho a mano') returning id");
        sucursal = id("insert into barrocontrol.sucursal (id_negocio, nombre) values (?, 'Centro') returning id", negocio);
        propietario = usuario(rolPropietario, negocio, "dueno");
        jdbc.update("insert into barrocontrol.negocio_usuario (id_negocio, id_usuario) values (?, ?)", negocio, propietario);
        empleado = usuario(rolEmpleado, negocio, "empleado");
        jdbc.update("insert into barrocontrol.empleado_sucursal (id_usuario, id_sucursal) values (?, ?)", empleado, sucursal);

        long otroNegocio = id("insert into barrocontrol.negocio (nombre) values ('Otro') returning id");
        sucursalAjena = id("insert into barrocontrol.sucursal (id_negocio, nombre) values (?, 'Lejos') returning id", otroNegocio);
        ajeno = usuario(rolPropietario, otroNegocio, "ajeno");
        jdbc.update("insert into barrocontrol.negocio_usuario (id_negocio, id_usuario) values (?, ?)", otroNegocio, ajeno);

        long area = id("insert into barrocontrol.area (id_negocio, nombre) values (?, 'Decoración') returning id", negocio);
        long categoria = id("insert into barrocontrol.categoria (id_area, nombre) values (?, 'Barro') returning id", area);
        jdbc.update("insert into barrocontrol.sucursal_area (id_sucursal, id_area) values (?, ?)", sucursal, area);
        jarron = producto(categoria, "Jarrón", "100.00", 10);
        maceta = producto(categoria, "Maceta", "50.00", 5);
        descuento(jarron, 2, 4, "10.00");
        descuento(jarron, 5, null, "15.00");
    }

    @AfterEach
    void cerrarSesion() {
        SecurityContextHolder.clearContext();
    }

    /** Sesión del usuario con su rol, como la deja el filtro JWT. */
    protected void como(long usuarioId, String rol) {
        var jwt = Jwt.withTokenValue("prueba").header("alg", "none").subject(String.valueOf(usuarioId)).build();
        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    protected void comoPropietario() { como(propietario, "PROPIETARIO"); }

    protected void comoEmpleado() { como(empleado, "EMPLEADO"); }

    protected long abrirCorte(long sucursalId, long usuarioId) {
        Long caja = jdbc.query("select id from barrocontrol.caja where id_sucursal = ?",
            rs -> rs.next() ? rs.getLong(1) : null, sucursalId);
        if (caja == null) {
            caja = id("insert into barrocontrol.caja (id_sucursal) values (?) returning id", sucursalId);
        }
        return id("insert into barrocontrol.corte_caja (id_caja, id_usuario, monto_inicial) values (?, ?, 100) returning id",
            caja, usuarioId);
    }

    protected ResultActions getJson(String url, Object... variables) throws Exception {
        return mvc.perform(get(url, variables));
    }

    protected ResultActions postJson(String url, String json, Object... variables) throws Exception {
        return mvc.perform(conJson(post(url, variables), json));
    }

    protected ResultActions putJson(String url, String json, Object... variables) throws Exception {
        return mvc.perform(conJson(put(url, variables), json));
    }

    protected ResultActions patchJson(String url, String json, Object... variables) throws Exception {
        return mvc.perform(conJson(patch(url, variables), json));
    }

    protected static long leerId(ResultActions resultado, String ruta) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), ruta)).longValue();
    }

    private static MockHttpServletRequestBuilder conJson(MockHttpServletRequestBuilder peticion, String json) {
        return peticion.contentType("application/json").content(json);
    }

    /** La aplicación crea los roles al arrancar; si no existen, se crean aquí. */
    private long rol(String nombre) {
        return id("""
            insert into barrocontrol.rol (nombre) values (?)
            on conflict on constraint uq_rol_nombre do update set nombre = excluded.nombre returning id
            """, nombre);
    }

    private long usuario(long rol, long negocioId, String alias) {
        return id("""
            insert into barrocontrol.usuario (id_rol, id_negocio, nombre, correo, password_hash)
            values (?, ?, ?, ?, 'hash-de-prueba') returning id
            """, rol, negocioId, alias, alias + "@prueba.test");
    }

    private long producto(long categoria, String nombre, String precio, int existencia) {
        long id = id("""
            insert into barrocontrol.producto (id_categoria, nombre, precio, imagen_url)
            values (?, ?, cast(? as numeric), 'https://res.cloudinary.com/prueba/image/upload/p.png') returning id
            """, categoria, nombre, precio);
        jdbc.update("insert into barrocontrol.inventario (id_sucursal, id_producto, existencia) values (?, ?, ?)",
            sucursal, id, existencia);
        return id;
    }

    private void descuento(long productoId, int minimo, Integer maximo, String porcentaje) {
        long id = id("""
            insert into barrocontrol.descuento (id_negocio, nombre, cantidad_minima, cantidad_maxima, descuento)
            values (?, 'Mayoreo', ?, ?, cast(? as numeric)) returning id
            """, negocio, minimo, maximo, porcentaje);
        jdbc.update("insert into barrocontrol.descuento_producto (id_descuento, id_producto) values (?, ?)", id, productoId);
    }

    protected long id(String sql, Object... parametros) {
        return jdbc.queryForObject(sql, Long.class, parametros);
    }
}
