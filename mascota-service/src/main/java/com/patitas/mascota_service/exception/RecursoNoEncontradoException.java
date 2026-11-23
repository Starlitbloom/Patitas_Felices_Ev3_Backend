// Carpeta (paquete) donde vive este archivo
package com.patitas.mascota_service.exception;

/** Se lanza cuando se pide un registro que no existe (responde 404). */
// Es un error PROPIO: significa "lo que buscas no existe". "extends RuntimeException" = es un tipo de error de Java
public class RecursoNoEncontradoException extends RuntimeException {
    // Constructor: recibe el mensaje del error (ej: "Mascota 99 no encontrada")
    public RecursoNoEncontradoException(String mensaje) {
        // Guarda el mensaje usando la clase padre, para poder leerlo después
        super(mensaje);
    }
}