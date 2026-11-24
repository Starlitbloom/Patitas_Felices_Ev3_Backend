package com.patitas.servicio_clinico_service;

// Clase que arranca Spring Boot
import org.springframework.boot.SpringApplication;
// Anotación que activa la configuración automática de Spring Boot
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication: busca todas las clases del proyecto (controller, service, config...) y las configura
@SpringBootApplication
public class ServicioClinicoServiceApplication {

	// Método principal: es lo primero que se ejecuta al correr "mvnw spring-boot:run"
	public static void main(String[] args) {
		// Arranca el servidor Tomcat en el puerto 8084 y carga toda la aplicación
		SpringApplication.run(ServicioClinicoServiceApplication.class, args);
	}

}
