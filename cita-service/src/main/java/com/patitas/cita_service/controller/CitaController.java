
package com.patitas.cita_service.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.patitas.cita_service.model.Cita;
import com.patitas.cita_service.service.CitaService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;

/**
 * Controlador REST del microservicio de citas veterinarias.
 *
 * Gestiona las solicitudes HTTP relacionadas con
 * las reservas de atencion de Patitas Felices.
 *
 * Funcionalidades:
 *
 * - Registrar nuevas citas veterinarias.
 * - Consultar las citas del usuario autenticado.
 * - Consultar una cita por su identificador.
 * - Modificar reservas existentes.
 * - Cancelar citas.
 * - Consultar todas las citas como administrador.
 * - Cambiar el estado de una cita.
 * - Eliminar citas como administrador.
 *
 * Utiliza autenticacion JWT para identificar al usuario.
 *
 * La logica de negocio y las validaciones de disponibilidad
 * se realizan en CitaService.
 */
@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CitaController {

    /**
     * Servicio encargado de gestionar las reservas
     * y su persistencia en MySQL.
     *
     * Lombok genera el constructor para inyectarlo.
     */
    private final CitaService citaService;

    /**
     * Obtiene todas las citas registradas.
     *
     * GET /api/citas
     *
     * Acceso exclusivo para administradores.
     *
     * @return HTTP 200 con todas las citas.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Cita>> listarTodas() {

        return ResponseEntity.ok(
                citaService.listarTodas()
        );
    }

    /**
     * Obtiene el historial de citas del usuario autenticado.
     *
     * GET /api/citas/mis-citas
     *
     * El correo se obtiene de la autenticacion JWT.
     * No se permite indicar el correo de otro cliente.
     *
     * @param authentication Usuario autenticado.
     * @return HTTP 200 con sus citas.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mis-citas")
    public ResponseEntity<List<Cita>> listarMisCitas(
            Authentication authentication) {

        String correoUsuario = authentication.getName();

        List<Cita> citas =
                citaService.listarMisCitas(correoUsuario);

        return ResponseEntity.ok(citas);
    }

    /**
     * Consulta una cita mediante su ID.
     *
     * GET /api/citas/{id}
     *
     * Los clientes solo pueden consultar sus propias citas.
     * Los administradores pueden consultar cualquier cita.
     *
     * @param id Identificador de la cita.
     * @param authentication Usuario autenticado.
     * @return HTTP 200 con la cita encontrada.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<Cita> obtenerCita(
            @PathVariable Long id,
            Authentication authentication) {

        String correoUsuario = authentication.getName();
        boolean esAdmin = esAdministrador(authentication);

        Cita cita = citaService.obtenerCita(
                id,
                correoUsuario,
                esAdmin
        );

        return ResponseEntity.ok(cita);
    }

    /**
     * Registra una nueva cita veterinaria.
     *
     * POST /api/citas
     *
     * Requiere un usuario autenticado.
     *
     * @Valid comprueba las restricciones del modelo Cita.
     *
     * El correo de la cuenta se obtiene del JWT.
     * Por seguridad, no se confia en un correoUsuario
     * enviado desde el formulario.
     *
     * El nombre del usuario autenticado se completara
     * posteriormente desde la informacion del JWT.
     *
     * @param datos Informacion de la reserva.
     * @param authentication Usuario autenticado.
     * @return HTTP 201 con la cita registrada en MySQL.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<Cita> crearCita(
            @Valid @RequestBody Cita datos,
            Authentication authentication) {

        String correoUsuario = authentication.getName();

        // El nombre de la cuenta se completara
        // al integrar los datos del JWT de auth-service.
        String nombreUsuario = null;

        Cita nueva = citaService.crearCita(
                datos,
                correoUsuario,
                nombreUsuario
        );

        URI ubicacion = URI.create(
                "/api/citas/" + nueva.getId()
        );

        return ResponseEntity.created(ubicacion).body(nueva);
    }

    /**
     * Actualiza una cita existente.
     *
     * PUT /api/citas/{id}
     *
     * Un cliente solo puede modificar sus propias citas.
     * Un administrador puede modificar cualquier cita
     * que aun permita cambios.
     *
     * El servicio comprueba:
     *
     * - Que la cita exista.
     * - Que el usuario tenga permiso.
     * - Que la fecha y la hora sean validas.
     * - Que el veterinario tenga disponibilidad.
     * - Que no existan horarios superpuestos.
     *
     * @param id Identificador de la cita.
     * @param datos Datos nuevos de la reserva.
     * @param authentication Usuario autenticado.
     * @return HTTP 200 con la cita actualizada.
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<Cita> actualizarCita(
            @PathVariable Long id,
            @Valid @RequestBody Cita datos,
            Authentication authentication) {

        String correoUsuario = authentication.getName();
        boolean esAdmin = esAdministrador(authentication);

        Cita actualizada = citaService.actualizarCita(
                id,
                datos,
                correoUsuario,
                esAdmin
        );

        return ResponseEntity.ok(actualizada);
    }

    /**
     * Cancela una reserva veterinaria.
     *
     * PATCH /api/citas/{id}/cancelar
     *
     * El cliente puede cancelar sus propias citas.
     * El administrador puede cancelar cualquier cita
     * que aun permita esta operacion.
     *
     * La reserva no se elimina de MySQL:
     * solamente cambia su estado a "Cancelada".
     *
     * @param id Identificador de la cita.
     * @param authentication Usuario autenticado.
     * @return HTTP 200 con la cita cancelada.
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Cita> cancelarCita(
            @PathVariable Long id,
            Authentication authentication) {

        String correoUsuario = authentication.getName();
        boolean esAdmin = esAdministrador(authentication);

        Cita cancelada = citaService.cancelarCita(
                id,
                correoUsuario,
                esAdmin
        );

        return ResponseEntity.ok(cancelada);
    }

    /**
     * Cambia el estado de una cita veterinaria.
     *
     * PATCH /api/citas/{id}/estado
     *
     * Acceso exclusivo para administradores.
     *
     * Estados permitidos:
     *
     * - Pendiente
     * - Confirmada
     * - Completada
     * - Cancelada
     *
     * El servicio valida las transiciones permitidas.
     *
     * Ejemplo de solicitud JSON:
     *
     * {
     *     "estado": "Confirmada"
     * }
     *
     * @param id Identificador de la cita.
     * @param solicitud Nuevo estado solicitado.
     * @return HTTP 200 con el estado actualizado.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Cita> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody EstadoRequest solicitud) {

        Cita cita = citaService.cambiarEstado(
                id,
                solicitud.estado()
        );

        return ResponseEntity.ok(cita);
    }

    /**
     * Elimina definitivamente una cita.
     *
     * DELETE /api/citas/{id}
     *
     * Acceso exclusivo para administradores.
     *
     * Los clientes deben utilizar el endpoint
     * de cancelacion para conservar su historial.
     *
     * @param id Identificador de la cita.
     * @return HTTP 204 cuando la eliminacion es correcta.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCita(
            @PathVariable Long id) {

        citaService.eliminarCita(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Comprueba si el usuario autenticado
     * tiene permisos de administrador.
     *
     * Spring Security utiliza el prefijo ROLE_
     * para representar los roles de los usuarios.
     *
     * @param authentication Usuario autenticado.
     * @return true si tiene rol ADMIN;
     *         false en caso contrario.
     */
    private boolean esAdministrador(
            Authentication authentication) {

        return authentication.getAuthorities()
                .stream()
                .anyMatch(autoridad ->
                        "ROLE_ADMIN".equals(
                                autoridad.getAuthority()
                        )
                );
    }

    /**
     * Estructura utilizada para recibir el nuevo
     * estado de una cita desde React o Swagger.
     *
     * No es una entidad ni crea una tabla en MySQL.
     *
     * @param estado Estado solicitado.
     */
    public record EstadoRequest(
            @NotBlank(message = "El estado es obligatorio")
            String estado
    ) {
    }
}
