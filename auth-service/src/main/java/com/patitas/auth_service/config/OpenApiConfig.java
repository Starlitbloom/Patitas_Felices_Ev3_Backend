package com.patitas.auth_service.config;

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
                .title("Patitas Felices - Auth Service API")
                .version("1.0.0")
                .description("Microservicio de registro, inicio de sesión (JWT) y gestión de usuarios y roles "
                    + "de la Veterinaria San Marcos. Para probar endpoints protegidos: haz login, copia el "
                    + "token y pégalo en el botón 'Authorize'."))
            // Agrega el botón "Authorize" en Swagger para enviar el token JWT
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
