package com.iaml.puntoventaapi.reportes.application.port;

import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Corte;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Cuadre;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Movimiento;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Sucursal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReporteConsultaPort {
    Optional<Sucursal> buscarSucursal(long sucursalId);

    List<CorteConsultado> buscarCortes(long sucursalId, Instant desde, Instant hastaExclusivo);

    List<Movimiento> buscarMovimientos(long sucursalId, Instant desde, Instant hastaExclusivo,
                                      boolean incluirAbiertos);

    record CorteConsultado(Corte corte, Cuadre cuadre) {}
}
