package com.patitas.contacto_service.controller;

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

import com.patitas.contacto_service.model.EstadoMensaje;
import com.patitas.contacto_service.model.MensajeContacto;
import com.patitas.contacto_service.service.MensajeService;
import com.patitas.contacto_service.service.NegocioException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Mensajes de contacto.
 * Permisos:
 * - Público: enviar un mensaje
 * - Usuario con sesión: ver sus propios mensajes y las respuestas
 * - ADMIN / RECEPCIONISTA: bandeja completa, cambiar estado y responder
 * - ADMIN: eliminar y ver estadísticas
 */
@RestController
@RequestMapping("/api/v1/mensajes")
@RequiredArgsConstructor
@Tag(name = "Mensajes de contacto", description = "Formulario de contacto y bandeja del personal")
public class MensajeController {

    private final MensajeService mensajeService;

    @Operation(summary = "Enviar mensaje de contacto",
        description = "Público (formulario de /contacto). Si se envía con token, el mensaje queda asociado a la cuenta "
            + "y el usuario podrá ver la respuesta en 'Mis mensajes'.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(value = """
                { "nombre": "Camila Rojas", "correo": "camila.rojas@gmail.com", "telefono": "+56 9 1234 5678",
                  "asunto": "Información sobre citas", "mensaje": "Hola, ¿atienden conejos los sábados?" }"""))))
    @ApiResponse(responseCode = "201", description = "Mensaje recibido",
        content = @Content(schema = @Schema(implementation = MensajeContacto.class)))
    @ApiResponse(responseCode = "400", description = "Algún campo no es válido (indica cuál en 'campo')", content = @Content)
    @SecurityRequirements // Público
    @PostMapping
    public ResponseEntity<MensajeContacto> enviar(@RequestBody MensajeContacto datos, Authentication auth) {
        String correoUsuario = auth != null ? auth.getName() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(mensajeService.enviarMensaje(datos, correoUsuario));
    }

    @Operation(summary = "Bandeja de mensajes", description = "ADMIN y RECEPCIONISTA. Se puede filtrar por estado.")
    @ApiResponse(responseCode = "200", description = "Mensajes, del más reciente al más antiguo",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = MensajeContacto.class))))
    @ApiResponse(responseCode = "403", description = "No es personal de la clínica", content = @Content)
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    @GetMapping
    public ResponseEntity<List<MensajeContacto>> listar(
            @Parameter(description = "PENDIENTE, LEIDO, RESPONDIDO o ARCHIVADO (opcional)")
            @RequestParam(required = false) EstadoMensaje estado) {
        return ResponseEntity.ok(mensajeService.listarMensajes(estado));
    }

    @Operation(summary = "Mis mensajes", description = "Cualquier usuario con sesión: los mensajes que envió y sus respuestas.")
    @ApiResponse(responseCode = "200", description = "Mensajes del usuario",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = MensajeContacto.class))))
    @ApiResponse(responseCode = "401", description = "Sin sesión", content = @Content)
    @GetMapping("/mis-mensajes")
    public ResponseEntity<List<MensajeContacto>> misMensajes(Authentication auth) {
        return ResponseEntity.ok(mensajeService.listarMisMensajes(auth.getName()));
    }

    @Operation(summary = "Estadísticas de mensajes", description = "Solo ADMIN. Total y cantidad por estado (para el dashboard).")
    @ApiResponse(responseCode = "200", description = "Ejemplo: { total: 5, pendiente: 2, leido: 1, respondido: 2, archivado: 0 }")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Long>> estadisticas() {
        return ResponseEntity.ok(mensajeService.estadisticas());
    }

    @Operation(summary = "Ver un mensaje", description = "El personal ve cualquiera; un usuario solo los que él envió.")
    @ApiResponse(responseCode = "200", description = "Mensaje encontrado",
        content = @Content(schema = @Schema(implementation = MensajeContacto.class)))
    @ApiResponse(responseCode = "403", description = "No es suyo", content = @Content)
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content)
    @GetMapping("/{id}")
    public ResponseEntity<MensajeContacto> obtener(@PathVariable Long id, Authentication auth) {
        MensajeContacto mensaje = mensajeService.obtenerPorId(id);
        boolean esPersonal = tieneRol(auth, "ADMIN") || tieneRol(auth, "RECEPCIONISTA");
        boolean esSuyo = auth.getName().equalsIgnoreCase(mensaje.getCorreoUsuario());
        if (!esPersonal && !esSuyo) {
            throw new NegocioException(null, "No tienes permisos para ver este mensaje.", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(mensaje);
    }

    @Operation(summary = "Cambiar estado", description = "ADMIN y RECEPCIONISTA. Ej: marcar como LEIDO o ARCHIVADO.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(value = "{ \"estado\": \"LEIDO\" }"))))
    @ApiResponse(responseCode = "200", description = "Estado actualizado",
        content = @Content(schema = @Schema(implementation = MensajeContacto.class)))
    @ApiResponse(responseCode = "400", description = "Estado inválido", content = @Content)
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    @PutMapping("/{id}/estado")
    public ResponseEntity<MensajeContacto> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> datos) {
        EstadoMensaje estado;
        try {
            estado = EstadoMensaje.valueOf(String.valueOf(datos.get("estado")).trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NegocioException("estado", "Estado inválido. Usa PENDIENTE, LEIDO o ARCHIVADO.");
        }
        return ResponseEntity.ok(mensajeService.cambiarEstado(id, estado));
    }

    @Operation(summary = "Responder mensaje", description = "ADMIN y RECEPCIONISTA. El mensaje queda RESPONDIDO.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(value = "{ \"respuesta\": \"¡Hola! Sí, atendemos conejos los sábados de 9 a 14 h.\" }"))))
    @ApiResponse(responseCode = "200", description = "Respuesta guardada",
        content = @Content(schema = @Schema(implementation = MensajeContacto.class)))
    @ApiResponse(responseCode = "400", description = "Respuesta vacía o muy larga", content = @Content)
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    @PutMapping("/{id}/responder")
    public ResponseEntity<MensajeContacto> responder(@PathVariable Long id, @RequestBody Map<String, String> datos,
            Authentication auth) {
        return ResponseEntity.ok(mensajeService.responder(id, datos.get("respuesta"), auth.getName()));
    }

    @Operation(summary = "Eliminar mensaje", description = "Solo ADMIN.")
    @ApiResponse(responseCode = "204", description = "Eliminado", content = @Content)
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mensajeService.eliminarMensaje(id);
        return ResponseEntity.noContent().build();
    }

    private boolean tieneRol(Authentication auth, String rol) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }
}
