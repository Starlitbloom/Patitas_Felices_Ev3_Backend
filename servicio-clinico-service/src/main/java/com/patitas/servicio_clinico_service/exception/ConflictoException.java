// Carpeta (paquete) donde vive este archivo: exception
package com.patitas.servicio_clinico_service.exception;

// Error propio: se lanza cuando algo choca con una regla de negocio. El ApiExceptionHandler lo convierte en 409
public class ConflictoException extends RuntimeException {  // RuntimeException = no obliga a usar try/catch

    // Constructor: recibe el mensaje que verá el usuario
    public ConflictoException(String mensaje) {
        // Le pasa el mensaje a la clase padre para poder leerlo después con getMessage()
        super(mensaje);
    }
}
