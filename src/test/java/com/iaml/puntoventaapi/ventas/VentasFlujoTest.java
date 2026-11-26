package com.iaml.puntoventaapi.ventas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iaml.puntoventaapi.FlujoApiDePrueba;
import com.iaml.puntoventaapi.caja.application.CalculadoraCorte;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.ResultActions;

/** HU-08: venta de punta a punta (catálogo, existencia, corte de caja y ticket) en PostgreSQL. */
class VentasFlujoTest extends FlujoApiDePrueba {

    @Autowired private CalculadoraCorte calculadoraCorte;

    private String venta(String metodo, String monto, String productos) {
        return """
            {"sucursalId":%d,"metodoPago":%s,"montoRecibido":%s,"productos":[%s]}
            """.formatted(sucursal, metodo == null ? "null" : "\"" + metodo + "\"", monto, productos);
    }

    private String linea(long producto, int cantidad, boolean descuento) {
        return "{\"productoId\":%d,\"cantidad\":%d,\"aplicarDescuento\":%s}".formatted(producto, cantidad, descuento);
    }

    private String carrito() {
        return venta("EFECTIVO", "500", linea(jarron, 3, true) + "," + linea(maceta, 1, false));
    }

    private ResultActions confirmar(String json, UUID clave) throws Exception {
        var peticion = post("/api/ventas/confirmar").contentType("application/json").content(json);
        return mvc.perform(clave == null ? peticion : peticion.header("Idempotency-Key", clave.toString()));
    }

