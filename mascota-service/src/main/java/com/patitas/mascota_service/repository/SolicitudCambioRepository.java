package com.patitas.mascota_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.patitas.mascota_service.model.SolicitudCambio;

public interface SolicitudCambioRepository extends JpaRepository<SolicitudCambio, Long>{
    List<SolicitudCambio> findByEstado(String estado);

    /** Borra las solicitudes de una mascota (al eliminar la mascota). */
    void deleteByMascotaId(Long mascotaId);

    /** Solicitud abierta de una mascota (solo puede haber una pendiente a la vez). */
    Optional<SolicitudCambio> findFirstByMascotaIdAndEstado(Long mascotaId, String estado);

    /** Todas las solicitudes de una mascota, la más reciente primero. */
    List<SolicitudCambio> findByMascotaIdOrderByFechaSolicitudDesc(Long mascotaId);
}