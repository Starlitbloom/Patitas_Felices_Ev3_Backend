// Carpeta (paquete) donde vive este archivo: exception
package com.patitas.historial_service.exception;

// Mapa que mantiene el orden en que se agregan los datos
import java.util.LinkedHashMap;
// Tipo "mapa" (clave -> valor), se convierte en un objeto JSON
import java.util.Map;

// Códigos HTTP con nombre (NOT_FOUND = 404, CONFLICT = 409...)
import org.springframework.http.HttpStatus;
// Respuesta HTTP completa: código + cuerpo
import org.springframework.http.ResponseEntity;
// Error cuando el JSON viene mal escrito
import org.springframework.http.converter.HttpMessageNotReadableException;
// Error cuando @PreAuthorize niega el acceso (rol insuficiente)
import org.springframework.security.access.AccessDeniedException;
// Error cuando falla una validación (@NotBlank, @Size...)
import org.springframework.web.bind.MethodArgumentNotValidException;
// Marca un método como "atrapador" de un tipo de error
import org.springframework.web.bind.annotation.ExceptionHandler;
// Hace que esta clase atrape errores de TODOS los controllers
import org.springframework.web.bind.annotation.RestControllerAdvice;
// Error cuando un parámetro de la URL tiene el tipo equivocado (ej: /atenciones/abc)
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Convierte los errores en JSON con el mismo formato que auth-service: { "status": 404, "campo": null, "mensaje": "..." }
// "campo" indica qué input del formulario marcar en rojo (solo en errores de validación)
@RestControllerAdvice
public class ApiExceptionHandler {

    // Atrapa RecursoNoEncontradoException
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex) {
        // Responde 404 con el mensaje de la excepción
        return respuesta(HttpStatus.NOT_FOUND, null, ex.getMessage(), null);
    }

    // Atrapa ConflictoException
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, Object>> conflicto(ConflictoException ex) {
        // Responde 409 con el mensaje de la regla que no se cumplió
        return respuesta(HttpStatus.CONFLICT, null, ex.getMessage(), null);
    }

    // Atrapa errores de validación de los DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        // Mapa campo -> mensaje de error
        Map<String, String> errores = new LinkedHashMap<>();
        // Recorre cada campo con error y guarda solo el primer mensaje de cada uno
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
        // El primer campo con error es el que el frontend marca en rojo
        String campo = errores.isEmpty() ? null : errores.keySet().iterator().next();
        // Mensaje principal: el de ese primer campo
        String mensaje = errores.isEmpty() ? "Datos invalidos" : errores.get(campo);
        // Responde 400 e incluye la lista completa de errores
        return respuesta(HttpStatus.BAD_REQUEST, campo, mensaje, errores);
    }

    // Atrapa el error de "rol insuficiente" que lanza @PreAuthorize
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accesoDenegado(AccessDeniedException ex) {
        // Responde 403
        return respuesta(HttpStatus.FORBIDDEN, null, "No tienes permisos para realizar esta acción.", null);
    }

    // Atrapa JSON mal escrito o parámetros con tipo incorrecto
    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<Map<String, Object>> peticionInvalida(Exception ex) {
        // Responde 400 con un mensaje genérico
        return respuesta(HttpStatus.BAD_REQUEST, null, "La peticion tiene un formato invalido.", null);
    }

    // Método ayudante: arma el JSON de respuesta con el formato común
    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String campo, String mensaje, Object errores) {
        // Mapa ordenado que será el cuerpo del JSON
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        // Código numérico (404, 409...)
        cuerpo.put("status", estado.value());
        // Campo con error (o null)
        cuerpo.put("campo", campo);
        // Mensaje para el usuario
        cuerpo.put("mensaje", mensaje);
        // Solo si hay varios errores de validación, se agregan todos
        if (errores != null) cuerpo.put("errores", errores);
        // Devuelve la respuesta con su código HTTP y el cuerpo
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
