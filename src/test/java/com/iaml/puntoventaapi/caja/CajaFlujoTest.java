package com.iaml.puntoventaapi.caja;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iaml.puntoventaapi.FlujoApiDePrueba;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Corte de caja de punta a punta en PostgreSQL: lo que entra y sale por ventas y pedidos, y el arqueo. */
class CajaFlujoTest extends FlujoApiDePrueba {

    private ResultActions abrir(Long sucursalId) throws Exception {
        return postJson("/api/cortes-caja/abrir", "{\"sucursalId\":%s,\"montoInicial\":200}".formatted(sucursalId));
    }

    private ResultActions cerrar(long corteId) throws Exception {
        return postJson("/api/cortes-caja/{id}/cerrar", "{\"totalReal\":900}", corteId);
    }

    private long pedido(String anticipo) throws Exception {
        String json = """
            {"sucursalId":%d,"cliente":{"nombre":"Ana","telefono":"5512345678"},
             "productos":[{"productoId":%d,"cantidad":3},{"productoId":%d,"cantidad":1}],"tipoEntrega":"LOCAL",
             "pagoInicial":{"monto":%s,"metodoPago":"EFECTIVO"}}
            """.formatted(sucursal, jarron, maceta, anticipo);
        return leerId(postJson("/api/pedidos", json).andExpect(status().isCreated()), "$.id");
    }

    private void vender(String metodo, String monto, String productos) throws Exception {
        postJson("/api/ventas/confirmar", """
            {"sucursalId":%d,"metodoPago":"%s","montoRecibido":%s,"productos":[%s]}
            """.formatted(sucursal, metodo, monto, productos)).andExpect(status().isCreated());
    }

    @Test
    void cuadraVentasPedidosYCancelacionesDelTurno() throws Exception {
        comoPropietario();
        long corte = leerId(abrir(sucursal).andExpect(status().isCreated())
            .andExpect(jsonPath("$.cerrado").value(false))
            .andExpect(jsonPath("$.totalEstimado").value(200.00)), "$.id");
        abrir(sucursal).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CORTE_CAJA_YA_ABIERTO"));

        comoEmpleado();
        getJson("/api/cortes-caja/activo").andExpect(status().isOk()).andExpect(jsonPath("$.id").value(corte));
        vender("EFECTIVO", "500", "{\"productoId\":%d,\"cantidad\":3,\"aplicarDescuento\":true},{\"productoId\":%d,\"cantidad\":1}"
            .formatted(jarron, maceta));
        vender("TARJETA", "null", "{\"productoId\":%d,\"cantidad\":1}".formatted(maceta));
        long liquidado = pedido("100");
        postJson("/api/pedidos/{id}/pagos", "{\"monto\":220,\"metodoPago\":\"TARJETA\"}", liquidado).andExpect(status().isOk());
        long cancelado = pedido("50");
        comoPropietario();
        mvc.perform(post("/api/pedidos/{id}/cancelar", cancelado)).andExpect(status().isNoContent());
        // Cancelado antes de existir las filas DEVOLUCION: se descuenta en el corte donde se cobró.
        long viejo = id("""
            insert into barrocontrol.pedido (id_sucursal, id_usuario, id_cliente, folio, tipo_entrega, estado)
            select ?, ?, id_cliente, 'PED-VIEJO', 'LOCAL', 'CANCELADO' from barrocontrol.pedido where id = ?
            returning id
            """, sucursal, propietario, cancelado);
        jdbc.update("""
            insert into barrocontrol.pago_pedido (id_pedido, id_corte_caja, monto, metodo_pago, tipo_pago)
            values (?, ?, 30, 'TARJETA', 'ANTICIPO')
            """, viejo, corte);

        cerrar(corte).andExpect(status().isOk())
            .andExpect(jsonPath("$.cerrado").value(true))
            .andExpect(jsonPath("$.totalVentas").value(370.00))
            .andExpect(jsonPath("$.totalAnticipos").value(180.00))
            .andExpect(jsonPath("$.totalLiquidaciones").value(220.00))
            .andExpect(jsonPath("$.totalCancelaciones").value(80.00))
            .andExpect(jsonPath("$.totalNeto").value(690.00))
            .andExpect(jsonPath("$.totalEfectivo").value(620.00))
            .andExpect(jsonPath("$.totalTarjeta").value(270.00))
            .andExpect(jsonPath("$.totalEstimado").value(890.00))
            .andExpect(jsonPath("$.diferencia").value(10.00))
            .andExpect(jsonPath("$.cancelacionesPedidos.detalles.length()").value(2))
            .andExpect(jsonPath("$.productos[0].nombre").value("Jarrón"))
            .andExpect(jsonPath("$.productos[0].cantidad").value(6))
            .andExpect(jsonPath("$.productos[1].cantidad").value(3));
        cerrar(corte).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CORTE_CAJA_YA_CERRADO"));
        cerrar(999_999).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("CORTE_CAJA_NO_ENCONTRADO"));

