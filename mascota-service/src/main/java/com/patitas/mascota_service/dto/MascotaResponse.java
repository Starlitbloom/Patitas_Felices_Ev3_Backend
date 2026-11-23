package com.patitas.mascota_service.dto;

import com.patitas.mascota_service.model.Mascota;

public record MascotaResponse(  // DTO de SALIDA: los datos que el servicio devuelve en el JSON al frontend/Postman
    
    Long id, String nombre, String especie, String raza, String edad, String sexo, String peso,  // Campos: id, nombre, especie, raza, edad, sexo y peso
    String nombreDueno, String correoDueno, boolean completo, String imagen, String estado, String descripcion  // Campos: dueño, correo, si la ficha está completa, imagen, estado y descripción
) {  
    // Abre el cuerpo del record, donde se puede agregar un método extra
    public static MascotaResponse de(Mascota m) {  // Convertidor: recibe una Mascota (de la BD) y devuelve su versión para enviar. "static" = se usa sin crear objeto
        return new MascotaResponse(m.getId(), m.getNombre(), m.getEspecie(), m.getRaza(), m.getEdad(), m.getSexo(),  // Crea la respuesta copiando id, nombre, especie, raza, edad y sexo de la mascota
            m.getPeso(), m.getNombreDueno(), m.getCorreoDueno(), m.isCompleto(), m.getImagen(), m.getEstado(),  // Sigue con peso, dueño, correo, si está completa, imagen y estado (mismo orden que los campos de arriba)
            m.getDescripcion());  // Termina con la descripción; el paréntesis cierra la creación del objeto
    }  // Cierra el método de
}  // Cierra el record
