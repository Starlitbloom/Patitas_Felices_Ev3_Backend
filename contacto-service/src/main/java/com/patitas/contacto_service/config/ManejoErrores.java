package com.patitas.contacto_service.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.patitas.contacto_service.service.NegocioException;

/**
 * Atrapa los errores de todos los controladores y responde siempre
 * el mismo JSON: { "status": 409, "campo": "correo", "mensaje": "..." }
 * Así no hay que escribir try/catch en cada endpoint y el frontend
 * sabe qué input marcar en rojo.
 */
@RestControllerAdvice
public class ManejoErrores {

    public static Map<String, Object> cuerpo(int status, String campo, String mensaje) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", status);
        error.put("campo", campo);
        error.put("mensaje", mensaje);
        return error;
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Map<String, Object>> negocio(NegocioException e) {
        return ResponseEntity.status(e.getStatus()).body(cuerpo(e.getStatus().value(), e.getCampo(), e.getMessage()));
    }

    // @PreAuthorize rechazó la petición (rol sin permiso)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accesoDenegado(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(cuerpo(403, null, "No tienes permisos para realizar esta acción."));
    }

    // JSON mal formado o id que no es número (ej: /usuarios/abc)
    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<Map<String, Object>> peticionInvalida(Exception e) {
        return ResponseEntity.badRequest().body(cuerpo(400, null, "La petición tiene un formato inválido."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> rutaInexistente(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo(404, null, "La ruta solicitada no existe."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> general(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(cuerpo(500, null, "Error interno del servidor: " + e.getMessage()));
    }
}
