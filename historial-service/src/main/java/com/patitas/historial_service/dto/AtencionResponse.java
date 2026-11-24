// Carpeta (paquete) donde vive este archivo: dto
package com.patitas.historial_service.dto;

// Fecha sin hora
import java.time.LocalDate;
// Tipo "lista" de Java
import java.util.List;

// La entidad que se convierte a respuesta
import com.patitas.historial_service.model.RegistroClinico;

// DTO de SALIDA: lo que la API devuelve en el JSON cuando se pide una atención
public record AtencionResponse(
    Long id,               // Id de la atención
    Long mascotaId,        // Mascota a la que pertenece
    LocalDate fecha,       // Día de la atención
    String motivo,         // Motivo de la consulta
    String diagnostico,    // Diagnóstico
    String tratamiento,    // Tratamiento indicado
    String veterinario,    // Quién atendió
    List<String> servicios // Servicios realizados
) {
    // Convierte la entidad (lo que está en la BD) en la respuesta (lo que sale por el JSON)
    public static AtencionResponse de(RegistroClinico r) {
        // Copia campo por campo; List.copyOf hace una copia de solo lectura de la lista de servicios
        return new AtencionResponse(r.getId(), r.getMascotaId(), r.getFecha(), r.getMotivo(), r.getDiagnostico(),
            r.getTratamiento(), r.getVeterinario(), List.copyOf(r.getServicios()));
    }
}
