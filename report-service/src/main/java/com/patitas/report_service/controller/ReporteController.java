package com.patitas.report_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.patitas.report_service.model.ReporteGenerado;
import com.patitas.report_service.service.ReporteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Dashboard y reportes del panel admin.
 * El token del usuario se reenvía (header Authorization) a cada microservicio
 * consultado, así cada uno aplica sus propios permisos.
 */
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Dashboard, reporte completo, exportación e historial")
public class ReporteController {

    private final ReporteService reporteService;

    @Operation(summary = "Dashboard",
        description = "ADMIN y RECEPCIONISTA. Indicadores del inicio del panel: usuarios (solo ADMIN), mascotas, "
            + "citas pendientes y solicitudes pendientes. Los valores en null indican un servicio que no respondió.")
    @ApiResponse(responseCode = "200", description = "{ usuarios, clinica, serviciosNoDisponibles }")
    @ApiResponse(responseCode = "403", description = "Es cliente", content = @Content)
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(Authentication auth,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        boolean esAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(reporteService.dashboard(authHeader, esAdmin));
    }

    @Operation(summary = "Reporte completo",
        description = "Solo ADMIN. Indicadores y datos para los gráficos de la página Reportes: usuarios por rol, "
            + "región y comuna; mascotas por especie; citas por estado y servicio; mensajes y notificaciones. "
            + "Queda registrado en el historial.")
    @ApiResponse(responseCode = "200", description = "{ usuarios, mascotas, citas, contacto, notificaciones, serviciosNoDisponibles, generadoPor, generadoEn }")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/completo")
    public ResponseEntity<Map<String, Object>> completo(Authentication auth,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(reporteService.reporteCompleto(authHeader, auth.getName()));
    }

    @Operation(summary = "Exportar reporte completo",
        description = "Solo ADMIN. Filas para el Excel de 3 hojas (Usuarios, Mascotas, Citas). "
            + "El frontend genera el .xlsx con SheetJS. Queda registrado en el historial.")
    @ApiResponse(responseCode = "200", description = "{ nombreArchivo, hojas: { Usuarios: [...], Mascotas: [...], Citas: [...] }, serviciosNoDisponibles }")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/exportar")
    public ResponseEntity<Map<String, Object>> exportar(Authentication auth,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(reporteService.exportar(authHeader, auth.getName()));
    }

    @Operation(summary = "Historial de reportes", description = "Solo ADMIN. Quién generó cada reporte y cuándo.")
    @ApiResponse(responseCode = "200", description = "Historial",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReporteGenerado.class))))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/historial")
    public ResponseEntity<List<ReporteGenerado>> historial() {
        return ResponseEntity.ok(reporteService.historial());
    }

    @Operation(summary = "Ver un registro del historial", description = "Solo ADMIN.")
    @ApiResponse(responseCode = "200", description = "Registro",
        content = @Content(schema = @Schema(implementation = ReporteGenerado.class)))
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/historial/{id}")
    public ResponseEntity<ReporteGenerado> verHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(reporteService.obtenerDelHistorial(id));
    }

    @Operation(summary = "Eliminar un registro del historial", description = "Solo ADMIN.")
    @ApiResponse(responseCode = "204", description = "Eliminado", content = @Content)
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/historial/{id}")
    public ResponseEntity<Void> eliminarHistorial(@PathVariable Long id) {
        reporteService.eliminarDelHistorial(id);
        return ResponseEntity.noContent().build();
    }
}
