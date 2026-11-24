
package com.patitas.historial_service.controller;

// ---- Imports (herramientas que usa el controller) ----
// Para devolver listas
import java.util.List;

// @Valid: activa las validaciones del DTO (@NotBlank, @Size...) antes de entrar al método
import jakarta.validation.Valid;
// Lombok: genera el constructor automáticamente
import lombok.RequiredArgsConstructor;

// @Operation: texto que aparece en Swagger junto a cada endpoint
import io.swagger.v3.oas.annotations.Operation;
// @Tag: nombre del grupo de endpoints en Swagger
import io.swagger.v3.oas.annotations.tags.Tag;

// Códigos HTTP con nombre (CREATED = 201, NO_CONTENT = 204)
import org.springframework.http.HttpStatus;
// @PreAuthorize: restringe un endpoint a ciertos roles
import org.springframework.security.access.prepost.PreAuthorize;
// Anotaciones que unen cada método con un verbo y una ruta HTTP
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Clases del proyecto que usa el controller
import com.patitas.historial_service.dto.AtencionRequest;
import com.patitas.historial_service.dto.AtencionResponse;
import com.patitas.historial_service.dto.VacunaRequest;
import com.patitas.historial_service.dto.VacunaResponse;
import com.patitas.historial_service.service.HistorialService;

/**
 * API REST del historial clínico.
 *   GET    /api/v1/historial/mascotas/{mascotaId}/atenciones   listar atenciones (cualquier usuario con sesión)
 *   POST   /api/v1/historial/mascotas/{mascotaId}/atenciones   registrar atención  (admin, recepcionista)
 *   GET    /api/v1/historial/atenciones/{id}                   detalle
 *   PUT    /api/v1/historial/atenciones/{id}                   actualizar          (admin, recepcionista)
 *   DELETE /api/v1/historial/atenciones/{id}                   eliminar            (admin)
 *   GET    /api/v1/historial/mascotas/{mascotaId}/vacunas      listar vacunas con su estado
 *   POST   /api/v1/historial/mascotas/{mascotaId}/vacunas      registrar vacuna    (admin, recepcionista)
 *   GET    /api/v1/historial/vacunas/{id}                      detalle de una vacuna
 *   PUT    /api/v1/historial/vacunas/{id}                      actualizar vacuna   (admin, recepcionista)
 *   DELETE /api/v1/historial/vacunas/{id}                      eliminar vacuna     (admin)
 */
// @RestController: esta clase recibe peticiones HTTP y responde en JSON
@RestController
// Todas las rutas de esta clase empiezan con /api/v1/historial
@RequestMapping("/api/v1/historial")
// Nombre y descripción del grupo en Swagger
@Tag(name = "Historial clinico", description = "Atenciones y vacunas por mascota")
// Lombok crea el constructor con los campos "final" (el service); Spring lo usa para entregarlo
@RequiredArgsConstructor
// El controller SOLO recibe la petición y llama al service; la lógica está en HistorialService
public class HistorialController {

    // El service con la lógica de negocio (Spring lo entrega por el constructor que genera Lombok)
    private final HistorialService servicio;

    // ============================ ATENCIONES ============================

    // READ (listar): GET /api/v1/historial/mascotas/1/atenciones
    @GetMapping("/mascotas/{mascotaId}/atenciones")
    // Texto en Swagger
    @Operation(summary = "Listar atenciones de una mascota (mas recientes primero)")
    // @PathVariable: toma el id de la mascota desde la URL
    public List<AtencionResponse> listarAtenciones(@PathVariable Long mascotaId) {
        // Pide la lista al service y convierte cada atención a su respuesta JSON
        return servicio.listarAtenciones(mascotaId).stream().map(AtencionResponse::de).toList();
    }

    // CREATE: POST /api/v1/historial/mascotas/1/atenciones
    @PostMapping("/mascotas/{mascotaId}/atenciones")
    // Solo ADMIN y RECEPCIONISTA; un cliente recibe 403
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    // Responde 201 (Creado)
    @ResponseStatus(HttpStatus.CREATED)
    // Texto en Swagger
    @Operation(summary = "Registrar una atencion clinica")
    // Recibe el id de la mascota (URL) y los datos (JSON validado con @Valid)
    public AtencionResponse crearAtencion(@PathVariable Long mascotaId, @Valid @RequestBody AtencionRequest req) {
        // El service crea la atención y se devuelve su respuesta
        return AtencionResponse.de(servicio.crearAtencion(mascotaId, req));
    }

