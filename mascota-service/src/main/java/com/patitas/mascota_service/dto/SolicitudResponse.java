package com.patitas.mascota_service.dto;

import java.time.LocalDateTime;

import com.patitas.mascota_service.model.SolicitudCambio;

public record SolicitudResponse(  // DTO de SALIDA: el JSON que se devuelve cuando consultas una solicitud de cambio
    Long id, Long mascotaId, String correoSolicitante, String raza, String sexo, String peso, String imagen,  // Campos: id de la solicitud, id de la mascota, correo de quien la pidió, y raza, sexo, peso e imagen propuestos
    String descripcion, String estado, LocalDateTime fechaSolicitud, LocalDateTime fechaResolucion  // Campos: descripción propuesta, estado (pendiente/aprobada/rechazada), fecha en que se pidió y fecha en que se resolvió
) {  // Abre el cuerpo del record, donde va el método convertidor
    public static SolicitudResponse de(SolicitudCambio s) {  // Convertidor: recibe una SolicitudCambio (de la BD) y devuelve su versión para enviar. "static" = se usa sin crear objeto
        return new SolicitudResponse(s.getId(), s.getMascotaId(), s.getCorreoSolicitante(), s.getRaza(), s.getSexo(),  // Crea la respuesta copiando id, id de mascota, correo del solicitante, raza y sexo
            s.getPeso(), s.getImagen(), s.getDescripcion(), s.getEstado(), s.getFechaSolicitud(), s.getFechaResolucion());  // Sigue con peso, imagen, descripción, estado y las dos fechas (mismo orden que los campos de arriba); el paréntesis cierra la creación
    }  // Cierra el método de
}  // Cierra el record
