package com.patitas.report_service.config;

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
                .title("Patitas Felices - Report Service API")
                .version("1.0.0")
                .description("Microservicio de dashboard y reportes del panel admin. No guarda usuarios, mascotas "
                    + "ni citas: los CONSULTA por WebClient a auth-service, mascota-service, cita-service, "
                    + "contacto-service y notificacion-service, y calcula los indicadores. Si algún servicio está "
                    + "apagado, el reporte se genera igual y lo indica en 'serviciosNoDisponibles'. "
                    + "Para probar: login en auth-service (8081) con una cuenta ADMIN y pega el token en 'Authorize'."))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
