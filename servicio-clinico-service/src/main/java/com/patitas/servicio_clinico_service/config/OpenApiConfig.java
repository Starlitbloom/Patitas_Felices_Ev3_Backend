// Carpeta (paquete) donde vive este archivo: config
package com.patitas.servicio_clinico_service.config;

// Clases de Swagger/OpenAPI para describir la API
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

// @Bean y @Configuration de Spring
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Documentación Swagger: http://localhost:8084/swagger-ui.html (botón "Authorize" para pegar el JWT)
@Configuration
public class OpenApiConfig {

    // Crea la descripción de la API que muestra Swagger
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            // Título, versión y descripción que aparecen arriba en Swagger
            .info(new Info().title("Servicio Clinico Service - Patitas Felices").version("1.0").description("Catalogo de servicios que se pueden registrar en una atencion."))
            // Todos los endpoints usan el esquema de seguridad "bearerAuth"
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            // Define "bearerAuth": token JWT en el header Authorization (esto crea el botón Authorize)
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
