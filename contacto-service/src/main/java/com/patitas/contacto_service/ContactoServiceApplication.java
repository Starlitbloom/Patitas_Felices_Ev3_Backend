package com.patitas.contacto_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ContactoServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContactoServiceApplication.class, args);
        // http://localhost:8088/swagger-ui/index.html
        // Consola de Swagger para probar los endpoints
    }
}
