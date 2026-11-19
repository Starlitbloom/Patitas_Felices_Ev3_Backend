package com.patitas.notificacion_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.patitas.notificacion_service.model.Notificacion;
import com.patitas.notificacion_service.model.TipoNotificacion;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findAllByOrderByFechaCreacionDesc(); // Todas (admin)
    List<Notificacion> findByCorreoDestinatarioIgnoreCaseOrderByFechaCreacionDesc(String correo); // Mis notificaciones
    List<Notificacion> findByCorreoDestinatarioIgnoreCaseAndLeidaFalseOrderByFechaCreacionDesc(String correo); // Solo no leídas
    long countByCorreoDestinatarioIgnoreCaseAndLeidaFalse(String correo); // Número de la campanita
    long countByLeidaFalse(); // KPI
    long countByTipo(TipoNotificacion tipo); // KPI por tipo
}
