package com.patitas.mascota_service.exception;

import java.nio.file.AccessDeniedException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Convierte las excepciones en respuestas JSON con el mismo formato que auth-service:
 *   { "status": 404, "campo": null, "mensaje": "..." }
 * "campo" dice que input del formulario marcar en rojo (solo en errores de validacion).
 */
@RestControllerAdvice  // Esta clase vigila TODOS los controllers: si uno lanza un error, lo atrapa aquí y lo convierte en JSON limpio
public class ApiExceptionHandler {  // Clase que reúne todos los manejadores de errores

    @ExceptionHandler(RecursoNoEncontradoException.class)  // Atrapa el error "no existe lo que buscas"
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex) {  // Recibe el error y devuelve una respuesta JSON
        return respuesta(HttpStatus.NOT_FOUND, null, ex.getMessage(), null);  // Responde 404 con el mensaje del error; sin campo ni lista de errores
    }  // Cierra el método

    @ExceptionHandler(ConflictoException.class)  // Atrapa el error de conflicto (ej: ya hay una solicitud pendiente)
    public ResponseEntity<Map<String, Object>> conflicto(ConflictoException ex) {  // Recibe el error y devuelve una respuesta JSON
        return respuesta(HttpStatus.CONFLICT, null, ex.getMessage(), null);  // Responde 409 con el mensaje del error
    }  // Cierra el método

    @ExceptionHandler(MethodArgumentNotValidException.class)  // Atrapa los errores de validación de los DTO (@NotBlank, @Size, @Pattern...)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {  // Recibe el error con la lista de campos inválidos
        Map<String, String> errores = new LinkedHashMap<>();  // Mapa vacío donde se guardará: campo -> mensaje de error
        ex.getBindingResult().getFieldErrors()  // Obtiene la lista de campos que fallaron
            .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));  // Por cada campo, guarda su mensaje (si un campo falla dos veces, deja solo el primero)
        String campo = errores.isEmpty() ? null : errores.keySet().iterator().next();  // Toma el primer campo con error (para que el frontend lo marque en rojo); null si no hay
        String mensaje = errores.isEmpty() ? "Datos invalidos" : errores.get(campo);  // Toma el mensaje de ese campo; o uno genérico si no hay
        return respuesta(HttpStatus.BAD_REQUEST, campo, mensaje, errores);  // Responde 400 con el campo, el mensaje y la lista completa de errores
    }  // Cierra el método

    @ExceptionHandler(AccessDeniedException.class)  // Atrapa el error de "no tienes permiso" (rol insuficiente)
    public ResponseEntity<Map<String, Object>> accesoDenegado(AccessDeniedException ex) {  // Recibe el error y devuelve una respuesta JSON
        return respuesta(HttpStatus.FORBIDDEN, null, "No tienes permisos para realizar esta accion.", null);  // Responde 403 con un mensaje fijo
    }  // Cierra el método

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })  // Atrapa dos errores a la vez: JSON mal formado e id que no es número
    public ResponseEntity<Map<String, Object>> peticionInvalida(Exception ex) {  // Recibe cualquiera de los dos errores
        return respuesta(HttpStatus.BAD_REQUEST, null, "La peticion tiene un formato invalido.", null);  // Responde 400 con un mensaje fijo
    }  // Cierra el método

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String campo, String mensaje, Object errores) {  // Método ayudante (privado): arma el JSON de error para que todos salgan con el mismo formato
        Map<String, Object> cuerpo = new LinkedHashMap<>();  // Crea el mapa que será el JSON de respuesta
        cuerpo.put("status", estado.value());  // Agrega el número del código HTTP (404, 400, ...)
        cuerpo.put("campo", campo);  // Agrega el campo con error (o null)
        cuerpo.put("mensaje", mensaje);  // Agrega el mensaje para mostrar al usuario
        if (errores != null) cuerpo.put("errores", errores);  // Si hay una lista de errores de validación, la agrega también
        return ResponseEntity.status(estado).body(cuerpo);  // Devuelve la respuesta: el código HTTP y el JSON armado
    }  
}  