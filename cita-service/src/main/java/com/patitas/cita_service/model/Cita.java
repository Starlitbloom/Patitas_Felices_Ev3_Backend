
package com.patitas.cita_service.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una cita veterinaria
 * en el sistema Patitas Felices.
 *
 * Almacena los datos del cliente, la mascota,
 * el veterinario y la atención solicitada.
 *
 * Utiliza Spring Data JPA para guardar
 * las citas en la base de datos MySQL.
 *
 * Los nombres de los atributos mantienen
 * compatibilidad con el frontend React.
 */
@Data
@Entity
@Table(name = "citas")
@NoArgsConstructor
@AllArgsConstructor
public class Cita {

    /**
     * Identificador unico de la cita.
     *
     * MySQL genera automaticamente este ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==========================================
    // DATOS DEL USUARIO
    // ==========================================

    /**
     * Correo de la cuenta que solicita la cita.
     *
     * El backend debera obtenerlo del token JWT
     * para impedir reservar a nombre de otra cuenta.
     */
    @Email(message = "El correo del usuario no es valido")
    @Column(nullable = false, length = 150)
    private String correoUsuario;

    /**
     * Nombre del usuario que inicio sesion.
     */
    @Column(length = 150)
    private String nombreUsuario;

    // ==========================================
    // DATOS DEL PROPIETARIO
    // ==========================================

    /**
     * Nombre del dueño de la mascota.
     */
    @NotBlank(message = "El nombre del dueño es obligatorio")
    @Column(nullable = false, length = 150)
    private String dueno;

    /**
     * Nombre del propietario utilizado
     * por el frontend para mostrar las citas.
     */
    @Column(length = 150)
    private String propietario;

    /**
     * Correo de contacto del propietario.
     */
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato valido")
    @Column(nullable = false, length = 150)
    private String correo;

    /**
     * Telefono de contacto del propietario.
     */
    @NotBlank(message = "El telefono es obligatorio")
    @Column(nullable = false, length = 30)
    private String telefono;

    // ==========================================
    // DATOS DE LA MASCOTA
    // ==========================================

    /**
     * Nombre de la mascota que recibira atencion.
     */
    @NotBlank(message = "El nombre de la mascota es obligatorio")
    @Column(nullable = false, length = 100)
    private String mascota;

    /**
     * Tipo o especie de la mascota.
     *
     * Ejemplo: Perro, Gato u Otro.
     */
    @NotBlank(message = "El tipo de mascota es obligatorio")
    @Column(nullable = false, length = 50)
    private String tipoMascota;

    /**
     * Especie especificada cuando el usuario
     * selecciona la opcion "Otro".
     */
    @Column(length = 80)
    private String otraEspecie;

    /**
     * Edad de la mascota expresada en años.
     *
     * Se utiliza Double porque el formulario
     * tambien permite registrar mascotas
     * que tienen menos de un año.
     */
    @NotNull(message = "La edad de la mascota es obligatoria")
    @DecimalMin(
        value = "0.0",
        inclusive = false,
        message = "La edad debe ser mayor que cero"
    )
    @DecimalMax(
        value = "30.0",
        message = "La edad no puede superar los 30 años"
    )
    @Column(nullable = false)
    private Double edadMascota;

    // ==========================================
    // DATOS DEL VETERINARIO
    // ==========================================

    /**
     * Identificador del veterinario seleccionado.
     *
     * Este ID corresponde a un registro
     * del microservicio veterinario-service.
     *
     * No se utiliza una relacion JPA directa
     * porque los microservicios tienen
     * bases de datos independientes.
     */
    @Column(name = "veterinario_id")
    private Long veterinarioId;

    /**
     * Nombre del veterinario seleccionado.
     *
     * Permite mostrarlo en el historial de citas.
     */
    @Column(length = 150)
    private String veterinarioNombre;

    // ==========================================
    // INFORMACION DE LA CITA
    // ==========================================

    /**
     * Servicio veterinario solicitado.
     *
     * Ejemplo: Consulta general o vacunacion.
     */
    @NotBlank(message = "El servicio es obligatorio")
    @Column(nullable = false, length = 150)
    private String servicio;

    /**
     * Fecha solicitada para la atencion.
     *
     * LocalDate permite trabajar con fechas
     * sin incluir la hora.
     *
     * Formato JSON: AAAA-MM-DD.
     */
    @NotNull(message = "La fecha es obligatoria")
    @Column(nullable = false)
    private LocalDate fecha;

    /**
     * Hora de inicio de la cita.
     *
     * LocalTime permite gestionar los horarios
     * sin depender de una fecha determinada.
     *
     * Formato JSON: HH:mm.
     */
    @NotNull(message = "La hora es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    @Column(nullable = false)
    private LocalTime hora;

    /**
     * Motivo de la consulta veterinaria.
     */
    @NotBlank(message = "El motivo de la cita es obligatorio")
    @Column(nullable = false, length = 2000)
    private String motivo;

    /**
     * Duracion estimada de la atencion en minutos.
     *
     * Este valor sera calculado por el servicio
     * segun el tipo de atencion solicitada.
     */
    @Min(
        value = 1,
        message = "La duracion debe ser mayor que cero"
    )
    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    /**
     * Indica si la clinica debe confirmar
     * el profesional que realizara la atencion.
     */
    @Column(nullable = false)
    private Boolean profesionalPorConfirmar = false;

    // ==========================================
    // ESTADO Y FECHA DE REGISTRO
    // ==========================================

    /**
     * Estado actual de la cita veterinaria.
     *
     * Los estados permitidos son:
     * - Pendiente
     * - Confirmada
     * - Completada
     * - Cancelada
     *
     * Una cita nueva comienza como Pendiente.
     */
    @NotBlank(message = "El estado es obligatorio")
    @Pattern(
        regexp = "Pendiente|Confirmada|Completada|Cancelada",
        message = "El estado de la cita no es valido"
    )
    @Column(nullable = false, length = 20)
    private String estado = "Pendiente";

    /**
     * Fecha y hora en que se registro la cita.
     *
     * Instant almacena un instante temporal
     * que puede serializarse en formato ISO 8601.
     */
    @Column(nullable = false, updatable = false)
    private Instant fechaRegistro;

    /**
     * Se ejecuta automaticamente antes
     * de guardar una cita nueva en MySQL.
     *
     * Asigna el estado inicial y registra
     * la fecha de creacion.
     */
    @PrePersist
    public void antesDeGuardar() {

        estado = "Pendiente";
        fechaRegistro = Instant.now();

        if (profesionalPorConfirmar == null) {
            profesionalPorConfirmar = false;
        }
    }
}
