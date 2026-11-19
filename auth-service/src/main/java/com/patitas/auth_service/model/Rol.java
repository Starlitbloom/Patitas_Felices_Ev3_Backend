package com.patitas.auth_service.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.NoArgsConstructor;

/**
 * Roles del sistema (control de acceso basado en roles)
 * - ADMIN: gestiona todo el sistema, incluidos usuarios y reportes
 * - RECEPCIONISTA: personal de la clínica; gestiona citas y fichas, NO usuarios
 * - CLIENTE: dueño/a de mascota
 */
@Data
@Entity
@Table(name = "roles") // Nombre de la tabla en la base de datos
@NoArgsConstructor
@AllArgsConstructor
public class Rol {

    public static final String ADMIN = "ADMIN";
    public static final String RECEPCIONISTA = "RECEPCIONISTA";
    public static final String CLIENTE = "CLIENTE";

    @Id // Clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Autoincrementable
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    @NotBlank(message = "El nombre del rol no puede estar vacío")
    private String nombre;

    @Column(length = 100)
    private String descripcion; // Texto para mostrar en pantalla (ej "Dueña de mascota")

    @OneToMany(mappedBy = "rol") // Un rol tiene muchos usuarios
    @JsonIgnore // No se incluyen los usuarios al consultar roles (evita ciclos)
    @ToString.Exclude // Evita un ciclo infinito Rol -> Usuario -> Rol en toString()
    @EqualsAndHashCode.Exclude
    private List<Usuario> usuarios;

    public Rol(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
}

