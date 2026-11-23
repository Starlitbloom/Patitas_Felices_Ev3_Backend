
package com.patitas.cita_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de arranque del microservicio de citas
 * veterinarias de Patitas Felices.
 *
 * Comprueba que Spring Boot pueda iniciar
 * correctamente utilizando application.properties.
 *
 * Durante la prueba se verifica:
 *
 * - La inicializacion del contexto de Spring Boot.
 * - La configuracion de los componentes del servicio.
 * - La conexion de Spring Data JPA con MySQL.
 *
 * Requisitos para ejecutar la prueba:
 *
 * - MySQL encendido en XAMPP.
 * - Base de datos cita_db creada.
 * - Variable JWT_SECRET configurada.
 *
 * No utiliza H2 ni una base de datos simulada.
 */
@SpringBootTest
class CitaServiceApplicationTests {

    /**
     * Comprueba que la aplicacion se inicia
     * correctamente sin errores.
     *
     * Si falla la configuracion del contexto
     * o la conexion con MySQL, esta prueba falla.
     */
    @Test
    void contextLoads() {

        // Spring Boot comprueba automaticamente
        // el arranque del contexto de la aplicacion.
    }
}
