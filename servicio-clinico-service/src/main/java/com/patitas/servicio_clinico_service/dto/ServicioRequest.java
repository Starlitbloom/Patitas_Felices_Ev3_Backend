// Carpeta (paquete) donde vive este archivo: dto
package com.patitas.servicio_clinico_service.dto;

// Tipo "lista" de Java
import java.util.List;

// Validaciones del JSON de entrada
import jakarta.validation.constraints.NotBlank;  // Texto obligatorio
import jakarta.validation.constraints.Size;      // Largo máximo

// DTO de ENTRADA: lo que manda el admin para crear o editar un servicio del catálogo
public record ServicioRequest(
    // Grupo: obligatorio, máximo 40 caracteres
    @NotBlank(message = "El grupo es obligatorio") @Size(max = 40, message = "Maximo 40 caracteres") String grupo,
    // Nombre: obligatorio, máximo 120 caracteres
    @NotBlank(message = "El nombre es obligatorio") @Size(max = 120, message = "Maximo 120 caracteres") String nombre,
    // Especies: opcional. Valores: Perro, Gato, Conejo, Ave. Vacío = todas las especies
    List<String> especies,
    // Activo: opcional (Boolean con mayúscula permite null). Si no viene, queda activo
    Boolean activo
) {}
