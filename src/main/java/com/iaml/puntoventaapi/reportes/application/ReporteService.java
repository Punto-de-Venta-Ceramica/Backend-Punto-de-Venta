package com.iaml.puntoventaapi.reportes.application;

import static java.time.DayOfWeek.MONDAY;
import static java.time.temporal.TemporalAdjusters.previousOrSame;
import static java.util.stream.Collectors.groupingBy;

import com.iaml.puntoventaapi.reportes.application.port.ReporteConsultaPort;
import com.iaml.puntoventaapi.reportes.application.port.ReporteConsultaPort.CorteConsultado;
import com.iaml.puntoventaapi.reportes.domain.Periodo;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Corte;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Dia;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.MedioPago;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Movimiento;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Resumen;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.Semana;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos.VentasEmpleado;
import com.iaml.puntoventaapi.sucursal.SucursalContrato;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteService {
    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Mexico_City");

    private final SucursalContrato sucursales;
    private final ReporteConsultaPort consulta;

    public ReporteService(SucursalContrato sucursales, ReporteConsultaPort consulta) {
        this.sucursales = sucursales;
        this.consulta = consulta;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReporteMovimientos consultar(long sucursalId, long propietarioId, Periodo periodo, LocalDate fecha) {
        if (sucursales.negocioAutorizado(sucursalId, propietarioId).isEmpty()) {
            throw new AccessDeniedException("La sucursal no está disponible para este usuario.");
        }
        var sucursal = consulta.buscarSucursal(sucursalId)
            .orElseThrow(() -> new AccessDeniedException("La sucursal no está disponible para este usuario."));
        var rango = periodo.rango(fecha);
        Instant desde = inicioDelDia(rango.inicio());
        Instant hasta = inicioDelDia(rango.finExclusivo());
        var cortes = consulta.buscarCortes(sucursalId, desde, hasta);
        var movimientos = consulta.buscarMovimientos(sucursalId, desde, hasta);
        var movimientosPorDia = movimientos.stream().collect(groupingBy(m -> fechaLocal(m.fechaHora())));

        var semanas = rango.inicio().datesUntil(rango.finExclusivo())
            .map(dia -> construirDia(dia, cortes, movimientosPorDia.getOrDefault(dia, List.of())))
            .collect(groupingBy(dia -> dia.fecha().with(previousOrSame(MONDAY)), LinkedHashMap::new,
                java.util.stream.Collectors.toList()))
            .values().stream()
            .map(ReporteService::construirSemana)
            .toList();
        return new ReporteMovimientos(sucursal, periodo, rango.inicio(), rango.ultimoDia(),
            ZONA_HORARIA.getId(), semanas, resumir(movimientos), VentasEmpleado.de(movimientos));
    }

    private Dia construirDia(LocalDate fecha, List<CorteConsultado> cortes, List<Movimiento> movimientos) {
        Instant inicio = inicioDelDia(fecha);
        Instant fin = inicioDelDia(fecha.plusDays(1));
        var cortesDelDia = cortes.stream()
            .map(CorteConsultado::corte)
            .filter(corte -> coincideConDia(corte, inicio, fin))
            .toList();
        var cuadres = cortes.stream()
            .filter(corte -> fecha.equals(fechaLocal(corte.corte().cierre())))
            .map(CorteConsultado::cuadre)
            .toList();
        var efectivo = porMedio(movimientos, MedioPago.EFECTIVO);
        var tarjeta = porMedio(movimientos, MedioPago.TARJETA);
        return new Dia(fecha, cortesDelDia, cuadres, efectivo, tarjeta, Resumen.de(efectivo, tarjeta),
            VentasEmpleado.de(movimientos));
    }

    private static Semana construirSemana(List<Dia> dias) {
        var efectivo = dias.stream().flatMap(dia -> dia.efectivo().stream()).toList();
        var tarjeta = dias.stream().flatMap(dia -> dia.tarjeta().stream()).toList();
        return new Semana(dias.getFirst().fecha(), dias.getLast().fecha(), dias, Resumen.de(efectivo, tarjeta));
    }

    private static boolean coincideConDia(Corte corte, Instant inicio, Instant fin) {
        return corte.apertura().isBefore(fin) && !corte.cierre().isBefore(inicio);
    }

    private static Resumen resumir(List<Movimiento> movimientos) {
        return Resumen.de(porMedio(movimientos, MedioPago.EFECTIVO), porMedio(movimientos, MedioPago.TARJETA));
    }

    private static List<Movimiento> porMedio(List<Movimiento> movimientos, MedioPago medio) {
        return movimientos.stream().filter(movimiento -> movimiento.medioPago() == medio).toList();
    }

    private static Instant inicioDelDia(LocalDate dia) {
        return dia.atStartOfDay(ZONA_HORARIA).toInstant();
    }

    private static LocalDate fechaLocal(Instant fecha) {
        return fecha.atZone(ZONA_HORARIA).toLocalDate();
    }
}
