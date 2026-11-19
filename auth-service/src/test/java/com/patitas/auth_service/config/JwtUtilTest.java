package com.patitas.auth_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.patitas.auth_service.model.Rol;
import com.patitas.auth_service.model.Usuario;

/** Pruebas de generación, verificación y expiración de los tokens JWT. */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private Usuario maria;

    @BeforeEach
    void preparar() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secreto", "PatitasFelices$2026#ClaveSecretaJWT!VeterinariaSanMarcos");
        ReflectionTestUtils.setField(jwtUtil, "expiracionMs", 1800000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiracionMs", 604800000L);

        maria = new Usuario();
        maria.setId(1L);
        maria.setNombre("Maria");
        maria.setCorreo("ma.calfileo@duoc.cl");
        maria.setRol(new Rol(1L, Rol.ADMIN, "Administrador", null));
    }

    @Test
    void tokenDeAccesoLlevaCorreoYRol() {
        String token = jwtUtil.generarToken(maria);

        assertTrue(jwtUtil.esValido(token, JwtUtil.TIPO_ACCESS));
        assertEquals("ma.calfileo@duoc.cl", jwtUtil.extraerCorreo(token));
        assertEquals("ADMIN", jwtUtil.extraerRol(token));
    }

    @Test
    void refreshTokenNoSirveComoTokenDeAcceso() {
        String refresh = jwtUtil.generarRefreshToken(maria);

        assertTrue(jwtUtil.esValido(refresh, JwtUtil.TIPO_REFRESH));
        assertFalse(jwtUtil.esValido(refresh, JwtUtil.TIPO_ACCESS));
    }

    @Test
    void tokenAlteradoEsRechazado() {
        String token = jwtUtil.generarToken(maria);
        String alterado = token.substring(0, token.length() - 4) + "XXXX";

        assertFalse(jwtUtil.esValido(alterado, JwtUtil.TIPO_ACCESS));
    }

    @Test
    void tokenExpiradoEsRechazado() {
        ReflectionTestUtils.setField(jwtUtil, "expiracionMs", -1000L); // Ya vencido al crearlo
        String token = jwtUtil.generarToken(maria);

        assertFalse(jwtUtil.esValido(token, JwtUtil.TIPO_ACCESS));
    }

    @Test
    void tokenFirmadoConOtraClaveEsRechazado() {
        JwtUtil otro = new JwtUtil();
        ReflectionTestUtils.setField(otro, "secreto", "OtraClaveSecretaDistintaDeLaDePatitasFelices2026!!");
        ReflectionTestUtils.setField(otro, "expiracionMs", 1800000L);
        String tokenFalso = otro.generarToken(maria);

        assertFalse(jwtUtil.esValido(tokenFalso, JwtUtil.TIPO_ACCESS));
    }
}
