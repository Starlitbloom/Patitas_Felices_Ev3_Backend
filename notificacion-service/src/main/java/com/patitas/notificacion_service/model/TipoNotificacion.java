package com.patitas.notificacion_service.model;

/**
 * De dónde viene el aviso (el frontend puede mostrar un ícono distinto por tipo).
 */
public enum TipoNotificacion {
    CITA,       // Cita confirmada, cancelada, reprogramada (cita-service)
    MASCOTA,    // Ficha creada o actualizada (mascota-service / historial-service)
    SOLICITUD,  // Solicitud de cambio aprobada o rechazada (mascota-service)
    CONTACTO,   // Respondieron tu mensaje de contacto (contacto-service)
    SISTEMA     // Avisos generales enviados por un administrador
}
