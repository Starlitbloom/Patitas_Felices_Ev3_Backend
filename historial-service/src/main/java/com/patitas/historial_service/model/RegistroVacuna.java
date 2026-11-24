// Carpeta (paquete) donde vive este archivo: model
package com.patitas.historial_service.model;

// Fecha sin hora (ej: 2026-01-15)
import java.time.LocalDate;

// Anotaciones de JPA: convierten la clase en una tabla de MySQL
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Anotaciones de Lombok: generan código automáticamente al compilar
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Una vacuna aplicada a una mascota. El estado (al-dia / proxima / atrasada) NO se guarda: se calcula en el service
@Entity                          // Esta clase es una tabla en la base de datos
@Table(name = "registro_vacuna") // Nombre de la tabla en MySQL
@Getter                          // Lombok crea todos los getters
@Setter                          // Lombok crea todos los setters
@NoArgsConstructor               // Constructor vacío (lo usa JPA)
@AllArgsConstructor              // Constructor con todos los campos
public class RegistroVacuna {

    @Id                                                   // Llave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // Id autoincremental
    private Long id;                                      // Número único del registro

    private Long mascotaId;             // Id de la mascota vacunada
    private String vacuna;              // Nombre de la vacuna (ej: "Antirrábica canina")
    private LocalDate fecha;            // Día en que se aplicó
    private LocalDate proximoRefuerzo;  // Día en que toca el siguiente refuerzo
}
