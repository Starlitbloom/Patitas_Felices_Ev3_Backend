package com.patitas.notificacion_service.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.patitas.notificacion_service.model.Notificacion;
import com.patitas.notificacion_service.model.TipoNotificacion;
import com.patitas.notificacion_service.repository.NotificacionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lógica de las notificaciones.
 *
 * Reglas:
 * - El personal (ADMIN / RECEPCIONISTA) puede crear avisos para cualquier usuario.
 *   Así lo hacen los otros microservicios cuando, por ejemplo, se responde un mensaje.
 * - Un CLIENTE solo puede crear avisos para sí mismo (ej: "Tu cita quedó registrada").
 * - Cada usuario solo ve, marca y elimina SUS notificaciones (el admin puede eliminar cualquiera).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    /* ---------------- CREAR ---------------- */

    public Notificacion crear(Notificacion datos, String correoCreador, boolean esPersonal) {
        String destino = texto(datos.getCorreoDestinatario()).toLowerCase();
        if (destino.isEmpty()) throw new NegocioException("correoDestinatario", "El destinatario es obligatorio.");
        if (!destino.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || destino.length() > 100) {
            throw new NegocioException("correoDestinatario", "El correo del destinatario no es válido.");
        }
        if (!esPersonal && !destino.equalsIgnoreCase(correoCreador)) {
            throw new NegocioException("correoDestinatario",
                "Solo el personal de la clínica puede enviar avisos a otros usuarios.", HttpStatus.FORBIDDEN);
        }

        String titulo = texto(datos.getTitulo());
        if (titulo.length() < 3 || titulo.length() > 80) {
            throw new NegocioException("titulo", "El título debe tener entre 3 y 80 caracteres.");
        }
        String mensaje = texto(datos.getMensaje());
        if (mensaje.length() < 3 || mensaje.length() > 300) {
            throw new NegocioException("mensaje", "El mensaje debe tener entre 3 y 300 caracteres.");
        }
        String enlace = texto(datos.getEnlace());
        if (!enlace.isEmpty() && (!enlace.startsWith("/") || enlace.length() > 150)) {
            throw new NegocioException("enlace", "El enlace debe ser una ruta del sitio que empiece con '/'.");
        }

        Notificacion nueva = new Notificacion();
        nueva.setCorreoDestinatario(destino);
        nueva.setTitulo(titulo);
        nueva.setMensaje(mensaje);
        nueva.setTipo(datos.getTipo() != null ? datos.getTipo() : TipoNotificacion.SISTEMA);
        nueva.setEnlace(enlace.isEmpty() ? null : enlace);
        nueva.setLeida(false);
        nueva.setCreadaPor(correoCreador);
        return notificacionRepository.save(nueva);
    }

    /* ---------------- LEER ---------------- */

    @Transactional(readOnly = true)
    public List<Notificacion> misNotificaciones(String correo, boolean soloNoLeidas) {
        return soloNoLeidas
            ? notificacionRepository.findByCorreoDestinatarioIgnoreCaseAndLeidaFalseOrderByFechaCreacionDesc(correo)
            : notificacionRepository.findByCorreoDestinatarioIgnoreCaseOrderByFechaCreacionDesc(correo);
    }

    /** Número que se muestra sobre la campanita. */
    @Transactional(readOnly = true)
    public long contarNoLeidas(String correo) {
        return notificacionRepository.countByCorreoDestinatarioIgnoreCaseAndLeidaFalse(correo);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> listarTodas() {
        return notificacionRepository.findAllByOrderByFechaCreacionDesc();
    }

    @Transactional(readOnly = true)
    public Notificacion obtenerPorId(Long id) {
        return notificacionRepository.findById(id)
            .orElseThrow(() -> new NegocioException("id", "La notificación con id " + id + " no existe.", HttpStatus.NOT_FOUND));
    }

    /* ---------------- ACTUALIZAR ---------------- */

    public Notificacion marcarLeida(Long id, String correo) {
        Notificacion n = obtenerPorId(id);
        verificarDueno(n, correo);
        if (!Boolean.TRUE.equals(n.getLeida())) {
            n.setLeida(true);
            n.setFechaLectura(LocalDateTime.now());
        }
        return notificacionRepository.save(n);
    }

    /** Marca todas las del usuario como leídas. Devuelve cuántas cambió. */
    public int marcarTodasLeidas(String correo) {
        List<Notificacion> pendientes =
            notificacionRepository.findByCorreoDestinatarioIgnoreCaseAndLeidaFalseOrderByFechaCreacionDesc(correo);
        LocalDateTime ahora = LocalDateTime.now();
        pendientes.forEach(n -> {
            n.setLeida(true);
            n.setFechaLectura(ahora);
        });
        notificacionRepository.saveAll(pendientes);
        return pendientes.size();
    }

    /* ---------------- ELIMINAR ---------------- */

    public void eliminar(Long id, String correo, boolean esAdmin) {
        Notificacion n = obtenerPorId(id);
        if (!esAdmin) verificarDueno(n, correo);
        notificacionRepository.delete(n);
    }

    /* ---------------- ESTADÍSTICAS ---------------- */

    @Transactional(readOnly = true)
    public Map<String, Long> estadisticas() {
        Map<String, Long> kpis = new LinkedHashMap<>();
        kpis.put("total", notificacionRepository.count());
        kpis.put("noLeidas", notificacionRepository.countByLeidaFalse());
        for (TipoNotificacion tipo : TipoNotificacion.values()) {
            kpis.put(tipo.name().toLowerCase(), notificacionRepository.countByTipo(tipo));
        }
        return kpis;
    }

    /* ---------------- Auxiliares ---------------- */

    private void verificarDueno(Notificacion n, String correo) {
        if (!n.getCorreoDestinatario().equalsIgnoreCase(correo)) {
            throw new NegocioException(null, "Esta notificación no es tuya.", HttpStatus.FORBIDDEN);
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
