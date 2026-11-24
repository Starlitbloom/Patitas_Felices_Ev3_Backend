// Carpeta (paquete) donde vive este archivo: dto
package com.patitas.historial_service.dto;

// Fecha sin hora
import java.time.LocalDate;

// DTO de SALIDA: lo que devuelve la API por cada vacuna
public record VacunaResponse(
    Long id,                    // Id del registro de vacuna
    Long mascotaId,             // Mascota vacunada
    String vacuna,              // Nombre de la vacuna
    LocalDate fecha,            // Fecha de aplicación
    LocalDate proximoRefuerzo,  // Fecha del próximo refuerzo
    String estado               // al-dia | proxima | atrasada (lo calcula HistorialService, no se guarda en la BD)
) {}
