// Carpeta (paquete) donde vive este archivo: exception
package com.patitas.servicio_clinico_service.exception;

// Error propio: se lanza cuando se pide algo que no existe. El ApiExceptionHandler lo convierte en 404
public class RecursoNoEncontradoException extends RuntimeException {  // RuntimeException = no obliga a usar try/catch

    // Constructor: recibe el mensaje (ej: "Servicio 99 no encontrado")
    public RecursoNoEncontradoException(String mensaje) {
        // Guarda el mensaje en la clase padre
        super(mensaje);
    }
}
