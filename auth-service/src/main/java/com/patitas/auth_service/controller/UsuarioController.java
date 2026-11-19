package com.patitas.auth_service.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.patitas.auth_service.model.Usuario;
import com.patitas.auth_service.service.NegocioException;
import com.patitas.auth_service.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * CRUD de usuarios.
 * Permisos:
 * - ADMIN: todo
 * - RECEPCIONISTA: puede consultar usuarios por id/correo (para citas y fichas)
 * - CLIENTE: solo ver y editar SU propio perfil
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "CRUD de usuarios (panel admin y perfil)")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Listar todos los usuarios", description = "Solo ADMIN. Ordenados del más reciente al más antiguo.")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = Usuario.class))))
    @ApiResponse(responseCode = "403", description = "No es administrador", content = @Content)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    @Operation(summary = "Estadísticas de usuarios",
        description = "Solo ADMIN. KPIs para el dashboard: total, activos, inactivos, nuevosEstaSemana, clientes, personal.")
    @ApiResponse(responseCode = "200", description = "Estadísticas")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Long>> estadisticas() {
        return ResponseEntity.ok(usuarioService.estadisticas());
    }

    @Operation(summary = "Obtener usuario por ID", description = "ADMIN y RECEPCIONISTA ven cualquiera; un CLIENTE solo a sí mismo.")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado",
        content = @Content(schema = @Schema(implementation = Usuario.class)))
    @ApiResponse(responseCode = "403", description = "Intenta ver a otro usuario sin permisos", content = @Content)
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content)
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtener(@PathVariable Long id, Authentication auth) {
        Usuario usuario = usuarioService.obtenerPorId(id);
        verificarPropioOPersonal(usuario, auth);
        return ResponseEntity.ok(usuario);
    }

    @Operation(summary = "Obtener usuario por correo",
        description = "ADMIN y RECEPCIONISTA ven cualquiera; un CLIENTE solo a sí mismo. "
            + "Lo usan otros microservicios (mascotas, citas) para validar al dueño.")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado",
        content = @Content(schema = @Schema(implementation = Usuario.class)))
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content)
    @GetMapping("/correo/{correo}")
    public ResponseEntity<Usuario> obtenerPorCorreo(@PathVariable String correo, Authentication auth) {
        Usuario usuario = usuarioService.obtenerPorCorreo(correo);
        verificarPropioOPersonal(usuario, auth);
        return ResponseEntity.ok(usuario);
    }

    @Operation(summary = "Crear usuario (panel admin)",
        description = "Solo ADMIN. Puede elegir el rol enviando \"rol\": { \"nombre\": \"RECEPCIONISTA\" } y si queda activo.")
    @ApiResponse(responseCode = "201", description = "Usuario creado",
        content = @Content(schema = @Schema(implementation = Usuario.class)))
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    @ApiResponse(responseCode = "409", description = "Correo o RUN repetido", content = @Content)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Usuario> crear(@RequestBody Usuario datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearUsuario(datos, true));
    }

    @Operation(summary = "Actualizar usuario",
        description = "ADMIN edita a cualquiera (incluido rol y estado). Un usuario normal solo edita su propio perfil "
            + "y no puede cambiarse el rol ni el estado. Los campos que no se envían no cambian.")
    @ApiResponse(responseCode = "200", description = "Usuario actualizado",
        content = @Content(schema = @Schema(implementation = Usuario.class)))
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content)
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Long id, @RequestBody Usuario cambios, Authentication auth) {
        Usuario usuario = usuarioService.obtenerPorId(id);
        boolean esAdmin = tieneRol(auth, "ADMIN");
        if (!esAdmin && !esElMismo(usuario, auth)) {
            throw new NegocioException(null, "Solo puedes editar tu propio perfil.", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, cambios, esAdmin));
    }

    @Operation(summary = "Eliminar usuario", description = "Solo ADMIN. Las cuentas fijas del sistema no se pueden eliminar.")
    @ApiResponse(responseCode = "204", description = "Usuario eliminado", content = @Content)
    @ApiResponse(responseCode = "403", description = "No es admin o es una cuenta fija", content = @Content)
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    /* ---------------- Auxiliares de permisos ---------------- */

    private boolean tieneRol(Authentication auth, String rol) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }

    private boolean esElMismo(Usuario usuario, Authentication auth) {
        return usuario.getCorreo().equalsIgnoreCase(auth.getName());
    }

    private void verificarPropioOPersonal(Usuario usuario, Authentication auth) {
        boolean personal = tieneRol(auth, "ADMIN") || tieneRol(auth, "RECEPCIONISTA");
        if (!personal && !esElMismo(usuario, auth)) {
            throw new NegocioException(null, "No tienes permisos para ver este usuario.", HttpStatus.FORBIDDEN);
        }
    }
}
