package com.patitas.contacto_service.config;

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
                .title("Patitas Felices - Contacto Service API")
                .version("1.0.0")
                .description("Microservicio de mensajes del formulario de contacto de la Veterinaria San Marcos. "
                    + "Cualquier visitante puede enviar un mensaje; el personal (ADMIN / RECEPCIONISTA) los revisa "
                    + "y responde desde su bandeja. Para endpoints protegidos: haz login en auth-service (puerto 8081), "
                    + "copia el token y pégalo en 'Authorize'."))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
