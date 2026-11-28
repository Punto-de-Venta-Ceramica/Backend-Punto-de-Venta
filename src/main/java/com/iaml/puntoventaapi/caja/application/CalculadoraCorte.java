package com.iaml.puntoventaapi.caja.application;

import com.iaml.puntoventaapi.caja.api.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
        List<ItemPago> cancelaciones = consultarCancelaciones(corteId);
        List<ItemPedidoProducto> productosPedido = consultarProductosPedido(corteId);
        return procesarCalculo(montoInicial, ventas, pagos, cancelaciones, productosPedido);
    }

    /**
     * @param pagos         pagos de pedidos cobrados en este corte (entran a caja).
     * @param cancelaciones pagos de los pedidos cancelados en este corte, sin importar en
     *                      qué corte se cobraron (salen de caja como devolución).
     */
    public ResultadoCalculo procesarCalculo(
        BigDecimal montoInicial,
        List<ItemVenta> ventas,
        List<ItemPago> pagos,
        List<ItemPago> cancelaciones,
        List<ItemPedidoProducto> productosPedido
    ) {
        BigDecimal seguroMontoInicial = montoInicial != null ? montoInicial.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);

        BigDecimal ventasEfectivo = BigDecimal.ZERO.setScale(2);
        BigDecimal ventasTarjeta = BigDecimal.ZERO.setScale(2);

        // Agrupación de productos
        Map<String, AcumuladorProducto> productosMap = new LinkedHashMap<>();

        for (ItemVenta v : ventas) {
            BigDecimal subtotal = v.subtotal() != null ? v.subtotal().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
            String metodo = "TARJETA".equalsIgnoreCase(v.metodoPago()) ? "TARJETA" : "EFECTIVO";
            if (metodo.equals("TARJETA")) {
                ventasTarjeta = ventasTarjeta.add(subtotal);
            } else {
                ventasEfectivo = ventasEfectivo.add(subtotal);
            }

            // Artículo artesanal se vende con nombres distintos bajo un mismo id: el nombre entra en la clave. En el
            // resto de productos el nombre es el mismo por id, así que agrupan igual que siempre.
            // El método de pago también separa renglones para mostrar qué se cobró en efectivo y qué con tarjeta.
            String key = v.productoId() + "_" + v.precioUnitario() + "_" + v.productoNombre() + "_" + metodo;
            productosMap.computeIfAbsent(key, k -> new AcumuladorProducto(v.productoId(), v.productoNombre(), v.precioUnitario(), metodo))
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
        }

        // Al cancelar un pedido se devuelve lo pagado, con el mismo método de cada pago.
        for (ItemPago p : cancelaciones != null ? cancelaciones : List.<ItemPago>of()) {
            BigDecimal monto = p.monto() != null ? p.monto().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
            boolean esTarjeta = "TARJETA".equalsIgnoreCase(p.metodoPago());
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

        // Incorporar productos de pedidos no cancelados
        if (productosPedido != null) {
            for (ItemPedidoProducto pp : productosPedido) {
                // Los pedidos van aparte: su cobro está en anticipos y liquidaciones, no en las ventas por método.
                String key = pp.productoId() + "_" + pp.precioUnitario() + "_" + pp.productoNombre() + "_PEDIDO";
                BigDecimal subtotal = pp.subtotal() != null ? pp.subtotal().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                productosMap.computeIfAbsent(key, k -> new AcumuladorProducto(pp.productoId(), pp.productoNombre(), pp.precioUnitario(), null))
                    .acumular(pp.cantidad(), subtotal);
            }
        }

        List<ProductoVendidoDto> productos = productosMap.values().stream()
            .map(AcumuladorProducto::toDto)
            .sorted(Comparator.comparing(ProductoVendidoDto::nombre, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ProductoVendidoDto::metodoPago, Comparator.nullsLast(Comparator.naturalOrder())))
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
        // El estimado en caja para el arqueo considera únicamente el efectivo físico (sin mezclar tarjeta).
        BigDecimal totalEstimado = totalEfectivo;

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
            select dv.id_producto,
                   case when p.precio_variable then coalesce(dv.nombre_producto, p.nombre) else p.nombre end,
                   dv.cantidad, dv.precio_unitario, dv.subtotal, v.metodo_pago
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
            where pp.id_corte_caja = ? and pp.tipo_pago <> 'DEVOLUCION'
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

    /**
     * Lo devuelto en este corte por pedidos cancelados. Al cancelar, el módulo de pedidos
     * inserta en el corte abierto una fila 'DEVOLUCION' por cada pago, aunque el cobro sea
     * de un corte anterior. Los pedidos cancelados antes de existir esas filas se
     * descuentan, como antes, en el corte donde se cobró cada pago.
     */
    private List<ItemPago> consultarCancelaciones(Long corteId) {
        String sql = """
            select pp.id, pp.id_pedido, p.folio, p.estado, pp.monto, pp.metodo_pago, pp.tipo_pago, pp.fecha_hora
            from barrocontrol.pago_pedido pp
            join barrocontrol.pedido p on p.id = pp.id_pedido
            where pp.id_corte_caja = ?
              and (pp.tipo_pago = 'DEVOLUCION'
                   or (p.estado = 'CANCELADO'
                       and not exists (select 1 from barrocontrol.pago_pedido dv
                                       where dv.id_pedido = p.id and dv.tipo_pago = 'DEVOLUCION')))
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

    /**
     * Productos de los pedidos no cancelados cuyo primer pago se cobró en este corte,
     * sea anticipo o liquidación completa. Así cada pedido se cuenta una sola vez.
     */
    private List<ItemPedidoProducto> consultarProductosPedido(Long corteId) {
        String sql = """
            select dp.id_producto, p.nombre, dp.cantidad, dp.precio_unitario, dp.subtotal
            from barrocontrol.pago_pedido pp
            join barrocontrol.pedido ped on ped.id = pp.id_pedido
            join barrocontrol.detalle_pedido dp on dp.id_pedido = ped.id
            join barrocontrol.producto p on p.id = dp.id_producto
            where pp.id_corte_caja = ? and ped.estado <> 'CANCELADO'
              and pp.id = (select min(primero.id) from barrocontrol.pago_pedido primero
                           where primero.id_pedido = ped.id)
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

    public record FilaPedido(Long id, String folio, BigDecimal total, BigDecimal pagado, boolean cancelado) {}

    public record FilaPago(Long pedidoId, String tipoPago, String metodoPago, BigDecimal monto, Instant fechaHora) {}

    public record FilaProducto(Long pedidoId, String nombre, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}

    /** Pedidos con algún pago, liquidación o devolución en este corte, con sus productos y lo que faltaba entonces. */
    public List<PedidoCorteDto> pedidos(Long corteId) {
        List<FilaPedido> pedidos = jdbc.query("""
            select p.id, p.folio,
                   (select coalesce(sum(dp.subtotal), 0) from barrocontrol.detalle_pedido dp where dp.id_pedido = p.id),
                   -- Lo pagado hasta el último movimiento de este corte, no lo de cortes posteriores.
                   (select coalesce(sum(x.monto), 0) from barrocontrol.pago_pedido x
                     where x.id_pedido = p.id and x.tipo_pago <> 'DEVOLUCION'
                       and x.id <= (select max(y.id) from barrocontrol.pago_pedido y
                                    where y.id_pedido = p.id and y.id_corte_caja = ?)),
                   exists (select 1 from barrocontrol.pago_pedido d
                           where d.id_pedido = p.id and d.id_corte_caja = ? and d.tipo_pago = 'DEVOLUCION')
                   or (p.estado = 'CANCELADO'
                       and not exists (select 1 from barrocontrol.pago_pedido d
                                       where d.id_pedido = p.id and d.tipo_pago = 'DEVOLUCION'))
            from barrocontrol.pedido p
            where p.id in (select id_pedido from barrocontrol.pago_pedido where id_corte_caja = ?)
            order by p.folio
            """, (rs, i) -> new FilaPedido(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4),
                rs.getBoolean(5)), corteId, corteId, corteId);
        List<FilaPago> pagos = jdbc.query("""
            select id_pedido, tipo_pago, metodo_pago, monto, fecha_hora
            from barrocontrol.pago_pedido where id_corte_caja = ?
            order by fecha_hora, id
            """, (rs, i) -> new FilaPago(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4),
                rs.getTimestamp(5).toInstant()), corteId);
        List<FilaProducto> productos = jdbc.query("""
            select dp.id_pedido, p.nombre, dp.cantidad, dp.precio_unitario, dp.subtotal
            from barrocontrol.detalle_pedido dp
            join barrocontrol.producto p on p.id = dp.id_producto
            where dp.id_pedido in (select id_pedido from barrocontrol.pago_pedido where id_corte_caja = ?)
            order by p.nombre
            """, (rs, i) -> new FilaProducto(rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getBigDecimal(4),
                rs.getBigDecimal(5)), corteId);
        return armarPedidos(pedidos, pagos, productos);
    }

    public static List<PedidoCorteDto> armarPedidos(
        List<FilaPedido> pedidos, List<FilaPago> pagos, List<FilaProducto> productos
    ) {
        // Se agrupa una vez por pedido (conserva el orden de llegada) en lugar de filtrar las listas por cada pedido.
        Map<Long, List<PedidoCorteDto.Pago>> pagosPorPedido = pagos.stream().collect(Collectors.groupingBy(
            FilaPago::pedidoId, Collectors.mapping(
                x -> new PedidoCorteDto.Pago(x.tipoPago(), x.metodoPago(), dinero(x.monto()), x.fechaHora()),
                Collectors.toList())));
        Map<Long, List<PedidoCorteDto.Producto>> productosPorPedido = productos.stream().collect(Collectors.groupingBy(
            FilaProducto::pedidoId, Collectors.mapping(
                x -> new PedidoCorteDto.Producto(x.nombre(), x.cantidad(), dinero(x.precioUnitario()),
                    dinero(x.subtotal())),
                Collectors.toList())));
        return pedidos.stream().map(p -> {
            BigDecimal total = dinero(p.total());
            BigDecimal pagado = dinero(p.pagado());
            // Cancelado no debe nada: se devolvió lo pagado.
            BigDecimal saldo = p.cancelado() ? BigDecimal.ZERO.setScale(2) : total.subtract(pagado).max(BigDecimal.ZERO);
            return new PedidoCorteDto(p.id(), p.folio(), total, pagado, saldo, p.cancelado(),
                List.copyOf(pagosPorPedido.getOrDefault(p.id(), List.of())),
                List.copyOf(productosPorPedido.getOrDefault(p.id(), List.of())));
        }).toList();
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return valor != null ? valor.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }

    private static class AcumuladorProducto {
        private final Long idProducto;
        private final String nombre;
        private final BigDecimal precioUnitario;
        private final String metodoPago;
        private int cantidad;
        private BigDecimal subtotal;

        public AcumuladorProducto(Long idProducto, String nombre, BigDecimal precioUnitario, String metodoPago) {
            this.idProducto = idProducto;
            this.metodoPago = metodoPago;
            this.nombre = nombre;
            this.precioUnitario = precioUnitario != null ? precioUnitario.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
            this.cantidad = 0;
            this.subtotal = BigDecimal.ZERO.setScale(2);
        }

        /** sub ya viene normalizado (nunca nulo, dos decimales). */
        public void acumular(int cant, BigDecimal sub) {
            this.cantidad += cant;
            this.subtotal = this.subtotal.add(sub);
        }

        public ProductoVendidoDto toDto() {
            return new ProductoVendidoDto(idProducto, nombre, cantidad, precioUnitario, subtotal, metodoPago);
        }
    }
}
