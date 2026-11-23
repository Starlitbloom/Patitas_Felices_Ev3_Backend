package com.patitas.mascota_service.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cambios que el dueno propone (solo los campos que quiere modificar). */
public record SolicitudRequest(  // DTO de ENTRADA: el JSON que envía el dueño al pedir un cambio en su mascota
    @Size(max = 60, message = "Maximo 60 caracteres") String raza,  // Raza nueva propuesta: máximo 60 caracteres. Puede venir vacía (no hay @NotBlank)
    @Pattern(regexp = "Macho|Hembra", message = "Sexo debe ser Macho o Hembra") String sexo,  // Sexo nuevo propuesto: si viene, solo puede ser Macho o Hembra
    @Size(max = 20, message = "Maximo 20 caracteres") String peso,  // Peso nuevo propuesto (ej: "7 kg"): máximo 20 caracteres
    @Size(max = 255, message = "Maximo 255 caracteres") String imagen,  // Ruta de la imagen nueva propuesta: máximo 255 caracteres
    @Size(max = 500, message = "Maximo 500 caracteres") String descripcion  // Descripción nueva propuesta: máximo 500 caracteres
) {}  // Cierra el record; las llaves quedan vacías porque no necesita código adentro