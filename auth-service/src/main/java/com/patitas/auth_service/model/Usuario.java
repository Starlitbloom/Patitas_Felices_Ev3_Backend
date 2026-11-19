package com.patitas.auth_service.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Usuario del sistema
 * - La contraseña se guarda ENCRIPTADA con BCrypt, nunca en texto plano.
 * - WRITE_ONLY: la contraseña se puede RECIBIR en el JSON (registro, editar)
 *   pero NUNCA se envía en las respuestas de la API.
 * - Las validaciones de formato están en service/Validaciones (mismas reglas que el frontend).
 * - Al actualizar, los campos que llegan en null no se modifican.
 */
@Data
@Entity
@Table(name = "usuarios")
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12) // RUN sin puntos ni guion (ej 111111111)
    private String run;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 100) // El correo es el "username" para iniciar sesión
    private String correo;

    @Column(nullable = false, length = 100)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // Nunca sale en las respuestas
    private String password;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String region;

    @Column(length = 60)
    private String comuna;

    @Column(length = 300)
    private String direccion;

    @Column(nullable = false)
    private Boolean activo; // Si es false, no puede iniciar sesión

    @Lob
    @Column(columnDefinition = "LONGTEXT") // Foto de perfil comprimida en base64 (opcional)
    private String avatar;

    // En el JSON viaja como { "id": 1, "nombre": "ADMIN", "descripcion": "Administrador" }.
    // Para crear o cambiar el rol basta enviar { "rol": { "nombre": "ADMIN" } }
    @ManyToOne // Muchos usuarios pueden tener un mismo rol
    @JoinColumn(name = "rol_id", nullable = false) // Llave a la tabla roles
    private Rol rol;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaRegistro; // La usa el KPI "Nuevos esta semana"

    @UpdateTimestamp
    private LocalDateTime actualizadoEn;
}
