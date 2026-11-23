package com.patitas.mascota_service.exception;
//Se lanza cuando la operacion choca con una regla de negocio (responde 409).
// Clase de error PROPIA. "extends RuntimeException" = es una excepción que no obliga a escribir try/catch en cada método
public class ConflictoException extends RuntimeException {
    // Constructor: recibe el mensaje que explica qué pasó (ej: "La mascota ya tiene una solicitud de cambio pendiente")
    public ConflictoException(String mensaje) {
        // Pasa el mensaje a la clase padre (RuntimeException), que lo guarda; después se obtiene con ex.getMessage()
        super(mensaje);
    }


}