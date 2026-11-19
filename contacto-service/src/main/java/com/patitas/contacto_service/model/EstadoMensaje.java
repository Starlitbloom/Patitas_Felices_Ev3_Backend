package com.patitas.contacto_service.model;

/**
 * Estados de un mensaje en la bandeja del personal:
 * PENDIENTE → recién llegado, nadie lo ha abierto
 * LEIDO     → el personal lo revisó
 * RESPONDIDO→ se le escribió una respuesta al remitente
 * ARCHIVADO → ya no requiere atención
 */
public enum EstadoMensaje {
    PENDIENTE,
    LEIDO,
    RESPONDIDO,
    ARCHIVADO
}
