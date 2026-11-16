package com.iaml.puntoventaapi.reportes.infrastructure.excel;

import com.iaml.puntoventaapi.reportes.application.port.ReporteExcelPort;
import com.iaml.puntoventaapi.reportes.domain.Periodo;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Corte;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Cuadre;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Dia;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Movimiento;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Producto;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Totales;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.VentasEmpleado;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ExcelExporter implements ReporteExcelPort {
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern(
        "EEEE d 'de' MMMM 'de' uuuu", Locale.forLanguageTag("es-MX"));
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String MONEDA = "\"$\"#,##0.00;-\"$\"#,##0.00";
    private static final int ULTIMA_COLUMNA = 5;

    @Override
    public byte[] generar(ReporteMovimientos reporte) {
        try (var libro = new SXSSFWorkbook(100); var salida = new ByteArrayOutputStream()) {
            libro.setCompressTempFiles(true);
            escribir(new HojaReporte(libro, ZoneId.of(reporte.zonaHoraria())), reporte);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo generar el archivo Excel del reporte.", ex);
        }
    }

    private static void escribir(HojaReporte hoja, ReporteMovimientos reporte) {
        hoja.titulo(reporte.sucursal().negocio() + " · " + reporte.sucursal().nombre(), ULTIMA_COLUMNA);
        hoja.titulo(rango(reporte.desde(), reporte.hasta()), ULTIMA_COLUMNA);
        hoja.espacio();
        for (var semana : reporte.semanas()) {
            if (reporte.periodo() == Periodo.MENSUAL) {
                hoja.seccion("SEMANA DEL " + rango(semana.desde(), semana.hasta()), ULTIMA_COLUMNA);
            }
            semana.dias().forEach(dia -> escribirDia(hoja, dia));
            if (reporte.periodo() == Periodo.MENSUAL) {
                hoja.resumen("TOTAL DE LA SEMANA", semana.resumen().general());
                hoja.espacio();
            }
        }
        hoja.resumen(reporte.periodo() == Periodo.SEMANAL ? "TOTAL DE LA SEMANA" : "TOTAL DEL MES",
            reporte.resumen().general());
    }

    private static void escribirDia(HojaReporte hoja, Dia dia) {
        hoja.titulo(FECHA_LARGA.format(dia.fecha()).toUpperCase(Locale.ROOT), ULTIMA_COLUMNA);
        hoja.espacio();
        hoja.cortes(dia.cortes());
        hoja.espacio();
        hoja.textoConEtiqueta("Registraron ventas", dia.ventasPorEmpleado().stream()
            .map(venta -> venta.empleado().nombre()).collect(Collectors.joining(", ")));
        hoja.espacio();
        hoja.movimientos("EFECTIVO", "Neto en efectivo", dia.efectivo(), dia.resumen().efectivo());
        hoja.espacio();
        hoja.movimientos("TARJETA", "Neto en tarjeta", dia.tarjeta(), dia.resumen().tarjeta());
        hoja.espacio();
        hoja.resumen("RESUMEN DEL DÍA", dia.resumen().general());
        hoja.espacio();
        hoja.cuadres(dia);
        hoja.espacio();
        hoja.ventasPorEmpleado(dia.ventasPorEmpleado());
        hoja.espacio();
    }

    private static String rango(LocalDate desde, LocalDate hasta) {
        return desde.format(DateTimeFormatter.ISO_LOCAL_DATE) + " al " + hasta.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static String descripcion(Movimiento movimiento) {
        String productos = movimiento.productos().stream()
            .map(ExcelExporter::describirProducto)
            .collect(Collectors.joining(", "));
        String referencia = movimiento.tipo() == ReporteMovimientos.TipoMovimiento.DEVOLUCION
            ? movimiento.referencia() + " · Devuelto por cancelación" : movimiento.referencia();
        return productos.isEmpty() ? referencia : referencia + " · " + productos;
    }

    private static String describirProducto(Producto producto) {
        return producto.cantidad() + " × " + producto.nombre();
    }

    private static final class HojaReporte {
        private final Sheet hoja;
        private final ZoneId zona;
        private final Estilos estilos;
        private int siguienteFila;

        private HojaReporte(Workbook libro, ZoneId zona) {
            this.hoja = libro.createSheet("Movimientos");
            this.zona = zona;
            this.estilos = new Estilos(libro);
            int[] anchos = {24, 21, 22, 47, 25, 18};
            for (int i = 0; i < anchos.length; i++) {
                hoja.setColumnWidth(i, anchos[i] * 256);
            }
        }

        private void titulo(String texto, int ultimaColumna) {
            combinar(fila(), 0, ultimaColumna, texto, estilos.titulo);
        }

        private void seccion(String texto, int ultimaColumna) {
            combinar(fila(), 0, ultimaColumna, texto, estilos.seccion);
        }

        private void espacio() {
            siguienteFila++;
        }

        private void cortes(List<Corte> cortes) {
            Row cabecera = fila();
            texto(cabecera, 0, "Corte", estilos.etiqueta);
            if (cortes.isEmpty()) {
                texto(cabecera, 1, "Sin cortes cerrados", estilos.texto);
                return;
            }
            Row abierto = etiqueta("Abrió la caja");
            Row apertura = etiqueta("Apertura");
            Row cierre = etiqueta("Cierre");
            Row fondo = etiqueta("Fondo inicial");
            for (int i = 0; i < cortes.size(); i++) {
                int columna = i + 1;
                Corte corte = cortes.get(i);
                prepararColumnaDeCorte(columna);
                texto(cabecera, columna, "#" + corte.id(), estilos.etiqueta);
                texto(abierto, columna, corte.abiertoPor(), estilos.texto);
                horario(apertura, columna, corte.apertura());
                horario(cierre, columna, corte.cierre());
                moneda(fondo, columna, corte.fondoInicial());
            }
        }

        private void textoConEtiqueta(String nombre, String valor) {
            combinar(etiqueta(nombre), 1, ULTIMA_COLUMNA, valor.isEmpty() ? "—" : valor, estilos.texto);
        }

        private void movimientos(String seccion, String etiquetaNeto, List<Movimiento> movimientos, Totales totales) {
            seccion(seccion, ULTIMA_COLUMNA);
            encabezados("Hora", "Corte", "Movimiento", "Referencia y detalle", "Registró", "Importe");
            if (movimientos.isEmpty()) {
                combinar(fila(), 0, ULTIMA_COLUMNA, "Sin movimientos", estilos.texto);
            }
            for (Movimiento movimiento : movimientos) {
                Row fila = fila();
                hora(fila, 0, movimiento.fechaHora());
                texto(fila, 1, "#" + movimiento.corteId(), estilos.texto);
                String tipo = switch (movimiento.tipo()) {
                    case VENTA -> "Venta";
                    case ANTICIPO -> "Anticipo";
                    case LIQUIDACION -> "Liquidación";
                    case DEVOLUCION -> "Devolución";
                };
                texto(fila, 2, tipo, estilos.texto);
                String detalle = descripcion(movimiento);
                texto(fila, 3, detalle, estilos.texto);
                fila.setHeightInPoints(Math.min(390, 15f * Math.max(1, (detalle.length() + 41) / 42)));
                texto(fila, 4, movimiento.vendedor() == null ? "—" : movimiento.vendedor().nombre(), estilos.texto);
                moneda(fila, 5, movimiento.importe());
            }
            espacio();
            moneda(etiqueta("Cobrado"), 1, totales.cobrado());
            moneda(etiqueta("Devuelto"), 1, totales.devuelto());
            moneda(etiqueta(etiquetaNeto), 1, totales.neto());
        }

        private void resumen(String nombre, Totales total) {
            seccion(nombre, 3);
            Row cabecera = fila();
            combinar(cabecera, 0, 1, "Dinero generado", estilos.etiqueta);
            combinar(cabecera, 2, 3, "Importe", estilos.etiqueta);
            total("Total cobrado entre efectivo y tarjeta", total.cobrado());
            total("Total devuelto", total.devuelto().negate());
            total("Ingresos netos generados", total.neto());
        }

        private void total(String nombre, BigDecimal monto) {
            Row fila = fila();
            combinar(fila, 0, 1, nombre, estilos.etiqueta);
            combinar(fila, 2, 3, null, estilos.moneda);
            fila.getCell(2).setCellValue(monto.doubleValue());
        }

        private void cuadres(Dia dia) {
            Row cabecera = fila();
            combinar(cabecera, 0, 1, "Cuadre de efectivo", estilos.etiqueta);
            if (dia.cuadres().isEmpty()) {
                combinar(cabecera, 2, ULTIMA_COLUMNA, "Sin cierres en este día", estilos.texto);
                return;
            }
            var cortes = dia.cortes().stream().collect(Collectors.toMap(Corte::id, Function.identity()));
            for (int i = 0; i < dia.cuadres().size(); i++) {
                Corte corte = cortes.get(dia.cuadres().get(i).corteId());
                int columna = i + 2;
                prepararColumnaDeCorte(columna);
                texto(cabecera, columna, "Corte #%d %s–%s".formatted(corte.id(),
                    HORA.format(horaLocal(corte.apertura())), HORA.format(horaLocal(corte.cierre()))), estilos.etiqueta);
            }
            filaCuadre("Fondo inicial", dia.cuadres(), Cuadre::fondoInicial);
            filaCuadre("Movimientos netos en efectivo", dia.cuadres(), Cuadre::netoEnEfectivo);
            filaCuadre("Efectivo esperado al cierre", dia.cuadres(), Cuadre::efectivoEsperado);
            filaCuadre("Efectivo contado al cierre", dia.cuadres(), Cuadre::efectivoContado);
            filaCuadre("Diferencia de efectivo", dia.cuadres(), Cuadre::diferenciaEfectivo);
        }

        private void filaCuadre(String etiqueta, List<Cuadre> cuadres, Function<Cuadre, BigDecimal> valor) {
            Row fila = fila();
            combinar(fila, 0, 1, etiqueta, estilos.etiqueta);
            for (int i = 0; i < cuadres.size(); i++) {
                moneda(fila, i + 2, valor.apply(cuadres.get(i)));
            }
        }

        private void ventasPorEmpleado(List<VentasEmpleado> vendedores) {
            seccion("VENTAS POR EMPLEADO", 4);
            encabezados("Empleado", "Ventas registradas", "Efectivo", "Tarjeta", "Total vendido");
            for (VentasEmpleado venta : vendedores) {
                filaEmpleado(venta.empleado().nombre(), venta.ventasRegistradas(), venta.efectivo(),
                    venta.tarjeta(), venta.totalVendido());
            }
            int cantidad = vendedores.stream().mapToInt(VentasEmpleado::ventasRegistradas).sum();
            BigDecimal efectivo = vendedores.stream().map(VentasEmpleado::efectivo).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal tarjeta = vendedores.stream().map(VentasEmpleado::tarjeta).reduce(BigDecimal.ZERO, BigDecimal::add);
            filaEmpleado("Total de ventas directas", cantidad, efectivo, tarjeta, efectivo.add(tarjeta));
        }

        private void filaEmpleado(String nombre, int cantidad, BigDecimal efectivo, BigDecimal tarjeta, BigDecimal total) {
            Row fila = fila();
            texto(fila, 0, nombre, estilos.texto);
            celda(fila, 1, estilos.texto).setCellValue(cantidad);
            moneda(fila, 2, efectivo);
            moneda(fila, 3, tarjeta);
            moneda(fila, 4, total);
        }

        private Row etiqueta(String nombre) {
            Row fila = fila();
            texto(fila, 0, nombre, estilos.etiqueta);
            return fila;
        }

        private void encabezados(String... nombres) {
            Row fila = fila();
            for (int i = 0; i < nombres.length; i++) {
                texto(fila, i, nombres[i], estilos.etiqueta);
            }
        }

        private void prepararColumnaDeCorte(int columna) {
            if (columna > ULTIMA_COLUMNA) {
                hoja.setColumnWidth(columna, 25 * 256);
            }
        }

        private Row fila() {
            return hoja.createRow(siguienteFila++);
        }

        private void moneda(Row fila, int columna, BigDecimal valor) {
            celda(fila, columna, estilos.moneda).setCellValue(valor.doubleValue());
        }

        private void horario(Row fila, int columna, Instant instante) {
            celda(fila, columna, estilos.fechaHora).setCellValue(instante.atZone(zona).toLocalDateTime());
        }

        private void hora(Row fila, int columna, Instant instante) {
            celda(fila, columna, estilos.hora).setCellValue(horaLocal(instante).toSecondOfDay() / 86_400.0);
        }

        private LocalTime horaLocal(Instant instante) {
            return instante.atZone(zona).toLocalTime();
        }

        private static void texto(Row fila, int columna, String valor, CellStyle estilo) {
            Cell celda = celda(fila, columna, estilo);
            if (valor != null) {
                celda.setCellValue(valor.length() <= 32_767 ? valor : valor.substring(0, 32_766) + "…");
            }
        }

        private void combinar(Row fila, int inicio, int fin, String texto, CellStyle estilo) {
            texto(fila, inicio, texto, estilo);
            for (int columna = inicio + 1; columna <= fin; columna++) {
                celda(fila, columna, estilo);
            }
            if (inicio < fin) {
                hoja.addMergedRegion(new CellRangeAddress(fila.getRowNum(), fila.getRowNum(), inicio, fin));
            }
        }

        private static Cell celda(Row fila, int columna, CellStyle estilo) {
            Cell celda = fila.createCell(columna);
            celda.setCellStyle(estilo);
            return celda;
        }
    }

    private static final class Estilos {
        private final CellStyle texto;
        private final CellStyle etiqueta;
        private final CellStyle seccion;
        private final CellStyle titulo;
        private final CellStyle moneda;
        private final CellStyle hora;
        private final CellStyle fechaHora;

        private Estilos(Workbook libro) {
            texto = libro.createCellStyle();
            texto.setBorderTop(BorderStyle.THIN);
            texto.setBorderBottom(BorderStyle.THIN);
            texto.setBorderLeft(BorderStyle.THIN);
            texto.setBorderRight(BorderStyle.THIN);
            texto.setVerticalAlignment(VerticalAlignment.CENTER);
            texto.setWrapText(true);

            etiqueta = libro.createCellStyle();
            etiqueta.cloneStyleFrom(texto);
            etiqueta.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            etiqueta.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            seccion = libro.createCellStyle();
            seccion.cloneStyleFrom(etiqueta);
            seccion.setAlignment(HorizontalAlignment.CENTER);
            titulo = libro.createCellStyle();
            titulo.setAlignment(HorizontalAlignment.CENTER);

            var formatos = libro.createDataFormat();
            moneda = libro.createCellStyle();
            moneda.cloneStyleFrom(texto);
            moneda.setDataFormat(formatos.getFormat(MONEDA));
            hora = libro.createCellStyle();
            hora.cloneStyleFrom(texto);
            hora.setDataFormat(formatos.getFormat("hh:mm"));
            fechaHora = libro.createCellStyle();
            fechaHora.cloneStyleFrom(texto);
            fechaHora.setDataFormat(formatos.getFormat("dd/mm/yyyy hh:mm"));
        }
    }
}
