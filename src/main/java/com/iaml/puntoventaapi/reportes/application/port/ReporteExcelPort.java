package com.iaml.puntoventaapi.reportes.application.port;

import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos;

public interface ReporteExcelPort {
    byte[] generar(ReporteMovimientos reporte);
}
