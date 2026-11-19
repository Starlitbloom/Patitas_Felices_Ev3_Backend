package com.patitas.contacto_service.service;

import org.springframework.http.HttpStatus;

/**
 * Error de una regla de negocio (correo repetido, RUN inválido, etc.).
 * Lleva el campo del formulario con problema y el código HTTP a responder.
 * Lo atrapa config/ManejoErrores y lo transforma en JSON.
 */
public class NegocioException extends RuntimeException {
    private final String campo;
    private final HttpStatus status;

    public NegocioException(String campo, String mensaje) {
        this(campo, mensaje, HttpStatus.BAD_REQUEST);
    }

    public NegocioException(String campo, String mensaje, HttpStatus status) {
        super(mensaje);
        this.campo = campo;
        this.status = status;
    }

    public String getCampo() { return campo; }
    public HttpStatus getStatus() { return status; }
}