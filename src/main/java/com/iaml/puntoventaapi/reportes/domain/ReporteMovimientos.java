package com.iaml.puntoventaapi.reportes.domain;

import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.groupingBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public record ReporteMovimientos(Sucursal sucursal, Periodo periodo, LocalDate desde, LocalDate hasta,
        String zonaHoraria, List<Semana> semanas, Resumen resumen, List<VentasEmpleado> ventasPorEmpleado) {

    public static final ZoneId ZONA_HORARIA = ZoneId.of("America/Mexico_City");

    public record Sucursal(long id, String nombre, String negocio) {}

    public record Vendedor(long id, String nombre) {}

    public record Producto(long id, String nombre, int cantidad, BigDecimal precioUnitario,
            BigDecimal porcentajeDescuento, BigDecimal subtotal) {}

    public enum MedioPago {
        EFECTIVO, TARJETA
    }

    public enum TipoMovimiento {
        VENTA, ANTICIPO, LIQUIDACION, DEVOLUCION
    }

    public record Movimiento(long id, long corteId, Instant fechaHora, TipoMovimiento tipo, MedioPago medioPago,
            String referencia, Vendedor vendedor, BigDecimal importe, List<Producto> productos,
            Vendedor creadorPedido) {
        private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

        public String getHoraLocal() {
            return fechaHora.atZone(ZONA_HORARIA).format(HORA);
        }
    }

    public record Corte(long id, String abiertoPor, Instant apertura, Instant cierre, BigDecimal fondoInicial) {}

    public record Cuadre(long corteId, BigDecimal fondoInicial, BigDecimal netoEnEfectivo,
            BigDecimal efectivoEsperado, BigDecimal efectivoContado, BigDecimal diferenciaEfectivo) {}

    public record Dia(LocalDate fecha, List<Corte> cortes, List<Cuadre> cuadres, List<Movimiento> efectivo,
            List<Movimiento> tarjeta, Resumen resumen, List<VentasEmpleado> ventasPorEmpleado) {}

    public record Semana(LocalDate desde, LocalDate hasta, List<Dia> dias, Resumen resumen) {}

    public record Totales(BigDecimal cobrado, BigDecimal devuelto, BigDecimal neto) {
        public static Totales de(List<Movimiento> movimientos) {
            BigDecimal cobrado = ZERO.setScale(2);
            BigDecimal devuelto = ZERO.setScale(2);
            for (Movimiento movimiento : movimientos) {
                if (movimiento.tipo() == TipoMovimiento.DEVOLUCION) {
                    devuelto = devuelto.subtract(movimiento.importe());
                } else {
                    cobrado = cobrado.add(movimiento.importe());
                }
            }
            return new Totales(cobrado, devuelto, cobrado.subtract(devuelto));
        }

        public Totales sumar(Totales otros) {
            return new Totales(cobrado.add(otros.cobrado), devuelto.add(otros.devuelto), neto.add(otros.neto));
        }
    }

    public record Resumen(Totales efectivo, Totales tarjeta, Totales general) {
        public static Resumen de(List<Movimiento> efectivo, List<Movimiento> tarjeta) {
            Totales totalesEfectivo = Totales.de(efectivo);
            Totales totalesTarjeta = Totales.de(tarjeta);
            return new Resumen(totalesEfectivo, totalesTarjeta, totalesEfectivo.sumar(totalesTarjeta));
        }
    }

    public record VentasEmpleado(Vendedor empleado, int ventasRegistradas, BigDecimal efectivo, BigDecimal tarjeta,
            BigDecimal totalVendido) {
        public static List<VentasEmpleado> de(List<Movimiento> movimientos) {
            return movimientos.stream()
                .filter(movimiento -> movimiento.tipo() == TipoMovimiento.VENTA)
                .collect(groupingBy(Movimiento::vendedor))
                .entrySet().stream()
                .map(grupo -> calcular(grupo.getKey(), grupo.getValue()))
                .sorted(Comparator.comparing((VentasEmpleado fila) -> fila.empleado.nombre())
                    .thenComparingLong(fila -> fila.empleado.id()))
                .toList();
        }

        private static VentasEmpleado calcular(Vendedor vendedor, List<Movimiento> ventas) {
            BigDecimal efectivo = ZERO.setScale(2);
            BigDecimal tarjeta = ZERO.setScale(2);
            for (Movimiento venta : ventas) {
                if (venta.medioPago() == MedioPago.EFECTIVO) {
                    efectivo = efectivo.add(venta.importe());
                } else {
                    tarjeta = tarjeta.add(venta.importe());
                }
            }
            return new VentasEmpleado(vendedor, ventas.size(), efectivo, tarjeta, efectivo.add(tarjeta));
        }
    }
}
