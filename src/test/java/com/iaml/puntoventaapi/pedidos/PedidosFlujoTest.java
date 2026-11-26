package com.iaml.puntoventaapi.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iaml.puntoventaapi.FlujoApiDePrueba;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Pedidos de punta a punta en PostgreSQL: registro, listado, pagos, estados, cancelación y edición. */
class PedidosFlujoTest extends FlujoApiDePrueba {

    private String pedido(long sucursalId, String monto, String productos) {
        return """
            {"sucursalId":%d,"descripcion":"  Para   regalo ","cliente":{"nombre":"Ana López","telefono":"5512345678",
             "ciudad":"Puebla","colonia":"Centro","calle":"5 de Mayo 10"},
             "productos":[%s],"tipoEntrega":"DOMICILIO","descripcionEnvio":"Tocar el timbre",
             "fechaEntrega":"2099-12-31T23:59:59Z","pagoInicial":{"monto":%s,"metodoPago":"EFECTIVO"}}
            """.formatted(sucursalId, productos, monto);
    }

    /** Jarrón x3 con 10 % y maceta x1: total $320.00. */
    private String carrito() {
        return "{\"productoId\":%d,\"cantidad\":3},{\"productoId\":%d,\"cantidad\":1}".formatted(jarron, maceta);
    }

    private ResultActions registrar(String json) throws Exception {
        return postJson("/api/pedidos", json);
    }

    private long registrarConAnticipo() throws Exception {
        return leerId(registrar(pedido(sucursal, "100", carrito())).andExpect(status().isCreated()), "$.id");
    }

    private ResultActions estado(long pedidoId, String estado) throws Exception {
        return patchJson("/api/pedidos/{id}/estado", "{\"estado\":\"" + estado + "\"}", pedidoId);
    }

    private ResultActions pagar(long pedidoId, String monto) throws Exception {
        return postJson("/api/pedidos/{id}/pagos", "{\"monto\":" + monto + ",\"metodoPago\":\"TARJETA\"}", pedidoId);
    }

    private ResultActions cancelar(long pedidoId) throws Exception {
        return mvc.perform(post("/api/pedidos/{id}/cancelar", pedidoId));
    }

    private int notificaciones(long usuarioId) {
        return jdbc.queryForObject("select count(*) from barrocontrol.notificacion where id_usuario = ?",
            Integer.class, usuarioId);
    }

    @Test
    void registraConAnticipoYMuestraElDetalleCompleto() throws Exception {
        abrirCorte(sucursal, empleado);
        comoEmpleado();
        long id = leerId(registrar(pedido(sucursal, "100", carrito()))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", startsWith("/api/pedidos/")))
            .andExpect(jsonPath("$.total").value(320.00))
            .andExpect(jsonPath("$.saldoPendiente").value(220.00))
            .andExpect(jsonPath("$.pagado").value(false)), "$.id");
        assertThat(notificaciones(propietario)).isEqualTo(1);

        comoPropietario();
        getJson("/api/pedidos/{id}", id)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.folio").value(startsWith("PED-")))
            .andExpect(jsonPath("$.descripcion").value("Para regalo"))
            .andExpect(jsonPath("$.descripcionEnvio").value("Tocar el timbre"))
            .andExpect(jsonPath("$.cliente.calle").value("5 de Mayo 10"))
            .andExpect(jsonPath("$.productos.length()").value(2))
            .andExpect(jsonPath("$.productos[0].imagenUrl").value(startsWith("https://res.cloudinary.com/")))
            .andExpect(jsonPath("$.pagos[0].tipoPago").value("ANTICIPO"))
            .andExpect(jsonPath("$.estado").value("ENCARGADO"))
            .andExpect(jsonPath("$.sucursal").value("Centro"))
            .andExpect(jsonPath("$.negocioId").value(negocio))
            .andExpect(jsonPath("$.negocio").value("Cerámica"))
            .andExpect(jsonPath("$.fechaEntrega").value(startsWith("2099-12-31")));
        // Pedidos registrados antes de capturar la fecha de entrega.
        jdbc.update("update barrocontrol.pedido set fecha_entrega = null where id = ?", id);
        getJson("/api/pedidos/{id}", id).andExpect(jsonPath("$.fechaEntrega").doesNotExist());
        como(ajeno, "PROPIETARIO");
        getJson("/api/pedidos/{id}", id).andExpect(status().isNotFound());
        getJson("/api/pedidos/{id}", 999_999).andExpect(status().isNotFound());
    }

    @Test
    void registraSinAnticipoNiDescuentoAunqueLaCajaEsteCerrada() throws Exception {
        comoEmpleado();
        String sinDescuento = "{\"productoId\":%d,\"cantidad\":3,\"aplicarDescuento\":false}".formatted(jarron);
        registrar(pedido(sucursal, "0", sinDescuento))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total").value(300.00))
            .andExpect(jsonPath("$.totalPagado").value(0));
    }

