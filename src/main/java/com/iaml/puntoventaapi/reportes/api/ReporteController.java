package com.iaml.puntoventaapi.reportes.api;

import com.iaml.puntoventaapi.reportes.application.ExportarExcelService;
import com.iaml.puntoventaapi.reportes.application.ReporteService;
import com.iaml.puntoventaapi.reportes.domain.Periodo;
import com.iaml.puntoventaapi.reportes.domain.ReporteMovimientos;
import com.iaml.puntoventaapi.shared.security.UsuarioActual;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sucursales/{sucursalId}/reportes/movimientos")
@PreAuthorize("hasRole('PROPIETARIO')")
public class ReporteController {
    private static final String TIPO_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReporteService reportes;
    private final ExportarExcelService exportador;
    private final UsuarioActual usuarioActual;

    public ReporteController(ReporteService reportes, ExportarExcelService exportador, UsuarioActual usuarioActual) {
        this.reportes = reportes;
        this.exportador = exportador;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReporteMovimientos> consultar(@PathVariable @Positive long sucursalId,
            @RequestParam Periodo periodo, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "false") boolean incluirAbiertos) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(reportes.consultar(sucursalId, usuarioActual.id(), periodo, fecha, incluirAbiertos));
    }

    @GetMapping(value = "/excel", produces = TIPO_EXCEL)
    public ResponseEntity<byte[]> exportar(@PathVariable @Positive long sucursalId,
            @RequestParam Periodo periodo, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        var archivo = exportador.exportar(sucursalId, usuarioActual.id(), periodo, fecha);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType(TIPO_EXCEL))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archivo.nombre())
                .build().toString())
            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
            .body(archivo.contenido());
    }
}
