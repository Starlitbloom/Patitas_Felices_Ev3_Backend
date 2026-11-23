package com.patitas.mascota_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

//Mascota (paciente) de la clinica. Tabla: mascota (la crea Hibernate al iniciar).
@Entity 
@Table (name="mascotas")
public class Mascota {
    @Id  // Marca este campo como la llave primaria de la tabla (identifica cada mascota)
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // MySQL asigna el id solo: 1, 2, 3... (AUTO_INCREMENT)
    private Long id;  // Número único de la mascota

    private String nombre;  // Nombre de la mascota (ej: Rocky)
    private String especie;      // Perro | Gato | Conejo | Ave
    private String raza;  // Raza (ej: Golden Retriever)
    private String edad;  // Edad escrita como texto (ej: "3 años")
    private String sexo;         // Macho | Hembra (null si aun no se sabe)
    private String peso;  // Peso escrito como texto (ej: "28 kg")
    private String nombreDueno;  // Nombre completo del dueño (sin ñ para evitar problemas en JSON y BD)
    private String correoDueno;  // correo del dueno: vincula la mascota con su usuario
    private boolean completo;    // true cuando ya tiene raza, sexo, peso e imagen
    private String imagen;  // Ruta de la foto (ej: /img/golden.jpg)
    private String estado;       // al-dia | tratamiento | pendiente
    private String descripcion;  // Texto libre sobre la mascota

    // ---- Getters y setters: permiten leer (get) y cambiar (set) cada campo desde otras clases ----

    //Getters y setters de id 
    public Long getId() { return id; }  // Devuelve el id
    public void setId(Long id) { this.id = id; }  // Cambia el id

    //Getters y setters de nombre
    public String getNombre() { return nombre; }  // Devuelve el nombre
    public void setNombre(String nombre) { this.nombre = nombre; }  // Cambia el nombre

    //Getters y setters de especie
    public String getEspecie() { return especie; }  // Devuelve la especie
    public void setEspecie(String especie) { this.especie = especie; }  // Cambia la especie

    //Getters y setters de raza
    public String getRaza() { return raza; }  // Devuelve la raza
    public void setRaza(String raza) { this.raza = raza; }  // Cambia la raza

    //Getters y setters de edad
    public String getEdad() { return edad; }  // Devuelve la edad
    public void setEdad(String edad) { this.edad = edad; }  // Cambia la edad

    //Getters y setters de sexo
    public String getSexo() { return sexo; }  // Devuelve el sexo
    public void setSexo(String sexo) { this.sexo = sexo; }  // Cambia el sexo

    //Getters y setters de peso
    public String getPeso() { return peso; }  // Devuelve el peso
    public void setPeso(String peso) { this.peso = peso; }  // Cambia el peso

    //Getters y setters de nombreDueno
    public String getNombreDueno() { return nombreDueno; }  // Devuelve el nombre del dueño
    public void setNombreDueno(String nombreDueno) { this.nombreDueno = nombreDueno; }  // Cambia el nombre del dueño

    //Getters y setters de correoDueno
    public String getCorreoDueno() { return correoDueno; }  // Devuelve el correo del dueño
    public void setCorreoDueno(String correoDueno) { this.correoDueno = correoDueno; }  // Cambia el correo del dueño

    //Getters y setters de completo
    public boolean isCompleto() { return completo; }  // Dice si la ficha está completa (en boolean el getter empieza con "is")
    public void setCompleto(boolean completo) { this.completo = completo; }  // Cambia si está completa

    //Getters y setters de imagen
    public String getImagen() { return imagen; }  // Devuelve la ruta de la imagen
    public void setImagen(String imagen) { this.imagen = imagen; }  // Cambia la imagen

    //Getters y setters de estado
    public String getEstado() { return estado; }  // Devuelve el estado de salud
    public void setEstado(String estado) { this.estado = estado; }  // Cambia el estado

    //Getters y setters de descripcion
    public String getDescripcion() { return descripcion; }  // Devuelve la descripción
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }  // Cambia la descripción

    /** Regla de negocio: una mascota esta "completa" si tiene raza, sexo, peso e imagen. */
    public void recalcularCompleto() {  // Método que decide si la ficha está completa
        this.completo = tiene(raza) && tiene(sexo) && tiene(peso) && tiene(imagen);  // true solo si los 4 datos tienen texto
    }

    private static boolean tiene(String s) {  // Ayudante: revisa si un texto trae algo escrito
        return s != null && !s.isBlank();  // true si no es null y no está vacío ni son solo espacios
    }
}
