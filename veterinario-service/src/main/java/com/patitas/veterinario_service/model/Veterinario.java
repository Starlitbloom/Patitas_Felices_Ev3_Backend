
package com.patitas.veterinario_service.model;

import java.util.ArrayList;
import java.util.List;

// Importaciones para el mapeo de entidades y relaciones con MySQL.
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

// Importaciones para validar los datos ingresados.
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lombok permite generar automaticamente getters,
// setters y constructores, evitando codigo repetitivo.
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa a un veterinario de Patitas Felices.
 *
 * Almacena la informacion profesional y de contacto
 * de los veterinarios, incluyendo sus especialidades,
 * especies atendidas, experiencia y disponibilidad.
 *
 * Los datos se guardan en MySQL mediante Spring Data JPA.
 */
@Data
@Entity
@Table(name = "veterinarios")
@NoArgsConstructor
@AllArgsConstructor
public class Veterinario {

    /**
     * Identificador unico del veterinario.
     *
     * Es la clave primaria de la tabla veterinarios.
     * MySQL genera automaticamente su valor.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre completo del veterinario.
     *
     * Es obligatorio y permite un maximo
     * de 100 caracteres en la base de datos.
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Cargo que desempena el profesional
     * dentro de la veterinaria.
     *
     * Ejemplo: Medico Veterinario.
     */
    @NotBlank(message = "El cargo es obligatorio")
    @Column(nullable = false, length = 50)
    private String cargo;

    /**
     * Titulo profesional del veterinario.
     *
     * Permite mostrar su formacion academica
     * en la pagina web.
     */
    @NotBlank(message = "El titulo es obligatorio")
    @Column(nullable = false, length = 100)
    private String titulo;

    /**
     * Area de especializacion del veterinario.
     *
     * Ejemplo: Medicina general, dermatologia
     * o cirugia veterinaria.
     */
    @NotBlank(message = "El area es obligatoria")
    @Column(nullable = false, length = 150)
    private String area;

    /**
     * Lista de especies que atiende el veterinario.
     *
     * Se almacena en la tabla veterinario_especies,
     * relacionada mediante veterinario_id.
     *
     * @ElementCollection permite guardar una lista
     * de valores simples sin crear otra entidad Java.
     *
     * FetchType.EAGER carga las especies junto
     * con la informacion del veterinario.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "veterinario_especies",
        joinColumns = @JoinColumn(name = "veterinario_id")
    )
    @OrderColumn(name = "orden")
    @Column(name = "especie", length = 50)
    private List<String> especies = new ArrayList<>();

    /**
     * Cantidad de anos de experiencia profesional.
     *
     * Es un dato obligatorio y no puede
     * contener valores negativos.
     */
    @NotNull(message = "La experiencia es obligatoria")
    @Min(value = 0, message = "La experiencia no puede ser negativa")
    @Column(nullable = false)
    private Integer experiencia;

    /**
     * Descripcion del perfil profesional.
     *
     * Se utiliza para presentar al veterinario
     * y su experiencia en la pagina web.
     */
    @NotBlank(message = "La descripcion es obligatoria")
    @Column(nullable = false, length = 2000)
    private String descripcion;

    /**
     * Lista de dias disponibles para atencion.
     *
     * Los dias se guardan en la tabla veterinario_dias,
     * relacionada con el identificador del veterinario.
     *
     * La columna orden conserva la posicion
     * de los elementos dentro de la lista.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "veterinario_dias",
        joinColumns = @JoinColumn(name = "veterinario_id")
    )
    @OrderColumn(name = "orden")
    @Column(name = "dia", length = 30)
    private List<String> dias = new ArrayList<>();

    /**
     * Horario habitual de atencion.
     *
     * Ejemplo: 09:00 - 18:00.
     *
     * Este campo permite mostrar la disponibilidad
     * general del profesional en la pagina web.
     */
    @NotBlank(message = "El horario es obligatorio")
    @Column(nullable = false, length = 50)
    private String horario;

    /**
     * Ruta o URL de la imagen del veterinario.
     *
     * Se utiliza en el frontend React para
     * mostrar su fotografia o imagen de perfil.
     */
    @NotBlank(message = "La imagen es obligatoria")
    @Column(nullable = false, length = 500)
    private String imagen;

    /**
     * Correo electronico de contacto.
     *
     * Debe tener un formato de correo valido.
     * Ademas, la base de datos no permite
     * registrar dos veterinarios con el mismo correo.
     */
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato valido")
    @Column(nullable = false, unique = true, length = 150)
    private String correo;
}
