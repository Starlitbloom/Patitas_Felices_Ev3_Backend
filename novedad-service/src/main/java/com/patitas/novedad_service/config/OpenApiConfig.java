
package com.patitas.novedad_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI novedadOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Novedades - Patitas Felices")
                        .version("1.0")
                        .description(
                            "Microservicio para gestionar las noticias " +
                            "y novedades de Veterinaria Patitas Felices."
                        ));
    }
}
