// Carpeta (paquete) donde vive este archivo: repository
package com.patitas.historial_service.repository;

// Tipo "lista" de Java
import java.util.List;

// JpaRepository: trae listo findAll, findById, save, delete, count...
import org.springframework.data.jpa.repository.JpaRepository;

// La entidad que maneja este repositorio
import com.patitas.historial_service.model.RegistroClinico;

// Interfaz (no clase): Spring escribe el código por nosotros. <RegistroClinico, Long> = entidad y tipo de su id
public interface RegistroClinicoRepository extends JpaRepository<RegistroClinico, Long> {

    // Spring arma la consulta leyendo el nombre: WHERE mascota_id = ? ORDER BY fecha DESC (más reciente primero)
    List<RegistroClinico> findByMascotaIdOrderByFechaDesc(Long mascotaId);
}
