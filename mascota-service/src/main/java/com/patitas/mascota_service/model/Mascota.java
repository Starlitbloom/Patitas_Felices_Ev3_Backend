package com.patitas.mascota_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//Mascota (paciente) de la clinica. Tabla: mascota (la crea Hibernate al iniciar).
@Entity 
@Table (name="mascotas")
@Getter                      // Lombok crea todos los getters (getNombre(), isCompleto(), etc.)
@Setter                      // Lombok crea todos los setters (setNombre(), setCompleto(), etc.)
@NoArgsConstructor           // Constructor vacío: JPA lo necesita para crear objetos al leer la BD
@AllArgsConstructor          // Constructor con todos los campos
public class Mascota {
    @Id  // Marca este campo como la llave primaria de la tabla (identifica cada mascota)
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // MySQL asigna el id solo: 1, 2, 3... (AUTO_INCREMENT)
    private Long id;  // Número único de la mascota

    private String nombre;  // Nombre de la mascota (ej: Rocky)
    private String especie;      // Perro | Gato | Conejo | Ave
    private String raza;  // Raza (ej: Golden Retriever)
    private String edad;  // Edad escrita como texto (ej: "3 años")
    private String sexo;         // Macho | Hembra (null si aun no se sabe)
    private String peso;  // Peso escrito como texto (ej: "28 kg")
    private String nombreDueno;  // Nombre completo del dueño (sin ñ para evitar problemas en JSON y BD)
    private String correoDueno;  // correo del dueno: vincula la mascota con su usuario
    private boolean completo;    // true cuando ya tiene raza, sexo, peso e imagen
    private String imagen;  // Ruta de la foto (ej: /img/golden.jpg)
    private String estado;       // al-dia | tratamiento | pendiente
    private String descripcion;  // Texto libre sobre la mascota

/** Regla de negocio: una mascota está "completa" si tiene raza, sexo, peso e imagen. */
    public void recalcularCompleto() {  // Método que decide si la ficha está completa
        this.completo = tiene(raza) && tiene(sexo) && tiene(peso) && tiene(imagen);  // true solo si los 4 datos tienen texto
    }

    private static boolean tiene(String s) {  // Ayudante: revisa si un texto trae algo escrito
        return s != null && !s.isBlank();  // true si no es null y no está vacío ni son solo espacios
    }
}