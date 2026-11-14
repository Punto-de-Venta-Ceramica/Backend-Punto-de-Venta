package com.iaml.puntoventaapi.caja.application;

import com.iaml.puntoventaapi.caja.api.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Component
public class CalculadoraCorte {

    private final JdbcTemplate jdbc;

    public CalculadoraCorte(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ItemVenta(
        Long productoId,
        String productoNombre,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        String metodoPago
    ) {}

    public record ItemPago(
        Long pagoId,
        Long pedidoId,
        String folio,
        String estadoPedido,
        BigDecimal monto,
        String metodoPago,
        String tipoPago,
        Instant fechaHora
    ) {}

    public record ItemPedidoProducto(
        Long productoId,
        String productoNombre,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
    ) {}

    public record ResultadoCalculo(
        BigDecimal totalVentas,
        BigDecimal totalAnticipos,
        BigDecimal totalLiquidaciones,
        BigDecimal totalCancelaciones,
        BigDecimal totalNeto,
        DesgloseMetodoPagoDto ventasEfectivo,
        DesgloseMetodoPagoDto ventasTarjeta,
        SeccionCancelacionesDto cancelacionesPedidos,
        BigDecimal totalEfectivo,
        BigDecimal totalTarjeta,
        BigDecimal totalEstimado,
        List<ProductoVendidoDto> productos
    ) {}

    public ResultadoCalculo calcular(Long corteId, BigDecimal montoInicial) {
        List<ItemVenta> ventas = consultarVentas(corteId);
        List<ItemPago> pagos = consultarPagos(corteId);
        List<ItemPedidoProducto> productosPedido = consultarProductosPedido(corteId);
        return procesarCalculo(montoInicial, ventas, pagos, productosPedido);
    }

    public ResultadoCalculo procesarCalculo(
        BigDecimal montoInicial,
        List<ItemVenta> ventas,
        List<ItemPago> pagos,
        List<ItemPedidoProducto> productosPedido
    ) {
        BigDecimal seguroMontoInicial = montoInicial != null ? montoInicial.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);

        BigDecimal ventasEfectivo = BigDecimal.ZERO.setScale(2);
        BigDecimal ventasTarjeta = BigDecimal.ZERO.setScale(2);

        // Agrupación de productos
        Map<String, AcumuladorProducto> productosMap = new LinkedHashMap<>();

        for (ItemVenta v : ventas) {
            BigDecimal subtotal = v.subtotal() != null ? v.subtotal().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
            if ("TARJETA".equalsIgnoreCase(v.metodoPago())) {
                ventasTarjeta = ventasTarjeta.add(subtotal);
            } else {
                ventasEfectivo = ventasEfectivo.add(subtotal);
            }

            String key = v.productoId() + "_" + v.precioUnitario();
            productosMap.computeIfAbsent(key, k -> new AcumuladorProducto(v.productoId(), v.productoNombre(), v.precioUnitario()))
                .acumular(v.cantidad(), subtotal);
        }

        BigDecimal totalVentas = ventasEfectivo.add(ventasTarjeta);

        // Desglose de pagos y pedidos
        BigDecimal anticiposEfectivo = BigDecimal.ZERO.setScale(2);
        BigDecimal anticiposTarjeta = BigDecimal.ZERO.setScale(2);
        BigDecimal liquidacionesEfectivo = BigDecimal.ZERO.setScale(2);
        BigDecimal liquidacionesTarjeta = BigDecimal.ZERO.setScale(2);
        BigDecimal cancelacionesEfectivo = BigDecimal.ZERO.setScale(2);
        BigDecimal cancelacionesTarjeta = BigDecimal.ZERO.setScale(2);

        List<CancelacionPedidoDto> detallesCancelaciones = new ArrayList<>();

        for (ItemPago p : pagos) {
            BigDecimal monto = p.monto() != null ? p.monto().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
            boolean esTarjeta = "TARJETA".equalsIgnoreCase(p.metodoPago());
            boolean esCancelado = "CANCELADO".equalsIgnoreCase(p.estadoPedido());

            if ("ANTICIPO".equalsIgnoreCase(p.tipoPago())) {
                if (esTarjeta) {
                    anticiposTarjeta = anticiposTarjeta.add(monto);
                } else {
                    anticiposEfectivo = anticiposEfectivo.add(monto);
                }
            } else if ("LIQUIDACION".equalsIgnoreCase(p.tipoPago())) {
                if (esTarjeta) {
                    liquidacionesTarjeta = liquidacionesTarjeta.add(monto);
                } else {
                    liquidacionesEfectivo = liquidacionesEfectivo.add(monto);
                }
            }

            if (esCancelado) {
                // Al cancelar un pedido, se descuenta automáticamente el anticipo/pago realizado
                if (esTarjeta) {
                    cancelacionesTarjeta = cancelacionesTarjeta.add(monto);
                } else {
                    cancelacionesEfectivo = cancelacionesEfectivo.add(monto);
                }
                detallesCancelaciones.add(new CancelacionPedidoDto(
                    p.pedidoId(),
                    p.folio(),
                    monto,
                    p.metodoPago(),
                    p.tipoPago(),
                    p.fechaHora()
                ));
            }
        }

        // Incorporar productos de pedidos no cancelados
        if (productosPedido != null) {
            for (ItemPedidoProducto pp : productosPedido) {
                String key = pp.productoId() + "_" + pp.precioUnitario();
                BigDecimal subtotal = pp.subtotal() != null ? pp.subtotal().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                productosMap.computeIfAbsent(key, k -> new AcumuladorProducto(pp.productoId(), pp.productoNombre(), pp.precioUnitario()))
                    .acumular(pp.cantidad(), subtotal);
            }
        }

        List<ProductoVendidoDto> productos = productosMap.values().stream()
            .map(AcumuladorProducto::toDto)
            .sorted(Comparator.comparing(ProductoVendidoDto::nombre, String.CASE_INSENSITIVE_ORDER))
            .toList();

        BigDecimal totalAnticipos = anticiposEfectivo.add(anticiposTarjeta);
        BigDecimal totalLiquidaciones = liquidacionesEfectivo.add(liquidacionesTarjeta);
        BigDecimal totalCancelaciones = cancelacionesEfectivo.add(cancelacionesTarjeta);

        // Desglose efectivo
        BigDecimal subtotalNetoEfectivo = ventasEfectivo
            .add(anticiposEfectivo)
            .add(liquidacionesEfectivo)
            .subtract(cancelacionesEfectivo);
        BigDecimal totalEsperadoEfectivo = seguroMontoInicial.add(subtotalNetoEfectivo);
        if (totalEsperadoEfectivo.compareTo(BigDecimal.ZERO) < 0) {
            totalEsperadoEfectivo = BigDecimal.ZERO.setScale(2);
        }

        DesgloseMetodoPagoDto seccionEfectivo = new DesgloseMetodoPagoDto(
            ventasEfectivo,
            anticiposEfectivo,
            liquidacionesEfectivo,
            cancelacionesEfectivo,
            subtotalNetoEfectivo,
            seguroMontoInicial,
            totalEsperadoEfectivo
        );

        // Desglose tarjeta
        BigDecimal subtotalNetoTarjeta = ventasTarjeta
            .add(anticiposTarjeta)
            .add(liquidacionesTarjeta)
            .subtract(cancelacionesTarjeta);
        BigDecimal totalEsperadoTarjeta = subtotalNetoTarjeta.compareTo(BigDecimal.ZERO) < 0
            ? BigDecimal.ZERO.setScale(2)
            : subtotalNetoTarjeta;

        DesgloseMetodoPagoDto seccionTarjeta = new DesgloseMetodoPagoDto(
            ventasTarjeta,
            anticiposTarjeta,
            liquidacionesTarjeta,
            cancelacionesTarjeta,
            subtotalNetoTarjeta,
            BigDecimal.ZERO.setScale(2),
            totalEsperadoTarjeta
        );

        SeccionCancelacionesDto seccionCancelaciones = new SeccionCancelacionesDto(
            totalCancelaciones,
            cancelacionesEfectivo,
            cancelacionesTarjeta,
            detallesCancelaciones
        );

        BigDecimal totalNeto = subtotalNetoEfectivo.add(subtotalNetoTarjeta);
        BigDecimal totalEfectivo = totalEsperadoEfectivo;
        BigDecimal totalTarjeta = totalEsperadoTarjeta;
        BigDecimal totalEstimado = totalEfectivo.add(totalTarjeta);

        return new ResultadoCalculo(
            totalVentas,
            totalAnticipos,
            totalLiquidaciones,
            totalCancelaciones,
            totalNeto,
            seccionEfectivo,
            seccionTarjeta,
            seccionCancelaciones,
            totalEfectivo,
            totalTarjeta,
            totalEstimado,
            productos
        );
    }

