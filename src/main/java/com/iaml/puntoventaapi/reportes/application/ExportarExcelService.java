package com.iaml.puntoventaapi.reportes.application;

import com.iaml.puntoventaapi.reportes.application.port.ReporteExcelPort;
import com.iaml.puntoventaapi.reportes.domain.Periodo;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class ExportarExcelService {
    private final ReporteService reportes;
    private final ReporteExcelPort excel;

    public ExportarExcelService(ReporteService reportes, ReporteExcelPort excel) {
        this.reportes = reportes;
        this.excel = excel;
    }

    public Archivo exportar(long sucursalId, long propietarioId, Periodo periodo, LocalDate fecha) {
        var reporte = reportes.consultar(sucursalId, propietarioId, periodo, fecha);
        String nombre = "reporte-sucursal-%d-%s-%s.xlsx".formatted(sucursalId, reporte.desde(), reporte.hasta());
        return new Archivo(nombre, excel.generar(reporte));
    }

    public record Archivo(String nombre, byte[] contenido) {}
}
