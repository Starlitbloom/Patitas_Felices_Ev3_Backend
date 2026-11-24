// Carpeta (paquete) donde vive este archivo: model
package com.patitas.servicio_clinico_service.model;

// Lista que se puede modificar
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

// Servicio del catálogo de la clínica (cirugía, desparasitación, exámenes...). Tabla: servicio_clinico
@Entity                            // Esta clase es una tabla en la base de datos
@Table(name = "servicio_clinico")  // Nombre de la tabla en MySQL
@Getter                            // Lombok crea los getters (getNombre(), isActivo()...)
@Setter                            // Lombok crea los setters (setNombre(), setActivo()...)
@NoArgsConstructor                 // Constructor vacío: JPA lo necesita
@AllArgsConstructor                // Constructor con todos los campos
public class ServicioClinico {

    @Id                                                   // Llave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // MySQL asigna el id solo (AUTO_INCREMENT)
    private Long id;                                      // Número único del servicio

    private String grupo;           // Grupo al que pertenece (Cirugía, Desparasitación, Exámenes, Otros)
    private String nombre;          // Nombre del servicio (único en el catálogo)
    private boolean activo = true;  // Si es false, no aparece en el listado (se "apaga" sin borrarlo)

    // Especies a las que se ofrece. Lista vacía = sirve para todas. Se guarda en una tabla aparte
    @ElementCollection(fetch = FetchType.EAGER)                      // Lista de textos simples; se carga junto con el servicio
    @CollectionTable(name = "servicio_clinico_especie",              // Nombre de la tabla aparte
                     joinColumns = @JoinColumn(name = "servicio_id")) // Columna que apunta al servicio
    @Column(name = "especie")                                        // Columna donde va cada especie
    private List<String> especies = new ArrayList<>();               // Empieza vacía para no tener null

    // Regla de negocio: sin especies indicadas, el servicio es para todas
    public boolean sirveParaEspecie(String especie) {
        // true si la lista está vacía, o si alguna especie coincide (sin importar mayúsculas)
        return especies.isEmpty() || especies.stream().anyMatch(e -> e.equalsIgnoreCase(especie));
    }
}