    private List<ItemVenta> consultarVentas(Long corteId) {
        String sql = """
            select dv.id_producto, p.nombre, dv.cantidad, dv.precio_unitario, dv.subtotal, v.metodo_pago
            from barrocontrol.venta v
            join barrocontrol.detalle_venta dv on dv.id_venta = v.id
            join barrocontrol.producto p on p.id = dv.id_producto
            where v.id_corte_caja = ?
            order by v.fecha_hora asc
            """;
        return jdbc.query(sql, (rs, i) -> new ItemVenta(
            rs.getLong(1),
            rs.getString(2),
            rs.getInt(3),
            rs.getBigDecimal(4),
            rs.getBigDecimal(5),
            rs.getString(6)
        ), corteId);
    }

    private List<ItemPago> consultarPagos(Long corteId) {
        String sql = """
            select pp.id, pp.id_pedido, p.folio, p.estado, pp.monto, pp.metodo_pago, pp.tipo_pago, pp.fecha_hora
            from barrocontrol.pago_pedido pp
            join barrocontrol.pedido p on p.id = pp.id_pedido
            where pp.id_corte_caja = ?
            order by pp.fecha_hora asc
            """;
        return jdbc.query(sql, (rs, i) -> new ItemPago(
            rs.getLong(1),
            rs.getLong(2),
            rs.getString(3),
            rs.getString(4),
            rs.getBigDecimal(5),
            rs.getString(6),
            rs.getString(7),
            rs.getTimestamp(8).toInstant()
        ), corteId);
    }

