package com.patitas.notificacion_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
            .info(new Info()
                .title("Patitas Felices - Notificacion Service API")
                .version("1.0.0")
                .description("Microservicio de avisos para los usuarios (la campanita del header). "
                    + "Los demás microservicios (contacto, mascotas, citas) le piden por WebClient que cree "
                    + "una notificación cuando pasa algo importante. Para probar: haz login en auth-service "
                    + "(puerto 8081), copia el token y pégalo en 'Authorize'."))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
