package com.patitas.mascota_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** El dueno propone cambios a una mascota ya completa; el personal los aprueba o rechaza. */
@Entity
@Table(name = "solicitud_cambio")
@Getter                           // Genera los getters
@Setter                           // Genera los setters
@NoArgsConstructor                // Constructor vacío (lo usa JPA)
@AllArgsConstructor               // Constructor con todos los campos
public class SolicitudCambio {
    public static final String PENDIENTE = "pendiente";  // Constante: estado de una solicitud que aún nadie revisa
    public static final String APROBADA = "aprobada";  // Constante: el personal aceptó el cambio
    public static final String RECHAZADA = "rechazada";  // Constante: el personal no aceptó el cambio

    @Id  // Llave primaria de la tabla solicitud_cambio
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // MySQL asigna el id solo (AUTO_INCREMENT)
    private Long id;  // Número único de la solicitud

    private Long mascotaId;  // Id de la mascota a la que se le quiere cambiar algo
    private String correoSolicitante;  // Correo de quien pide el cambio (el dueño)
    private String raza;  // Raza nueva propuesta
    private String sexo;  // Sexo nuevo propuesto
    private String peso;  // Peso nuevo propuesto
    @Column(columnDefinition = "LONGTEXT")
    private String imagen;  // Imagen nueva propuesta
    private String descripcion;  // Descripción nueva propuesta
    private String estado;  // pendiente | aprobada | rechazada
    private LocalDateTime fechaSolicitud;  // Fecha y hora en que se creó la solicitud
    private LocalDateTime fechaResolucion;  // Fecha y hora en que se aprobó o rechazó (null mientras esté pendiente)

}