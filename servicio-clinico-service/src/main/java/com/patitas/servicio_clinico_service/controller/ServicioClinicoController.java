// Carpeta (paquete) donde vive este archivo: controller
package com.patitas.servicio_clinico_service.controller;

// ---- Imports (herramientas que usa el controller) ----
// Para devolver listas
import java.util.List;

// @Valid: activa las validaciones del DTO antes de entrar al método
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Clases del proyecto que usa el controller
import com.patitas.servicio_clinico_service.dto.ServicioRequest;
import com.patitas.servicio_clinico_service.dto.ServicioResponse;
import com.patitas.servicio_clinico_service.service.ServicioClinicoService;

/**
 * API REST del catálogo de servicios clínicos.
 *   GET    /api/v1/servicios        listar activos (público)   ?especie=Perro&grupo=Cirugía
 *   GET    /api/v1/servicios/{id}   detalle (público)
 *   POST   /api/v1/servicios        crear       (admin)
 *   PUT    /api/v1/servicios/{id}   actualizar  (admin)
 *   DELETE /api/v1/servicios/{id}   eliminar    (admin)
 */
// @RestController: esta clase recibe peticiones HTTP y responde en JSON
@RestController
// Todas las rutas de esta clase empiezan con /api/v1/servicios
@RequestMapping("/api/v1/servicios")
// Nombre y descripción del grupo en Swagger
@Tag(name = "Servicios clinicos", description = "Catalogo de servicios de la clinica")
// Lombok crea el constructor con los campos "final" (el service); Spring lo usa para entregarlo
@RequiredArgsConstructor
// El controller SOLO recibe la petición y llama al service; la lógica está en ServicioClinicoService
public class ServicioClinicoController {

    // El service con la lógica de negocio (Spring lo entrega por el constructor que genera Lombok)
    private final ServicioClinicoService servicio;

    // READ (listar): GET /api/v1/servicios  (público, no necesita token)
    @GetMapping
    // Texto en Swagger
    @Operation(summary = "Listar servicios activos (filtros opcionales por especie y grupo)")
    // Dos filtros opcionales de la URL: ?especie=Gato&grupo=Cirugía
    public List<ServicioResponse> listar(@RequestParam(required = false) String especie,
                                         @RequestParam(required = false) String grupo) {
        // Pide la lista al service y convierte cada servicio a su respuesta JSON
        return servicio.listar(especie, grupo).stream().map(ServicioResponse::de).toList();
    }

    // READ (uno): GET /api/v1/servicios/3  (público)
    @GetMapping("/{id}")
    // Texto en Swagger
    @Operation(summary = "Obtener un servicio por id")
    // Toma el id desde la URL
    public ServicioResponse obtener(@PathVariable Long id) {
        // Busca el servicio (404 si no existe) y lo convierte a respuesta
        return ServicioResponse.de(servicio.obtener(id));
    }

    // CREATE: POST /api/v1/servicios
    @PostMapping
    // Solo el ADMIN puede agregar servicios al catálogo
    @PreAuthorize("hasRole('ADMIN')")
    // Responde 201 (Creado)
    @ResponseStatus(HttpStatus.CREATED)
    // Texto en Swagger
    @Operation(summary = "Crear un servicio (solo admin)")
    // Lee el JSON del cuerpo y lo valida
    public ServicioResponse crear(@Valid @RequestBody ServicioRequest req) {
        // El service crea el servicio y se devuelve su respuesta
        return ServicioResponse.de(servicio.crear(req));
    }

    // UPDATE: PUT /api/v1/servicios/3
    @PutMapping("/{id}")
    // Solo el ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    // Texto en Swagger
    @Operation(summary = "Actualizar un servicio (solo admin)")
    // Recibe el id (URL) y los datos nuevos (JSON validado)
    public ServicioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ServicioRequest req) {
        // El service actualiza y se devuelve el servicio modificado
        return ServicioResponse.de(servicio.actualizar(id, req));
    }

    // DELETE: DELETE /api/v1/servicios/3
    @DeleteMapping("/{id}")
    // Solo el ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    // Responde 204 (Sin contenido)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // Texto en Swagger
    @Operation(summary = "Eliminar un servicio (solo admin)")
    // No devuelve nada
    public void eliminar(@PathVariable Long id) {
        // El service borra el servicio
        servicio.eliminar(id);
    }
}
