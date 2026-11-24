// Carpeta (paquete) donde vive este archivo: dto
package com.patitas.historial_service.dto;

// Fecha sin hora
import java.time.LocalDate;

// Validaciones del JSON de entrada
import jakarta.validation.constraints.NotBlank;       // Texto obligatorio
import jakarta.validation.constraints.NotNull;        // Valor obligatorio
import jakarta.validation.constraints.PastOrPresent;  // Fecha de hoy o anterior
import jakarta.validation.constraints.Size;           // Largo máximo

// DTO de ENTRADA: datos para registrar o editar una vacuna
public record VacunaRequest(
    // Nombre de la vacuna: obligatorio, máximo 100 caracteres
    @NotBlank(message = "La vacuna es obligatoria") @Size(max = 100, message = "Maximo 100 caracteres") String vacuna,
    // Fecha en que se aplicó: obligatoria y no futura
    @NotNull(message = "La fecha de aplicacion es obligatoria") @PastOrPresent(message = "La fecha no puede ser futura") LocalDate fecha,
    // Fecha del próximo refuerzo: obligatoria (el service revisa que sea posterior a la aplicación)
    @NotNull(message = "El proximo refuerzo es obligatorio") LocalDate proximoRefuerzo
) {}
