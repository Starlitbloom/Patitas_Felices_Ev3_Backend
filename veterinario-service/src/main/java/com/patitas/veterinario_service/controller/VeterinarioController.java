
package com.patitas.veterinario_service.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.patitas.veterinario_service.model.Veterinario;
import com.patitas.veterinario_service.service.VeterinarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST del microservicio de veterinarios.
 *
 * Permite gestionar los veterinarios de Patitas Felices
 * mediante operaciones CRUD.
 *
 * Las consultas GET son publicas.
 * Las operaciones POST, PUT y DELETE requieren un token
 * JWT con permisos de administrador.
 *
 * El controlador recibe las solicitudes HTTP y delega
 * la logica de negocio a VeterinarioService.
 */
@RestController
@RequestMapping("/api/v1/veterinarios")
@RequiredArgsConstructor
public class VeterinarioController {

    /**
     * Servicio encargado de gestionar los datos
     * de los veterinarios almacenados en MySQL.
     */
    private final VeterinarioService veterinarioService;

    /**
     * Obtiene todos los veterinarios registrados.
     *
     * GET /api/v1/veterinarios
     *
     * Acceso publico.
     *
     * @return HTTP 200 con la lista de veterinarios.
     */
    @GetMapping
    public ResponseEntity<List<Veterinario>> listarVeterinarios() {

        List<Veterinario> veterinarios =
                veterinarioService.listarVeterinarios();

        return ResponseEntity.ok(veterinarios);
    }

    /**
     * Busca un veterinario mediante su identificador.
     *
     * GET /api/v1/veterinarios/{id}
     *
     * Acceso publico.
     *
     * @param id Identificador del veterinario.
     * @return HTTP 200 con el veterinario encontrado.
     *         HTTP 404 si no existe.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Veterinario> obtenerVeterinario(
            @PathVariable Long id) {

        Veterinario veterinario =
                veterinarioService.obtenerVeterinarioPorId(id);

        return ResponseEntity.ok(veterinario);
    }

    /**
     * Registra un nuevo veterinario en MySQL.
     *
     * POST /api/v1/veterinarios
     *
     * Requiere un token JWT con rol ADMIN.
     *
     * @Valid verifica las restricciones del modelo.
     *
     * @param veterinario Datos del nuevo veterinario.
     * @return HTTP 201 con el veterinario registrado.
     *         HTTP 400 si los datos son invalidos.
     *         HTTP 409 si el correo ya existe.
     */
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<Veterinario> crearVeterinario(
            @Valid @RequestBody Veterinario veterinario) {

        Veterinario nuevo =
                veterinarioService.crearVeterinario(veterinario);

        URI ubicacion = URI.create(
                "/api/v1/veterinarios/" + nuevo.getId()
        );

        return ResponseEntity.created(ubicacion).body(nuevo);
    }

    /**
     * Actualiza los datos de un veterinario existente.
     *
     * PUT /api/v1/veterinarios/{id}
     *
     * Requiere un token JWT con rol ADMIN.
     *
     * @param id Identificador del veterinario.
     * @param datos Informacion actualizada.
     * @return HTTP 200 con los datos actualizados.
     *         HTTP 404 si el veterinario no existe.
     *         HTTP 409 si el correo esta duplicado.
     */
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<Veterinario> actualizarVeterinario(
            @PathVariable Long id,
            @Valid @RequestBody Veterinario datos) {

        Veterinario actualizado =
                veterinarioService.actualizarVeterinario(id, datos);

        return ResponseEntity.ok(actualizado);
    }

    /**
     * Elimina un veterinario de MySQL.
     *
     * DELETE /api/v1/veterinarios/{id}
     *
     * Requiere un token JWT con rol ADMIN.
     *
     * @param id Identificador del veterinario.
     * @return HTTP 204 si la eliminacion fue correcta.
     *         HTTP 404 si el veterinario no existe.
     */
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarVeterinario(
            @PathVariable Long id) {

        veterinarioService.eliminarVeterinario(id);

        return ResponseEntity.noContent().build();
    }
}