    // READ (uno): GET /api/v1/historial/atenciones/5
    @GetMapping("/atenciones/{id}")
    // Texto en Swagger
    @Operation(summary = "Obtener una atencion por id")
    // Recibe el id de la atención desde la URL
    public AtencionResponse obtenerAtencion(@PathVariable Long id) {
        // Busca la atención (404 si no existe) y la convierte a respuesta
        return AtencionResponse.de(servicio.obtenerAtencion(id));
    }

    // UPDATE: PUT /api/v1/historial/atenciones/5
    @PutMapping("/atenciones/{id}")
    // Solo personal de la clínica
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    // Texto en Swagger
    @Operation(summary = "Actualizar una atencion")
    // Recibe el id (URL) y los datos nuevos (JSON validado)
    public AtencionResponse actualizarAtencion(@PathVariable Long id, @Valid @RequestBody AtencionRequest req) {
        // El service actualiza y se devuelve la atención modificada
        return AtencionResponse.de(servicio.actualizarAtencion(id, req));
    }

    // DELETE: DELETE /api/v1/historial/atenciones/5
    @DeleteMapping("/atenciones/{id}")
    // Solo el ADMIN puede borrar
    @PreAuthorize("hasRole('ADMIN')")
    // Responde 204 (Sin contenido): borrado correcto
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // Texto en Swagger
    @Operation(summary = "Eliminar una atencion (solo admin)")
    // "void" = no devuelve nada
    public void eliminarAtencion(@PathVariable Long id) {
        // El service borra la atención
        servicio.eliminarAtencion(id);
    }

    // ============================== VACUNAS ==============================

    // READ (listar): GET /api/v1/historial/mascotas/1/vacunas
    @GetMapping("/mascotas/{mascotaId}/vacunas")
    // Texto en Swagger
    @Operation(summary = "Listar vacunas de una mascota con su estado (al-dia, proxima, atrasada)")
    // Recibe el id de la mascota desde la URL
    public List<VacunaResponse> listarVacunas(@PathVariable Long mascotaId) {
        // Pide la lista y convierte cada vacuna a respuesta (con el estado calculado)
        return servicio.listarVacunas(mascotaId).stream().map(servicio::aRespuesta).toList();
    }

    // CREATE: POST /api/v1/historial/mascotas/1/vacunas
    @PostMapping("/mascotas/{mascotaId}/vacunas")
    // Solo personal de la clínica
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    // Responde 201 (Creado)
    @ResponseStatus(HttpStatus.CREATED)
    // Texto en Swagger
    @Operation(summary = "Registrar una vacuna")
    // Recibe el id de la mascota (URL) y los datos de la vacuna (JSON validado)
    public VacunaResponse crearVacuna(@PathVariable Long mascotaId, @Valid @RequestBody VacunaRequest req) {
        // El service crea la vacuna y se convierte a respuesta
        return servicio.aRespuesta(servicio.crearVacuna(mascotaId, req));
    }

    // READ (uno): GET /api/v1/historial/vacunas/3
    @GetMapping("/vacunas/{id}")
    // Texto en Swagger
    @Operation(summary = "Obtener una vacuna por id")
    // Recibe el id de la vacuna desde la URL
    public VacunaResponse obtenerVacuna(@PathVariable Long id) {
        // Busca la vacuna (404 si no existe) y la convierte a respuesta
        return servicio.aRespuesta(servicio.obtenerVacuna(id));
    }

    // UPDATE: PUT /api/v1/historial/vacunas/3
    @PutMapping("/vacunas/{id}")
    // Solo personal de la clínica
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    // Texto en Swagger
    @Operation(summary = "Actualizar una vacuna")
    // Recibe el id (URL) y los datos corregidos (JSON validado)
    public VacunaResponse actualizarVacuna(@PathVariable Long id, @Valid @RequestBody VacunaRequest req) {
        // El service actualiza y se devuelve la vacuna modificada
        return servicio.aRespuesta(servicio.actualizarVacuna(id, req));
    }

    // DELETE: DELETE /api/v1/historial/vacunas/3
    @DeleteMapping("/vacunas/{id}")
    // Solo el ADMIN puede borrar
    @PreAuthorize("hasRole('ADMIN')")
    // Responde 204 (Sin contenido)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // Texto en Swagger
    @Operation(summary = "Eliminar una vacuna (solo admin)")
    // No devuelve nada
    public void eliminarVacuna(@PathVariable Long id) {
        // El service borra la vacuna
        servicio.eliminarVacuna(id);
    }
}
