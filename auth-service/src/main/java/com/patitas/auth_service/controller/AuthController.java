package com.patitas.auth_service.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.patitas.auth_service.model.Usuario;
import com.patitas.auth_service.service.AuthService;
import com.patitas.auth_service.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController // Maneja peticiones HTTP y responde JSON
@RequestMapping("/api/v1/auth") // Ruta base
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login, registro y renovación de tokens JWT")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    @Operation(summary = "Iniciar sesión",
        description = "Recibe correo y contraseña. Si son correctos devuelve el token de acceso (30 min), "
            + "el refresh token (7 días) y los datos del usuario sin contraseña.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(value = "{ \"correo\": \"ma.calfileo@duoc.cl\", \"password\": \"1234\" }"))))
    @ApiResponse(responseCode = "200", description = "Login correcto: { token, refreshToken, tipo, expiraEnMs, usuario }")
    @ApiResponse(responseCode = "401", description = "Correo no registrado o contraseña incorrecta", content = @Content)
    @ApiResponse(responseCode = "403", description = "Cuenta desactivada", content = @Content)
    @SecurityRequirements // Público: no pide token en Swagger
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> datos) {
        return ResponseEntity.ok(authService.login(datos.get("correo"), datos.get("password")));
    }

    @Operation(summary = "Registrar cuenta nueva (cliente)",
        description = "Registro público desde la página /registro. La cuenta queda siempre con rol CLIENTE "
            + "(aunque se envíe otro rol) y con la sesión iniciada (devuelve los tokens).")
    @ApiResponse(responseCode = "201", description = "Cuenta creada: { token, refreshToken, tipo, expiraEnMs, usuario }")
    @ApiResponse(responseCode = "400", description = "Algún dato no es válido (RUN, correo, contraseña...)", content = @Content)
    @ApiResponse(responseCode = "409", description = "El correo o el RUN ya están registrados", content = @Content)
    @SecurityRequirements
    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registro(@RequestBody Usuario datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(datos));
    }

    @Operation(summary = "Renovar token de acceso",
        description = "Recibe el refresh token y entrega un token de acceso nuevo sin volver a pedir la contraseña.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(value = "{ \"refreshToken\": \"eyJhbGciOiJIUzI1NiJ9...\" }"))))
    @ApiResponse(responseCode = "200", description = "Tokens renovados")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado", content = @Content)
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> datos) {
        return ResponseEntity.ok(authService.renovar(datos.get("refreshToken")));
    }

    @Operation(summary = "Usuario de la sesión actual",
        description = "Devuelve los datos del dueño del token. El frontend lo usa al recargar la página "
            + "para comprobar que la sesión guardada sigue siendo válida.")
    @ApiResponse(responseCode = "200", description = "Usuario de la sesión",
        content = @Content(schema = @Schema(implementation = Usuario.class)))
    @ApiResponse(responseCode = "401", description = "Sin token o token expirado", content = @Content)
    @GetMapping("/me")
    public ResponseEntity<Usuario> me(Authentication auth) {
        return ResponseEntity.ok(usuarioService.obtenerPorCorreo(auth.getName()));
    }
}