        getJson("/api/cortes-caja/{id}", corte).andExpect(status().isOk()).andExpect(jsonPath("$.totalReal").value(900.00));
        getJson("/api/cortes-caja/{id}", 999_999).andExpect(status().isNotFound());
        getJson("/api/cortes-caja?sucursalId={s}", sucursal).andExpect(jsonPath("$.content[0].id").value(corte));
        getJson("/api/cortes-caja").andExpect(jsonPath("$.content[0].diferencia").value(10.00));
        getJson("/api/cortes-caja/activo?sucursalId={s}", sucursal)
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("CORTE_CAJA_NO_ABIERTO"));
        // El historial del empleado (si su token lo permitiera) se limita a su sucursal.
        como(empleado, "PROPIETARIO");
        getJson("/api/cortes-caja").andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void elEmpleadoSoloOperaSusSucursales() throws Exception {
        // Los cambios por SQL van antes de la primera petición: después Hibernate ya tiene al usuario en memoria.
        long otra = id("insert into barrocontrol.sucursal (id_negocio, nombre) values (?, 'Norte') returning id", negocio);
        jdbc.update("insert into barrocontrol.empleado_sucursal (id_usuario, id_sucursal) values (?, ?)", empleado, otra);
        long sinSucursal = id("""
            insert into barrocontrol.usuario (id_rol, id_negocio, nombre, correo, password_hash)
            select id_rol, id_negocio, 'Nuevo', 'nuevo@prueba.test', 'hash' from barrocontrol.usuario where id = ?
            returning id
            """, empleado);
        comoEmpleado();
        abrir(sucursalAjena).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        abrir(null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
        abrir(otra).andExpect(status().isCreated());
        como(sinSucursal, "EMPLEADO");
        getJson("/api/cortes-caja/activo").andExpect(status().isForbidden());
    }

    @Test
    void elPropietarioIndicaLaSucursalSiTieneVarias() throws Exception {
        comoPropietario();
        abrir(null).andExpect(status().isCreated());
        id("insert into barrocontrol.sucursal (id_negocio, nombre) values (?, 'Norte') returning id", negocio);
        getJson("/api/cortes-caja/activo").andExpect(status().isBadRequest());
        getJson("/api/cortes-caja?sucursalId={s}", sucursalAjena).andExpect(status().isForbidden());
        getJson("/api/cortes-caja/activo?sucursalId={s}", sucursalAjena).andExpect(status().isForbidden());

        long sinNegocio = id("""
            insert into barrocontrol.usuario (id_rol, nombre, correo, password_hash)
            select id_rol, 'Sin negocio', 'sin@prueba.test', 'hash' from barrocontrol.usuario where id = ?
            returning id
            """, propietario);
        como(sinNegocio, "PROPIETARIO");
        getJson("/api/cortes-caja/activo").andExpect(status().isBadRequest());
        getJson("/api/cortes-caja").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rechazaUsuariosSinRolDeCajaOInexistentes() throws Exception {
        long rolOtro = id("insert into barrocontrol.rol (nombre) values ('AUDITOR') returning id");
        long auditor = id("""
            insert into barrocontrol.usuario (id_rol, nombre, correo, password_hash)
            values (?, 'Audi', 'audi@prueba.test', 'hash') returning id
            """, rolOtro);
        como(auditor, "PROPIETARIO");
        abrir(sucursal).andExpect(status().isForbidden());
        getJson("/api/cortes-caja").andExpect(status().isForbidden());
        como(999_999, "PROPIETARIO");
        abrir(sucursal).andExpect(status().isForbidden());
        getJson("/api/cortes-caja").andExpect(status().isForbidden());
    }
}