    @Test
    void rechazaRegistrosQueNoSePuedenCobrarOProductosAjenos() throws Exception {
        comoEmpleado();
        registrar(pedido(sucursal, "100", carrito()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("CORTE_CAJA_NO_ABIERTO"));
        registrar(pedido(sucursal, "0", "{\"productoId\":999999,\"cantidad\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.codigo").value("PRODUCTO_NO_DISPONIBLE"));
        registrar(pedido(sucursalAjena, "0", carrito())).andExpect(status().isForbidden());
    }

    @Test
    void unaSucursalSinPropietarioNoPuedeRecibirPedidos() throws Exception {
        long otroNegocio = id("insert into barrocontrol.negocio (nombre) values ('Sin dueño') returning id");
        long otraSucursal = id("insert into barrocontrol.sucursal (id_negocio, nombre) values (?, 'Sola') returning id", otroNegocio);
        jdbc.update("update barrocontrol.usuario set id_negocio = ? where id = ?", otroNegocio, empleado);
        jdbc.update("insert into barrocontrol.empleado_sucursal (id_usuario, id_sucursal) values (?, ?)", empleado, otraSucursal);
        long area = id("insert into barrocontrol.area (id_negocio, nombre) values (?, 'Área') returning id", otroNegocio);
        long categoria = id("insert into barrocontrol.categoria (id_area, nombre) values (?, 'Cat') returning id", area);
        long vaso = id("insert into barrocontrol.producto (id_categoria, nombre, precio) values (?, 'Vaso', 10) returning id", categoria);
        comoEmpleado();
        registrar(pedido(otraSucursal, "0", "{\"productoId\":%d,\"cantidad\":1}".formatted(vaso)))
            .andExpect(status().isConflict());
    }

    @Test
    void listaPorEstadoYPagina() throws Exception {
        abrirCorte(sucursal, empleado);
        comoEmpleado();
        long primero = registrarConAnticipo();
        long segundo = registrarConAnticipo();
        comoPropietario();
        estado(primero, "PRODUCCION").andExpect(status().isNoContent());
        getJson("/api/pedidos?sucursalId={s}", sucursal)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(2))
            .andExpect(jsonPath("$.ultima").value(true))
            .andExpect(jsonPath("$.contenido[0].id").value(segundo))
            .andExpect(jsonPath("$.contenido[0].cliente").value("Ana López"))
            .andExpect(jsonPath("$.contenido[0].productos[0].nombre").value("Jarrón"));
        getJson("/api/pedidos?sucursalId={s}&estado=PRODUCCION", sucursal)
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.contenido[0].id").value(primero));
        getJson("/api/pedidos?sucursalId={s}&page=1", sucursal)
            .andExpect(jsonPath("$.contenido.length()").value(0));
        getJson("/api/pedidos?sucursalId={s}&page=-1", sucursal).andExpect(status().isBadRequest());
        getJson("/api/pedidos?sucursalId={s}", sucursalAjena).andExpect(status().isForbidden());
    }

