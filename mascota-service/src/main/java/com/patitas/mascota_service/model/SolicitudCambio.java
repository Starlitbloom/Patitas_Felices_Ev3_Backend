package com.patitas.mascota_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** El dueno propone cambios a una mascota ya completa; el personal los aprueba o rechaza. */
@Entity
@Table(name = "solicitud_cambio")
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
    private String imagen;  // Imagen nueva propuesta
    private String descripcion;  // Descripción nueva propuesta
    private String estado;  // pendiente | aprobada | rechazada
    private LocalDateTime fechaSolicitud;  // Fecha y hora en que se creó la solicitud
    private LocalDateTime fechaResolucion;  // Fecha y hora en que se aprobó o rechazó (null mientras esté pendiente)

    // ---- Getters y setters: leer (get) y cambiar (set) cada campo ----

    public Long getId() { return id; }  // Devuelve el id
    public void setId(Long id) { this.id = id; }  // Cambia el id

    public Long getMascotaId() { return mascotaId; }  // Devuelve el id de la mascota
    public void setMascotaId(Long mascotaId) { this.mascotaId = mascotaId; }  // Cambia el id de la mascota

    public String getCorreoSolicitante() { return correoSolicitante; }  // Devuelve el correo de quien pidió el cambio
    public void setCorreoSolicitante(String correoSolicitante) { this.correoSolicitante = correoSolicitante; }  // Cambia ese correo

    public String getRaza() { return raza; }  // Devuelve la raza propuesta
    public void setRaza(String raza) { this.raza = raza; }  // Cambia la raza propuesta

    public String getSexo() { return sexo; }  // Devuelve el sexo propuesto
    public void setSexo(String sexo) { this.sexo = sexo; }  // Cambia el sexo propuesto

    public String getPeso() { return peso; }  // Devuelve el peso propuesto
    public void setPeso(String peso) { this.peso = peso; }  // Cambia el peso propuesto

    public String getImagen() { return imagen; }  // Devuelve la imagen propuesta
    public void setImagen(String imagen) { this.imagen = imagen; }  // Cambia la imagen propuesta

    public String getDescripcion() { return descripcion; }  // Devuelve la descripción propuesta
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }  // Cambia la descripción propuesta

    public String getEstado() { return estado; }  // Devuelve el estado de la solicitud
    public void setEstado(String estado) { this.estado = estado; }  // Cambia el estado (pendiente, aprobada, rechazada)

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }  // Devuelve cuándo se pidió el cambio
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }  // Cambia esa fecha

    public LocalDateTime getFechaResolucion() { return fechaResolucion; }  // Devuelve cuándo se resolvió
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }  // Cambia esa fecha

}
