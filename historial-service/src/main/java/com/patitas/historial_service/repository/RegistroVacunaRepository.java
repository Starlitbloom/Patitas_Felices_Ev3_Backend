// Carpeta (paquete) donde vive este archivo: repository
package com.patitas.historial_service.repository;

// Tipo "lista" de Java
import java.util.List;

// JpaRepository: trae listo findAll, findById, save, delete, count...
import org.springframework.data.jpa.repository.JpaRepository;

// La entidad que maneja este repositorio
import com.patitas.historial_service.model.RegistroVacuna;

// Interfaz: Spring genera la implementación. <RegistroVacuna, Long> = entidad y tipo de su id
public interface RegistroVacunaRepository extends JpaRepository<RegistroVacuna, Long> {

    // Vacunas de una mascota, de la más reciente a la más antigua (WHERE mascota_id = ? ORDER BY fecha DESC)
    List<RegistroVacuna> findByMascotaIdOrderByFechaDesc(Long mascotaId);
}