    @Test
    void registraPagosHastaLiquidar() throws Exception {
        long corte = abrirCorte(sucursal, empleado);
        comoEmpleado();
        long id = registrarConAnticipo();
        pagar(id, "300").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PAGO_SUPERA_SALDO"));
        pagar(id, "20").andExpect(status().isOk()).andExpect(jsonPath("$.saldoPendiente").value(200.00));
        pagar(id, "200").andExpect(status().isOk()).andExpect(jsonPath("$.pagado").value(true));
        getJson("/api/pedidos/{id}", id).andExpect(jsonPath("$.pagado").value(true));
        getJson("/api/pedidos?sucursalId={s}", sucursal).andExpect(jsonPath("$.contenido[0].pagado").value(true));
        assertThat(jdbc.queryForList("select tipo_pago from barrocontrol.pago_pedido where id_pedido = ? order by id",
            String.class, id)).containsExactly("ANTICIPO", "ANTICIPO", "LIQUIDACION");
        pagar(999_999, "10").andExpect(status().isNotFound());

        long otro = registrarConAnticipo();
        jdbc.update("update barrocontrol.corte_caja set total_real = 0, fecha_hora_cierre = current_timestamp where id = ?", corte);
        pagar(otro, "10").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("CORTE_CAJA_NO_ABIERTO"));
    }

    @Test
    void avanzaEstadosSegunElRolYAvisaAlEntregar() throws Exception {
        abrirCorte(sucursal, empleado);
        comoEmpleado();
        long id = registrarConAnticipo();
        estado(id, "PRODUCCION").andExpect(status().isForbidden());
        comoPropietario();
        estado(id, "ENTREGADO").andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("TRANSICION_ESTADO_INVALIDA"))
            .andExpect(jsonPath("$.estadoActual").value("ENCARGADO"));
        estado(id, "PRODUCCION").andExpect(status().isNoContent());
        estado(id, "ENTREGADO").andExpect(status().isNoContent());
        assertThat(notificaciones(empleado)).isEqualTo(1);
        // Al entregarse, la fecha acordada se reemplaza por la real y ya no cambia.
        var entregado = jdbc.queryForObject("select fecha_entrega from barrocontrol.pedido where id = ?",
            java.sql.Timestamp.class, id);
        assertThat(entregado.toInstant()).isBefore(java.time.Instant.parse("2099-01-01T00:00:00Z"));
        estado(id, "ENTREGADO").andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select fecha_entrega from barrocontrol.pedido where id = ?",
            java.sql.Timestamp.class, id)).isEqualTo(entregado);
        assertThat(notificaciones(empleado)).isEqualTo(1);
        comoEmpleado();
        estado(id, "TERMINADO").andExpect(status().isNoContent());
        estado(999_999, "TERMINADO").andExpect(status().isNotFound());

        // Pedido del propietario: al entregarlo se avisa a los empleados de la sucursal.
        comoPropietario();
        long propio = registrarConAnticipo();
        estado(propio, "PRODUCCION").andExpect(status().isNoContent());
        estado(propio, "ENTREGADO").andExpect(status().isNoContent());
        assertThat(notificaciones(empleado)).isEqualTo(2);
    }

    @Test
    void cancelarDevuelveLoPagadoEnElCorteAbierto() throws Exception {
        long corte = abrirCorte(sucursal, empleado);
        comoEmpleado();
        long conPagos = registrarConAnticipo();
        long sinPagos = leerId(registrar(pedido(sucursal, "0", carrito())), "$.id");
        long sinCaja = registrarConAnticipo();
        comoPropietario();
        cancelar(conPagos).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("""
            select count(*) from barrocontrol.pago_pedido
            where id_pedido = ? and tipo_pago = 'DEVOLUCION' and id_corte_caja = ?
            """, Integer.class, conPagos, corte)).isEqualTo(1);
        getJson("/api/pedidos/{id}", conPagos).andExpect(jsonPath("$.totalPagado").value(100.00));
        cancelar(conPagos).andExpect(status().isConflict());
        pagar(conPagos, "10").andExpect(status().isBadRequest());

        jdbc.update("update barrocontrol.corte_caja set total_real = 0, fecha_hora_cierre = current_timestamp where id = ?", corte);
        cancelar(sinPagos).andExpect(status().isNoContent());
        cancelar(sinCaja).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("CORTE_CAJA_NO_ABIERTO"));
    }

    @Test
    void elPropietarioEditaLosDatosDelPedido() throws Exception {
        comoEmpleado();
        long id = leerId(registrar(pedido(sucursal, "0", carrito())), "$.id");
        String datos = """
            {"descripcion":"Nueva","tipoEntrega":" RECOGER ","descripcionEnvio":"Puerta azul",
             "cliente":{"nombre":"Luis","telefono":"5598765432"}}
            """;
        comoPropietario();
        putJson("/api/pedidos/{id}/datos", datos, id).andExpect(status().isNoContent());
        getJson("/api/pedidos/{id}", id)
            .andExpect(jsonPath("$.cliente.nombre").value("Luis"))
            .andExpect(jsonPath("$.tipoEntrega").value("RECOGER"))
            .andExpect(jsonPath("$.descripcionEnvio").value("Puerta azul"));

        // Con permiso de propietario en el token pero sin serlo en la base: se rechaza.
        como(empleado, "PROPIETARIO");
        putJson("/api/pedidos/{id}/datos", datos, id).andExpect(status().isForbidden());

        comoPropietario();
        cancelar(id).andExpect(status().isNoContent());
        putJson("/api/pedidos/{id}/datos", datos, id).andExpect(status().isConflict());
    }

    @Test
    void ofreceProductosCortesYSucursalesParaElRegistro() throws Exception {
        long corte = abrirCorte(sucursal, empleado);
        comoEmpleado();
        getJson("/api/pedidos/opciones/productos?sucursalId={s}", sucursal)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nombre").value("Jarrón"))
            .andExpect(jsonPath("$[0].descuentos.length()").value(2))
            .andExpect(jsonPath("$[1].descuentos.length()").value(0));
        getJson("/api/pedidos/opciones/cortes?sucursalId={s}", sucursal)
            .andExpect(jsonPath("$[0].id").value(corte));
        getJson("/api/pedidos/opciones/sucursales-asignadas")
            .andExpect(jsonPath("$[0].sucursalId").value(sucursal))
            .andExpect(jsonPath("$[0].negocio").value("Cerámica"));
        getJson("/api/pedidos/opciones/cortes?sucursalId={s}", sucursalAjena).andExpect(status().isForbidden());
    }
}
