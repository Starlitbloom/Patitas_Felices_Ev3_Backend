package com.patitas.contacto_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.patitas.contacto_service.model.EstadoMensaje;
import com.patitas.contacto_service.model.MensajeContacto;

@Repository // Se comunica con la base de datos
public interface MensajeContactoRepository extends JpaRepository<MensajeContacto, Long> {
    List<MensajeContacto> findAllByOrderByFechaEnvioDesc(); // Bandeja: más recientes primero
    List<MensajeContacto> findByEstadoOrderByFechaEnvioDesc(EstadoMensaje estado); // Filtrar por estado
    List<MensajeContacto> findByCorreoUsuarioIgnoreCaseOrderByFechaEnvioDesc(String correoUsuario); // "Mis mensajes"
    long countByEstado(EstadoMensaje estado); // KPIs
}
