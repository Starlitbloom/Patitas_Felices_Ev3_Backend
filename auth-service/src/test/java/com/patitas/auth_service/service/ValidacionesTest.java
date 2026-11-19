package com.patitas.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;


class ValidacionesTest {

    @Test
    void runValidoConPuntosYGuion() {
        assertDoesNotThrow(() -> Validaciones.validarRun("11.111.111-1"));
        assertDoesNotThrow(() -> Validaciones.validarRun("21456789-k")); // DV K en minúscula
    }

    @Test
    void runConDigitoVerificadorIncorrecto() {
        NegocioException e = assertThrows(NegocioException.class, () -> Validaciones.validarRun("11111111-2"));
        assertEquals("run", e.getCampo());
    }

    @Test
    void formateaElRun() {
        assertEquals("11111111-1", Validaciones.formatearRun("11.111.111-1"));
    }

    @Test
    void rechazaDominioDeCorreoNoPermitido() {
        NegocioException e = assertThrows(NegocioException.class, () -> Validaciones.validarCorreo("ana@hotmail.com"));
        assertEquals("correo", e.getCampo());
    }

    @Test
    void passwordDebeTenerEntre4y10Caracteres() {
        assertThrows(NegocioException.class, () -> Validaciones.validarPassword("123"));
        assertThrows(NegocioException.class, () -> Validaciones.validarPassword("12345678901"));
        assertDoesNotThrow(() -> Validaciones.validarPassword("1234"));
    }
}
