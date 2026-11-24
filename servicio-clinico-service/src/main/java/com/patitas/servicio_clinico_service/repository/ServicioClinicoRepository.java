// Carpeta (paquete) donde vive este archivo: repository
package com.patitas.servicio_clinico_service.repository;

// Tipo "lista" de Java
import java.util.List;

// JpaRepository: trae listo findAll, findById, save, delete, count...
import org.springframework.data.jpa.repository.JpaRepository;

// La entidad que maneja este repositorio
import com.patitas.servicio_clinico_service.model.ServicioClinico;

// Interfaz: Spring genera la implementación. <ServicioClinico, Long> = entidad y tipo de su id
public interface ServicioClinicoRepository extends JpaRepository<ServicioClinico, Long> {

    // Servicios activos ordenados por grupo y luego por nombre (WHERE activo = true ORDER BY grupo, nombre)
    List<ServicioClinico> findByActivoTrueOrderByGrupoAscNombreAsc();

    // ¿Ya existe un servicio con este nombre? (sin importar mayúsculas). Sirve para no duplicar
    boolean existsByNombreIgnoreCase(String nombre);
}
