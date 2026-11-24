// Carpeta (paquete) donde vive este archivo: dto
package com.patitas.servicio_clinico_service.dto;

// Tipo "lista" de Java
import java.util.List;

// La entidad que se convierte a respuesta
import com.patitas.servicio_clinico_service.model.ServicioClinico;

// DTO de SALIDA: lo que la API devuelve por cada servicio
public record ServicioResponse(
    Long id,               // Id del servicio
    String grupo,          // Grupo (Cirugía, Exámenes...)
    String nombre,         // Nombre del servicio
    boolean activo,        // Si está activo
    List<String> especies  // Especies a las que aplica (vacía = todas)
) {
    // Convierte la entidad (BD) en la respuesta (JSON)
    public static ServicioResponse de(ServicioClinico s) {
        // Copia campo por campo; List.copyOf crea una copia de solo lectura de las especies
        return new ServicioResponse(s.getId(), s.getGrupo(), s.getNombre(), s.isActivo(), List.copyOf(s.getEspecies()));
    }
}
