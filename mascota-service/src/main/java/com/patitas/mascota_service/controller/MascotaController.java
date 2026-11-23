// Carpeta (paquete) donde vive este archivo: controller
package com.patitas.mascota_service.controller;

// ---- Imports (herramientas que usa el controller) ----
// Para devolver listas
import java.util.List;

// @Valid: activa las validaciones del DTO (@NotBlank, @Size...) antes de entrar al método
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
// @Operation: texto que aparece en Swagger junto a cada endpoint
import io.swagger.v3.oas.annotations.Operation;
// @Tag: nombre del grupo de endpoints en Swagger
import io.swagger.v3.oas.annotations.tags.Tag;

// Códigos HTTP con nombre (CREATED = 201, NO_CONTENT = 204)
import org.springframework.http.HttpStatus;
// @PreAuthorize: restringe un endpoint a ciertos roles
import org.springframework.security.access.prepost.PreAuthorize;
// Authentication: el usuario autenticado de la petición (lo dejó el JwtRequestFilter)
import org.springframework.security.core.Authentication;
// Anotaciones que mapean cada método a un verbo y una ruta HTTP
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

// Clases de tu proyecto que usa el controller
import com.patitas.mascota_service.config.UsuarioActual;
import com.patitas.mascota_service.dto.MascotaRequest;
import com.patitas.mascota_service.dto.MascotaResponse;
import com.patitas.mascota_service.dto.SolicitudRequest;
import com.patitas.mascota_service.dto.SolicitudResponse;
import com.patitas.mascota_service.service.MascotaService;

/**
 * API REST de mascotas.
 *   GET    /api/v1/mascotas              listar (cliente: las suyas; personal: todas)  ?especie=&texto=
 *   GET    /api/v1/mascotas/{id}         detalle
 *   POST   /api/v1/mascotas              crear
 *   PUT    /api/v1/mascotas/{id}         actualizar          (admin, recepcionista)
 *   DELETE /api/v1/mascotas/{id}         eliminar            (admin)
 *   POST   /api/v1/mascotas/{id}/solicitudes   pedir cambio de datos
 *   GET    /api/v1/solicitudes  listar solicitudes  (admin, recepcionista)  ?estado=
 *   PUT    /api/v1/solicitudes/{id}/aprobar | /rechazar   (admin, recepcionista)
 */
// @RestController: esta clase recibe peticiones HTTP y responde en JSON
@RestController
// Todas las rutas de esta clase empiezan con /api/v1
@RequestMapping("/api/v1")
// Nombre y descripción del grupo en Swagger
@Tag(name = "Mascotas", description = "CRUD de mascotas y solicitudes de cambio")
// El controller SOLO recibe la petición y llama al service; la lógica está en MascotaService
// Lombok crea el constructor con los campos "final" (el service); Spring lo usa para entregarlo
@RequiredArgsConstructor
public class MascotaController {

    // El service con la lógica de negocio
    private final MascotaService servicio;

    // READ (listar): responde a GET /api/v1/mascotas
    @GetMapping("/mascotas")
    // Texto que se ve en Swagger
    @Operation(summary = "Listar mascotas (filtros opcionales por especie y nombre)")
    // Recibe: el usuario autenticado y dos filtros opcionales de la URL (?especie=Gato&texto=lu)
    public List<MascotaResponse> listar(Authentication auth,
                                        // @RequestParam: lee el parámetro "especie" de la URL; required = false = es opcional
                                        @RequestParam(required = false) String especie,
                                        // Lo mismo con "texto" (parte del nombre)
                                        @RequestParam(required = false) String texto) {
        // Convierte el usuario a UsuarioActual, pide la lista al service, y cada mascota la convierte a MascotaResponse (lo que sale por el JSON)
        return servicio.listar(UsuarioActual.de(auth), especie, texto).stream().map(MascotaResponse::de).toList();
    }

    // READ (uno): responde a GET /api/v1/mascotas/1 (el 1 es el id)
    @GetMapping("/mascotas/{id}")
    // Texto en Swagger
    @Operation(summary = "Obtener una mascota por id")
    // @PathVariable: toma el id desde la URL
    public MascotaResponse obtener(@PathVariable Long id, Authentication auth) {
        // El service busca la mascota (y comprueba que el usuario pueda verla) y se convierte a respuesta
        return MascotaResponse.de(servicio.obtener(id, UsuarioActual.de(auth)));
    }

