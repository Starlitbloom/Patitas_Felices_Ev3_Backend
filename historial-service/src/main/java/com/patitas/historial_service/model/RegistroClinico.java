// Carpeta (paquete) donde vive este archivo: model
package com.patitas.historial_service.model;

// Fecha sin hora (ej: 2026-08-12)
import java.time.LocalDate;
// Lista que se puede modificar (agregar/quitar elementos)
import java.util.ArrayList;
// Tipo "lista" de Java
import java.util.List;

// Anotaciones de JPA: convierten la clase en una tabla de MySQL
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

// Anotaciones de Lombok: generan código automáticamente al compilar
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Una atención clínica de una mascota (una fila del historial). Tabla: registro_clinico
@Entity                            // Esta clase es una tabla en la base de datos
@Table(name = "registro_clinico")  // Nombre de la tabla en MySQL
@Getter                            // Lombok crea todos los getters (getFecha(), getMotivo()...)
@Setter                            // Lombok crea todos los setters (setFecha(), setMotivo()...)
@NoArgsConstructor                 // Constructor vacío: JPA lo necesita para crear objetos al leer la BD
@AllArgsConstructor                // Constructor con todos los campos
public class RegistroClinico {

    @Id                                                   // Llave primaria de la tabla
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // MySQL asigna el id solo: 1, 2, 3... (AUTO_INCREMENT)
    private Long id;                                      // Número único de la atención

    private Long mascotaId;      // Id de la mascota en mascota-service (une la atención con su mascota)
    private LocalDate fecha;     // Día de la atención
    private String motivo;       // Motivo de la consulta (ej: "Control anual")
    private String diagnostico;  // Lo que encontró el veterinario
    private String tratamiento;  // Lo que se indicó (medicamentos, dieta, etc.)
    private String veterinario;  // Nombre del veterinario que atendió

    // Servicios realizados en la atención (cirugía, exámenes...). Se guardan en una tabla aparte
    @ElementCollection(fetch = FetchType.EAGER)                     // Lista de textos simples; EAGER = se carga junto con la atención
    @CollectionTable(name = "registro_clinico_servicio",            // Nombre de la tabla aparte
                     joinColumns = @JoinColumn(name = "registro_id")) // Columna que apunta a la atención
    @Column(name = "servicio")                                      // Columna donde va cada servicio
    private List<String> servicios = new ArrayList<>();             // Empieza vacía para no tener null
}