    private List<ItemPedidoProducto> consultarProductosPedido(Long corteId) {
        String sql = """
            select dp.id_producto, p.nombre, dp.cantidad, dp.precio_unitario, dp.subtotal
            from barrocontrol.pago_pedido pp
            join barrocontrol.pedido ped on ped.id = pp.id_pedido
            join barrocontrol.detalle_pedido dp on dp.id_pedido = ped.id
            join barrocontrol.producto p on p.id = dp.id_producto
            where pp.id_corte_caja = ? and pp.tipo_pago = 'ANTICIPO' and ped.estado <> 'CANCELADO'
            order by p.nombre asc
            """;
        return jdbc.query(sql, (rs, i) -> new ItemPedidoProducto(
            rs.getLong(1),
            rs.getString(2),
            rs.getInt(3),
            rs.getBigDecimal(4),
            rs.getBigDecimal(5)
        ), corteId);
    }

    private static class AcumuladorProducto {
        private final Long idProducto;
        private final String nombre;
        private final BigDecimal precioUnitario;
        private int cantidad;
        private BigDecimal subtotal;

        public AcumuladorProducto(Long idProducto, String nombre, BigDecimal precioUnitario) {
            this.idProducto = idProducto;
            this.nombre = nombre;
            this.precioUnitario = precioUnitario != null ? precioUnitario.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
            this.cantidad = 0;
            this.subtotal = BigDecimal.ZERO.setScale(2);
        }

        public void acumular(int cant, BigDecimal sub) {
            this.cantidad += cant;
            if (sub != null) {
                this.subtotal = this.subtotal.add(sub.setScale(2, RoundingMode.HALF_UP));
            }
        }

        public ProductoVendidoDto toDto() {
            return new ProductoVendidoDto(idProducto, nombre, cantidad, precioUnitario, subtotal);
        }
    }
}
