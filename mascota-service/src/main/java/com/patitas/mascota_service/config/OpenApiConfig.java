// Carpeta (paquete) donde vive este archivo: config
package com.patitas.mascota_service.config;

// Clases de la librería Swagger/OpenAPI: sirven para describir la documentación de la API
// Components: contenedor donde se registran piezas reutilizables (aquí, el esquema de seguridad)
import io.swagger.v3.oas.models.Components;
// OpenAPI: el objeto principal que representa toda la documentación
import io.swagger.v3.oas.models.OpenAPI;
// Info: título, versión y descripción que se muestran arriba en Swagger
import io.swagger.v3.oas.models.info.Info;
// SecurityRequirement: dice "estos endpoints piden este tipo de seguridad"
import io.swagger.v3.oas.models.security.SecurityRequirement;
// SecurityScheme: define CÓMO se envía la seguridad (aquí, con un token Bearer)
import io.swagger.v3.oas.models.security.SecurityScheme;

// @Bean: marca un método cuyo resultado Spring guarda y usa
import org.springframework.context.annotation.Bean;
// @Configuration: marca la clase como "de configuración"; Spring la lee al arrancar
import org.springframework.context.annotation.Configuration;

/** Documentacion Swagger: http://localhost:PUERTO/swagger-ui.html (boton "Authorize" para pegar el JWT). */
// Le dice a Spring "esta clase configura algo; léela al iniciar"
@Configuration
// Clase que personaliza cómo se ve la documentación Swagger de este microservicio
public class OpenApiConfig {

    // Spring ejecuta este método al iniciar y guarda el resultado; springdoc lo usa para armar Swagger
    @Bean
    // Método que construye la configuración de la documentación y la devuelve
    public OpenAPI openAPI() {
        // Crea el objeto de documentación y lo va armando paso a paso
        return new OpenAPI()
            // Info que se ve arriba en Swagger: título, versión 1.0 y descripción de qué hace este servicio
            .info(new Info().title("Mascota Service - Patitas Felices").version("1.0").description("CRUD de mascotas (pacientes) y solicitudes de cambio de datos."))
            // Indica que los endpoints usan la seguridad llamada "bearerAuth" (la definida abajo); esto hace aparecer el candado
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            // Registra la seguridad "bearerAuth"
            .components(new Components().addSecuritySchemes("bearerAuth",
                // La define: tipo HTTP, esquema "bearer" (token en la cabecera Authorization) y formato JWT
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}