    @Test
    void listaLosProductosDeLaSucursalConSusDescuentos() throws Exception {
        comoEmpleado();
        getJson("/api/ventas/productos?sucursalId={s}", sucursal)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(2))
            .andExpect(jsonPath("$.ultima").value(true))
            .andExpect(jsonPath("$.contenido[?(@.nombre == 'Jarrón')].descuentos.length()").value(2))
            .andExpect(jsonPath("$.contenido[?(@.nombre == 'Maceta')].existencia").value(5));
        getJson("/api/ventas/productos?sucursalId={s}&size=1", sucursal)
            .andExpect(jsonPath("$.ultima").value(false));
        getJson("/api/ventas/productos?sucursalId={s}&precioMinimo=60", sucursal)
            .andExpect(jsonPath("$.totalElementos").value(1));
        getJson("/api/ventas/productos?sucursalId={s}&precioMinimo=10&precioMaximo=60", sucursal)
            .andExpect(jsonPath("$.contenido[0].nombre").value("Maceta"));
    }

    @Test
    void rechazaFiltrosDeListadoInvalidos() throws Exception {
        comoEmpleado();
        getJson("/api/ventas/productos?sucursalId={s}&precioMinimo=90&precioMaximo=10", sucursal)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.precioMaximo").exists());
        getJson("/api/ventas/productos?sucursalId={s}&page=30000000&size=100", sucursal)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.page").exists());
        getJson("/api/ventas/productos?sucursalId={s}", sucursalAjena)
            .andExpect(status().isNotFound());
    }

    @Test
    void preparaLaVentaConPreciosYDescuentosDelServidor() throws Exception {
        comoEmpleado();
        postJson("/api/ventas/preparar", carrito())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.confirmada").value(false))
            .andExpect(jsonPath("$.subtotal").value(350.00))
            .andExpect(jsonPath("$.descuento").value(30.00))
            .andExpect(jsonPath("$.total").value(320.00))
            .andExpect(jsonPath("$.cambio").value(180.00));
    }

    @Test
    void rechazaPrepararSinDescuentoSinExistenciaOProductoAjeno() throws Exception {
        comoEmpleado();
        postJson("/api/ventas/preparar", venta("TARJETA", "null", linea(maceta, 1, true)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("DESCUENTO_VENTA_NO_DISPONIBLE"))
            .andExpect(jsonPath("$.productoId").value(maceta));
        postJson("/api/ventas/preparar", venta("TARJETA", "null", linea(maceta, 6, false)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.cantidadSolicitada").value(6))
            .andExpect(jsonPath("$.existenciaDisponible").value(5));
        postJson("/api/ventas/preparar", venta("TARJETA", "null", linea(999_999, 1, false)))
            .andExpect(status().isNotFound());
        postJson("/api/ventas/preparar", venta("TARJETA", "10", linea(maceta, 1, false)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.montoRecibido").exists());
    }

    @Test
    void confirmarExigeMetodoMontoYCorteAbierto() throws Exception {
        comoEmpleado();
        confirmar(venta(null, "null", linea(maceta, 1, false)), null)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.metodoPago").exists());
        confirmar(venta("EFECTIVO", "null", linea(maceta, 1, false)), null)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errores.montoRecibido").exists());
        confirmar(carrito(), null)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Abre un corte de caja para confirmar la venta."));
    }

    @Test
    void confirmaDescuentaExistenciaYDevuelveLaMismaVentaAlReintentar() throws Exception {
        long corte = abrirCorte(sucursal, empleado);
        comoEmpleado();
        UUID clave = UUID.randomUUID();
        long ventaId = leerId(confirmar(carrito(), clave)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.folio").value(org.hamcrest.Matchers.startsWith("VTA-")))
            .andExpect(jsonPath("$.corteCajaId").value(corte))
            .andExpect(jsonPath("$.negocioEslogan").value("Hecho a mano")), "$.ventaId");
        assertThat(jdbc.queryForObject("select existencia from barrocontrol.inventario where id_producto = ?",
            Integer.class, jarron)).isEqualTo(7);

        confirmar(carrito(), clave).andExpect(status().isCreated()).andExpect(jsonPath("$.ventaId").value(ventaId));
        getJson("/api/ventas/{id}/ticket", ventaId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(320.00))
            .andExpect(jsonPath("$.detalles[0].nombreProducto").value("Jarrón"));

        UUID tarjeta = UUID.randomUUID();
        confirmar(venta("TARJETA", "null", linea(maceta, 1, false)), tarjeta).andExpect(status().isCreated());
        confirmar(venta("TARJETA", "null", linea(maceta, 1, false)), tarjeta).andExpect(status().isCreated());
        confirmar(venta("TARJETA", "null", linea(maceta, 1, false)), null).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select count(*) from barrocontrol.venta", Integer.class)).isEqualTo(3);
    }

    @Test
    void rechazaReutilizarLaClaveParaOtraVenta() throws Exception {
        abrirCorte(sucursal, empleado);
        comoEmpleado();
        UUID clave = UUID.randomUUID();
        confirmar(carrito(), clave).andExpect(status().isCreated());
        List<String> distintas = List.of(
            venta("TARJETA", "null", linea(jarron, 3, true) + "," + linea(maceta, 1, false)),
            venta("EFECTIVO", "600", linea(jarron, 3, true) + "," + linea(maceta, 1, false)),
            venta("EFECTIVO", "500", linea(jarron, 2, true) + "," + linea(maceta, 1, false)),
            venta("EFECTIVO", "500", linea(jarron, 3, false) + "," + linea(maceta, 1, false)));
        for (String json : distintas) {
            confirmar(json, clave).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La clave de operación ya se usó para otra venta."));
        }
        comoPropietario();
        confirmar(carrito(), clave).andExpect(status().isConflict());
    }

    @Test
    void elTicketSoloLoVeQuienTieneAccesoALaSucursal() throws Exception {
        long corte = abrirCorte(sucursal, empleado);
        // Venta anterior al nombre guardado en el detalle: el ticket usa el nombre actual del producto.
        long caja = id("select id_caja from barrocontrol.corte_caja where id = ?", corte);
        long ventaId = id("""
            insert into barrocontrol.venta (id_usuario, id_caja, id_corte_caja, metodo_pago, folio)
            values (?, ?, ?, 'TARJETA', 'VTA-VIEJA') returning id
            """, empleado, caja, corte);
        jdbc.update("""
            insert into barrocontrol.detalle_venta (id_venta, id_producto, cantidad, precio_unitario, desc_aplicado)
            values (?, ?, 1, 50, 0)
            """, ventaId, maceta);
        comoPropietario();
        getJson("/api/ventas/{id}/ticket", ventaId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.folio").value("VTA-VIEJA"))
            .andExpect(jsonPath("$.detalles[0].nombreProducto").value("Maceta"));
        como(ajeno, "PROPIETARIO");
        getJson("/api/ventas/{id}/ticket", ventaId).andExpect(status().isNotFound());
        getJson("/api/ventas/{id}/ticket", 999_999).andExpect(status().isNotFound());
    }

    @Test
    void sinUsuarioValidoResponde401() throws Exception {
        var jwt = Jwt.withTokenValue("prueba").header("alg", "none").subject("no-numerico").build();
        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_EMPLEADO"))));
        postJson("/api/ventas/preparar", carrito()).andExpect(status().isUnauthorized());
    }

    /** Línea de "Artículo artesanal": precio y nombre los pone el cajero. */
    private String libre(long producto, int cantidad, String precio, String nombre) {
        return "{\"productoId\":%d,\"cantidad\":%d,\"aplicarDescuento\":false,\"precioUnitario\":%s,\"nombre\":%s}"
            .formatted(producto, cantidad, precio, nombre == null ? "null" : "\"" + nombre + "\"");
    }

    private long productoLibre() throws Exception {
        return leerId(getJson("/api/ventas/producto-libre?sucursalId={s}", sucursal).andExpect(status().isOk()),
            "$.productoId");
    }

    @Test
    void variosSeCreaUnaVezPorNegocioYNoApareceEnElCatalogo() throws Exception {
        comoEmpleado();
        long varios = productoLibre();
        assertThat(productoLibre()).isEqualTo(varios);
        getJson("/api/ventas/producto-libre?sucursalId={s}", sucursal)
            .andExpect(jsonPath("$.nombre").value("Artículo artesanal"));
        getJson("/api/ventas/producto-libre?sucursalId={s}", sucursalAjena).andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("select count(*) from barrocontrol.producto where precio_variable", Integer.class))
            .isEqualTo(1);
        // Cuelga de un área y una categoría dadas de baja: ninguna consulta del catálogo lo ve.
        assertThat(jdbc.queryForObject("""
            select count(*) from barrocontrol.producto p
            join barrocontrol.categoria c on c.id = p.id_categoria
            join barrocontrol.area a on a.id = c.id_area
            where p.precio_variable and not c.activo and not a.activo and a.id_negocio = ?
            """, Integer.class, negocio)).isEqualTo(1);
        getJson("/api/ventas/productos?sucursalId={s}", sucursal)
            .andExpect(jsonPath("$.totalElementos").value(2));
    }

    @Test
    void cobraSoloVariosConPrecioYNombreSinTocarElInventario() throws Exception {
        long corte = abrirCorte(sucursal, empleado);
        comoEmpleado();
        long varios = productoLibre();
        String json = venta("TARJETA", "null", libre(varios, 5, "10.00", "Servilletas"));

        postJson("/api/ventas/preparar", json)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.detalles[0].nombreProducto").value("Servilletas"))
            .andExpect(jsonPath("$.total").value(50.00));
        confirmar(json, UUID.randomUUID()).andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(50.00));

        assertThat(jdbc.queryForObject("select nombre_producto from barrocontrol.detalle_venta where id_producto = ?",
            String.class, varios)).isEqualTo("Servilletas");
        assertThat(jdbc.queryForObject("select sum(existencia) from barrocontrol.inventario", Integer.class)).isEqualTo(15);
        // El corte muestra el nombre que se cobró y suma el importe.
        var resultado = calculadoraCorte.calcular(corte, BigDecimal.ZERO);
        assertThat(resultado.productos()).anySatisfy(p -> {
            assertThat(p.nombre()).isEqualTo("Servilletas");
            assertThat(p.cantidad()).isEqualTo(5);
        });
        assertThat(resultado.totalVentas()).isEqualByComparingTo("50.00");
    }

    @Test
    void laVentaMixtaDescuentaSoloLoQueTieneInventario() throws Exception {
        abrirCorte(sucursal, empleado);
        comoEmpleado();
        long varios = productoLibre();
        confirmar(venta("EFECTIVO", "500", linea(maceta, 2, false) + "," + libre(varios, 1, "25.50", null)),
            UUID.randomUUID())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total").value(125.50));
        assertThat(jdbc.queryForObject("select existencia from barrocontrol.inventario where id_producto = ?",
            Integer.class, maceta)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select nombre_producto from barrocontrol.detalle_venta where id_producto = ?",
            String.class, varios)).isEqualTo("Artículo artesanal");
    }

    @Test
    void rechazaPrecioNombreOMalUsoDeVarios() throws Exception {
        comoEmpleado();
        long varios = productoLibre();
        // Un producto normal nunca acepta precio ni nombre del cliente.
        postJson("/api/ventas/preparar", venta("TARJETA", "null", libre(maceta, 1, "1.00", null)))
            .andExpect(status().isBadRequest());
        postJson("/api/ventas/preparar", venta("TARJETA", "null", libre(maceta, 1, "null", "Otro")))
            .andExpect(status().isBadRequest());
        // Artículo artesanal sin precio, con descuento o con un precio inválido.
        postJson("/api/ventas/preparar", venta("TARJETA", "null", linea(varios, 1, false)))
            .andExpect(status().isBadRequest());
        postJson("/api/ventas/preparar", venta("TARJETA", "null", linea(varios, 1, true)))
            .andExpect(status().isBadRequest());
        postJson("/api/ventas/preparar", venta("TARJETA", "null", libre(varios, 1, "0", null)))
            .andExpect(status().isBadRequest());
        postJson("/api/ventas/preparar", venta("TARJETA", "null", libre(varios, 1, "10.005", null)))
            .andExpect(status().isBadRequest());
        postJson("/api/ventas/preparar", venta("TARJETA", "null", libre(varios, 1, "1000000.00", null)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void elReintentoDeVariosConOtroPrecioONombreEsOtraVenta() throws Exception {
        abrirCorte(sucursal, empleado);
        comoEmpleado();
        long varios = productoLibre();
        UUID clave = UUID.randomUUID();
        String original = venta("TARJETA", "null", libre(varios, 5, "10.00", "Servilletas"));
        long ventaId = leerId(confirmar(original, clave).andExpect(status().isCreated()), "$.ventaId");

        confirmar(original, clave).andExpect(status().isCreated()).andExpect(jsonPath("$.ventaId").value(ventaId));
        confirmar(venta("TARJETA", "null", libre(varios, 5, "12.00", "Servilletas")), clave)
            .andExpect(status().isConflict());
        confirmar(venta("TARJETA", "null", libre(varios, 5, "10.00", "Bolsas")), clave)
            .andExpect(status().isConflict());
    }
}
