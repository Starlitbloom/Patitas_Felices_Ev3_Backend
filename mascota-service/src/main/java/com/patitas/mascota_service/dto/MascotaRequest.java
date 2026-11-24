package com.patitas.mascota_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para crear o actualizar una mascota. Las validaciones responden 400 con el detalle del campo. */
public record MascotaRequest(
    @NotBlank(message = "El nombre es obligatorio")  // No puede ser null, vacío ni solo espacios
    @Size(max = 60, message = "Maximo 60 caracteres")  // Largo máximo de 60 letras
    String nombre,  // Nombre de la mascota (ej: Rocky)

    @NotBlank(message = "La especie es obligatoria")  // Es obligatoria
    @Pattern(regexp = "Perro|Gato|Conejo|Ave", message = "Especie debe ser Perro, Gato, Conejo o Ave")  // Solo acepta una de estas 4 palabras exactas
    String especie,  // Especie de la mascota

    @Size(max = 60, message = "Maximo 60 caracteres")  // Máximo 60 caracteres (puede venir vacía: no tiene @NotBlank)
    String raza,  // Raza (ej: Golden Retriever)

    @Size(max = 30, message = "Maximo 30 caracteres")  // Máximo 30 caracteres
    String edad,  // Edad como texto (ej: "3 años")

    @Pattern(regexp = "Macho|Hembra", message = "Sexo debe ser Macho o Hembra")  // Si viene, debe ser Macho o Hembra
    String sexo,  // Sexo (puede ser null si aún no se sabe)

    @Size(max = 20, message = "Maximo 20 caracteres")  // Máximo 20 caracteres
    String peso,  // Peso como texto (ej: "28 kg")

    @NotBlank(message = "El nombre del dueno es obligatorio")  // Es obligatorio
    @Size(max = 100, message = "Maximo 100 caracteres")  // Máximo 100 caracteres
    String nombreDueno,  // Nombre completo del dueño

    @NotBlank(message = "El correo del dueno es obligatorio")  // Es obligatorio
    @Email(message = "Correo invalido")  // Debe tener formato de correo (con @ y dominio)
    String correoDueno,  // Correo del dueño: lo vincula con su usuario

    @Size(max = 1000000, message = "La imagen es demasiado grande")  // Máximo 1000000 caracteres
    String imagen,  // Ruta de la foto (ej: /img/golden.jpg)

    @Pattern(regexp = "al-dia|tratamiento|pendiente",
    message = "Estado debe ser al-dia, tratamiento o pendiente")  // Solo uno de estos 3 estados
    String estado,  // Estado de salud de la mascota

    @Size(max = 500, message = "Maximo 500 caracteres")  // Máximo 500 caracteres
    String descripcion  // Texto libre sobre la mascota
) {}  // Cierra el record. Las llaves vacías porque no necesita código adentro