    // CREATE: responde a POST /api/v1/mascotas
    @PostMapping("/mascotas")
    // Responde con código 201 (Creado) en vez del 200 normal
    @ResponseStatus(HttpStatus.CREATED)
    // Texto en Swagger
    @Operation(summary = "Crear una mascota")
    // @Valid: revisa las validaciones del DTO; si fallan, responde 400. @RequestBody: lee el JSON del cuerpo y lo convierte a MascotaRequest
    public MascotaResponse crear(@Valid @RequestBody MascotaRequest req, Authentication auth) {
        // El service crea la mascota y se devuelve su versión de respuesta
        return MascotaResponse.de(servicio.crear(req, UsuarioActual.de(auth)));
    }

    // UPDATE: responde a PUT /api/v1/mascotas/1
    @PutMapping("/mascotas/{id}")
    // Solo pueden usarlo los roles ADMIN y RECEPCIONISTA; un cliente recibe 403
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    // Texto en Swagger
    @Operation(summary = "Actualizar una mascota (personal)")
    // Recibe el id de la URL y los datos nuevos del JSON (validados con @Valid)
    public MascotaResponse actualizar(@PathVariable Long id, @Valid @RequestBody MascotaRequest req) {
        // El service actualiza y se devuelve la mascota ya modificada
        return MascotaResponse.de(servicio.actualizar(id, req));
    }

    // DELETE: responde a DELETE /api/v1/mascotas/1
    @DeleteMapping("/mascotas/{id}")
    // Solo el ADMIN puede borrar; los demás reciben 403
    @PreAuthorize("hasRole('ADMIN')")
    // Responde 204 (Sin contenido): borrado correcto, no hay nada que devolver
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // Texto en Swagger
    @Operation(summary = "Eliminar una mascota (solo admin)")
    // "void" = no devuelve nada
    public void eliminar(@PathVariable Long id) {
        // El service borra la mascota y sus solicitudes
        servicio.eliminar(id);
    }

    // CREATE solicitud: responde a POST /api/v1/mascotas/1/solicitudes
    @PostMapping("/mascotas/{id}/solicitudes")
    // Responde 201 (Creado)
    @ResponseStatus(HttpStatus.CREATED)
    // Texto en Swagger
    @Operation(summary = "Solicitar cambio de datos de una mascota")
    // Recibe el id de la mascota, los cambios propuestos (JSON validado) y el usuario. No tiene @PreAuthorize: cualquier usuario con token puede; el service revisa que sea su mascota
    public SolicitudResponse solicitar(@PathVariable Long id, @Valid @RequestBody SolicitudRequest req, Authentication auth) {
        // El service crea la solicitud (aplica directo o la deja pendiente) y se devuelve su respuesta
        return SolicitudResponse.de(servicio.solicitarCambio(id, req, UsuarioActual.de(auth)));
    }

    // READ solicitudes: responde a GET /api/v1/solicitudes
    @GetMapping("/solicitudes")
    // Solo personal (ADMIN o RECEPCIONISTA)
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    // Texto en Swagger
    @Operation(summary = "Listar solicitudes de cambio (personal)")
    // Filtro opcional por estado de la URL (?estado=pendiente)
    public List<SolicitudResponse> solicitudes(@RequestParam(required = false) String estado) {
        // Pide la lista al service y convierte cada solicitud a su respuesta
        return servicio.listarSolicitudes(estado).stream().map(SolicitudResponse::de).toList();
    }

    // Aprobar: responde a PUT /api/v1/solicitudes/5/aprobar
    @PutMapping("/solicitudes/{id}/aprobar")
    // Solo personal
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    // Texto en Swagger
    @Operation(summary = "Aprobar una solicitud: aplica los cambios a la mascota")
    // Recibe el id de la solicitud
    public SolicitudResponse aprobar(@PathVariable Long id) {
        // resolver(id, true) = aprobar: aplica los cambios a la mascota
        return SolicitudResponse.de(servicio.resolver(id, true));
    }

    // Rechazar: responde a PUT /api/v1/solicitudes/5/rechazar
    @PutMapping("/solicitudes/{id}/rechazar")
    // Solo personal
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    // Texto en Swagger
    @Operation(summary = "Rechazar una solicitud")
    // Recibe el id de la solicitud
    public SolicitudResponse rechazar(@PathVariable Long id) {
        // resolver(id, false) = rechazar: no toca la mascota
        return SolicitudResponse.de(servicio.resolver(id, false));
    }
}