package com.patitas.contacto_service.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mensaje enviado desde el formulario de la página /contacto.
 * - nombre, correo, telefono, asunto y mensaje: los escribe el visitante
 * - estado, respuesta, respondidoPor y fechaRespuesta: los maneja el personal
 * - correoUsuario: si el visitante tenía la sesión iniciada, se guarda su
 *   correo de cuenta para que después vea la respuesta en "Mis mensajes"
 */
@Data
@Entity
@Table(name = "mensajes_contacto")
@NoArgsConstructor
@AllArgsConstructor
public class MensajeContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String correo;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 40)
    private String asunto; // Consulta general | Servicios veterinarios | Información sobre citas | Otro

    @Column(nullable = false, length = 1000)
    private String mensaje;

    @Enumerated(EnumType.STRING) // Se guarda como texto ("PENDIENTE") y no como número
    @Column(nullable = false, length = 20)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // El visitante no puede elegir el estado
    private EstadoMensaje estado;

    @Column(length = 1000)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String respuesta;

    @Column(length = 100)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String respondidoPor; // Correo del admin/recepcionista que respondió

    @Column(length = 100)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String correoUsuario; // Correo de la cuenta si envió el mensaje con sesión iniciada

    @CreationTimestamp
    @Column(updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaEnvio;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaRespuesta;
}
