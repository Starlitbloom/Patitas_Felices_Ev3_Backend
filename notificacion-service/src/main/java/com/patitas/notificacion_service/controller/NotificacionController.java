package com.patitas.notificacion_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.patitas.notificacion_service.model.Notificacion;
import com.patitas.notificacion_service.service.NotificacionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Avisos para los usuarios (campanita del header)")
public class NotificacionController {

    private final NotificacionService notificacionService;

    @Operation(summary = "Crear notificación",
        description = "El personal (ADMIN / RECEPCIONISTA) puede enviar avisos a cualquier usuario; un CLIENTE solo a sí mismo. "
            + "Los otros microservicios llaman a este endpoint por WebClient reenviando el token de quien hizo la acción.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(value = """
                { "correoDestinatario": "camila.rojas@gmail.com", "titulo": "Cita confirmada",
                  "mensaje": "Tu cita de vacunación para Rocky quedó confirmada para el lunes a las 10:00.",
                  "tipo": "CITA", "enlace": "/mis-citas" }"""))))
    @ApiResponse(responseCode = "201", description = "Notificación creada",
        content = @Content(schema = @Schema(implementation = Notificacion.class)))
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    @ApiResponse(responseCode = "403", description = "Un cliente intentó enviar un aviso a otra persona", content = @Content)
    @PostMapping
    public ResponseEntity<Notificacion> crear(@RequestBody Notificacion datos, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(notificacionService.crear(datos, auth.getName(), esPersonal(auth)));
    }

    @Operation(summary = "Mis notificaciones", description = "Las del usuario del token, de la más reciente a la más antigua.")
    @ApiResponse(responseCode = "200", description = "Lista de notificaciones",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = Notificacion.class))))
    @GetMapping("/mis-notificaciones")
    public ResponseEntity<List<Notificacion>> misNotificaciones(Authentication auth,
            @Parameter(description = "true = solo las no leídas")
            @RequestParam(defaultValue = "false") boolean soloNoLeidas) {
        return ResponseEntity.ok(notificacionService.misNotificaciones(auth.getName(), soloNoLeidas));
    }

    @Operation(summary = "Cantidad de no leídas", description = "El número que va sobre la campanita: { \"cantidad\": 3 }")
    @ApiResponse(responseCode = "200", description = "Cantidad")
    @GetMapping("/mis-notificaciones/no-leidas")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(Authentication auth) {
        return ResponseEntity.ok(Map.of("cantidad", notificacionService.contarNoLeidas(auth.getName())));
    }

    @Operation(summary = "Marcar como leída", description = "Solo el destinatario.")
    @ApiResponse(responseCode = "200", description = "Notificación marcada",
        content = @Content(schema = @Schema(implementation = Notificacion.class)))
    @ApiResponse(responseCode = "403", description = "No es tuya", content = @Content)
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content)
    @PutMapping("/{id}/leer")
    public ResponseEntity<Notificacion> marcarLeida(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(notificacionService.marcarLeida(id, auth.getName()));
    }

    @Operation(summary = "Marcar todas como leídas", description = "Devuelve cuántas se marcaron: { \"marcadas\": 4 }")
    @ApiResponse(responseCode = "200", description = "Listo")
    @PutMapping("/mis-notificaciones/leer-todas")
    public ResponseEntity<Map<String, Integer>> marcarTodas(Authentication auth) {
        return ResponseEntity.ok(Map.of("marcadas", notificacionService.marcarTodasLeidas(auth.getName())));
    }

    @Operation(summary = "Eliminar notificación", description = "El destinatario o un ADMIN.")
    @ApiResponse(responseCode = "204", description = "Eliminada", content = @Content)
    @ApiResponse(responseCode = "403", description = "No es tuya", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        notificacionService.eliminar(id, auth.getName(), tieneRol(auth, "ADMIN"));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Todas las notificaciones", description = "Solo ADMIN.")
    @ApiResponse(responseCode = "200", description = "Lista completa",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = Notificacion.class))))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Notificacion>> listarTodas() {
        return ResponseEntity.ok(notificacionService.listarTodas());
    }

    @Operation(summary = "Estadísticas", description = "Solo ADMIN. Total, no leídas y cantidad por tipo.")
    @ApiResponse(responseCode = "200", description = "KPIs")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Long>> estadisticas() {
        return ResponseEntity.ok(notificacionService.estadisticas());
    }

    /* ---------------- Auxiliares ---------------- */

    private boolean tieneRol(Authentication auth, String rol) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }

    private boolean esPersonal(Authentication auth) {
        return tieneRol(auth, "ADMIN") || tieneRol(auth, "RECEPCIONISTA");
    }
}
