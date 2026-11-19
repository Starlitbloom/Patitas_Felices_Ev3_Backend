package com.patitas.notificacion_service.model;

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
 * Aviso para un usuario. Se identifica al destinatario por su correo,
 * que es el mismo "sub" que trae el token JWT.
 */
@Data
@Entity
@Table(name = "notificaciones")
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String correoDestinatario;

    @Column(nullable = false, length = 80)
    private String titulo;

    @Column(nullable = false, length = 300)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoNotificacion tipo;

    @Column(length = 150)
    private String enlace; // Ruta del frontend a la que lleva el aviso (ej: "/mis-citas")

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // Al crearla siempre parte sin leer
    private Boolean leida;

    @Column(length = 100)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String creadaPor; // Correo de quien generó el aviso (admin, recepcionista o el propio usuario)

    @CreationTimestamp
    @Column(updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaCreacion;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaLectura;
}
