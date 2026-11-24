// Carpeta (paquete) donde vive este archivo: dto
package com.patitas.historial_service.dto;

// Fecha sin hora
import java.time.LocalDate;
// Tipo "lista" de Java
import java.util.List;

// Validaciones: si no se cumplen, el ApiExceptionHandler responde 400 con el mensaje
import jakarta.validation.constraints.NotBlank;       // Texto obligatorio (no vacío ni solo espacios)
import jakarta.validation.constraints.NotNull;        // Valor obligatorio (no null)
import jakarta.validation.constraints.PastOrPresent;  // Fecha de hoy o anterior
import jakarta.validation.constraints.Size;           // Largo máximo del texto

// DTO de ENTRADA: lo que el frontend manda en el JSON para crear o editar una atención.
// "record" = clase de solo datos; Java crea solo el constructor y los métodos fecha(), motivo(), etc.
public record AtencionRequest(
    // Fecha de la atención: obligatoria y no puede ser futura
    @NotNull(message = "La fecha es obligatoria") @PastOrPresent(message = "La fecha no puede ser futura") LocalDate fecha,
    // Motivo de la consulta: obligatorio, máximo 100 caracteres
    @NotBlank(message = "El motivo de consulta es obligatorio") @Size(max = 100, message = "Maximo 100 caracteres") String motivo,
    // Diagnóstico: obligatorio, máximo 300 caracteres
    @NotBlank(message = "El diagnostico es obligatorio") @Size(max = 300, message = "Maximo 300 caracteres") String diagnostico,
    // Tratamiento: obligatorio, máximo 300 caracteres
    @NotBlank(message = "El tratamiento es obligatorio") @Size(max = 300, message = "Maximo 300 caracteres") String tratamiento,
    // Veterinario: obligatorio, máximo 100 caracteres
    @NotBlank(message = "El veterinario es obligatorio") @Size(max = 100, message = "Maximo 100 caracteres") String veterinario,
    // Servicios realizados: opcional; cada elemento no puede venir vacío
    List<@NotBlank(message = "Servicio vacio") @Size(max = 100, message = "Maximo 100 caracteres") String> servicios
) {}
