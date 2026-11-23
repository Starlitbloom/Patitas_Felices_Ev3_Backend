
package com.patitas.veterinario_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Pruebas de arranque del microservicio de veterinarios.
 *
 * Comprueba que Spring Boot puede iniciar correctamente
 * utilizando la configuracion de application.properties.
 *
 * Durante la prueba se verifica la inicializacion
 * del contexto de Spring y la conexion JPA con MySQL.
 *
 * Para ejecutarla se necesita:
 * - MySQL activo en XAMPP.
 * - Base de datos veterinario_db creada.
 * - Variable de entorno JWT_SECRET configurada.
 */
@SpringBootTest
class VeterinarioServiceApplicationTests {

    /**
     * Verifica que el contexto de Spring Boot
     * se inicia sin errores.
     *
     * Si alguna dependencia o la conexion
     * con MySQL falla, esta prueba no se aprueba.
     */
    @Test
    void contextLoads() {
        // La prueba se aprueba si Spring inicia correctamente.
    }
}
