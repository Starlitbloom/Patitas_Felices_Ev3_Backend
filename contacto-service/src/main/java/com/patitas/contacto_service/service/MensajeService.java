package com.patitas.contacto_service.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.patitas.contacto_service.model.EstadoMensaje;
import com.patitas.contacto_service.model.MensajeContacto;
import com.patitas.contacto_service.repository.MensajeContactoRepository;
import com.patitas.contacto_service.webclient.NotificacionClient;

import lombok.RequiredArgsConstructor;

/**
 * Lógica de los mensajes de contacto.
 * Las validaciones son las mismas del componente FormularioContacto.tsx del frontend.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MensajeService {

    public static final List<String> ASUNTOS_PERMITIDOS = List.of(
        "Consulta general", "Servicios veterinarios", "Información sobre citas", "Otro");

    private static final String REGEX_NOMBRE = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü\\s]+$";
    private static final String REGEX_CORREO = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String REGEX_CELULAR = "^(?:\\+?56)?9\\d{8}$"; // +56 9 1234 5678 | 912345678

    private final MensajeContactoRepository mensajeRepository;
    private final NotificacionClient notificacionClient;

    /* ---------------- LEER ---------------- */

    @Transactional(readOnly = true)
    public List<MensajeContacto> listarMensajes(EstadoMensaje estado) {
        if (estado == null) return mensajeRepository.findAllByOrderByFechaEnvioDesc();
        return mensajeRepository.findByEstadoOrderByFechaEnvioDesc(estado);
    }

    @Transactional(readOnly = true)
    public MensajeContacto obtenerPorId(Long id) {
        return mensajeRepository.findById(id)
            .orElseThrow(() -> new NegocioException("id", "El mensaje con id " + id + " no existe.", HttpStatus.NOT_FOUND));
    }

    /** Mensajes que envió un usuario con su sesión iniciada (para ver las respuestas). */
    @Transactional(readOnly = true)
    public List<MensajeContacto> listarMisMensajes(String correoUsuario) {
        return mensajeRepository.findByCorreoUsuarioIgnoreCaseOrderByFechaEnvioDesc(correoUsuario);
    }

    /* ---------------- CREAR ---------------- */

    /**
     * Guarda un mensaje del formulario.
     * @param correoUsuario correo de la cuenta si el visitante tenía sesión iniciada, o null
     */
    public MensajeContacto enviarMensaje(MensajeContacto datos, String correoUsuario) {
        validar(datos);

        MensajeContacto nuevo = new MensajeContacto();
        nuevo.setNombre(datos.getNombre().trim());
        nuevo.setCorreo(datos.getCorreo().trim());
        nuevo.setTelefono(datos.getTelefono().replaceAll("[\\s-]", ""));
        nuevo.setAsunto(datos.getAsunto().trim());
        nuevo.setMensaje(datos.getMensaje().trim());
        nuevo.setEstado(EstadoMensaje.PENDIENTE);
        nuevo.setCorreoUsuario(correoUsuario);
        return mensajeRepository.save(nuevo);
    }

    /* ---------------- ACTUALIZAR ---------------- */

    /** Cambia el estado (marcar como leído, archivar...). */
    public MensajeContacto cambiarEstado(Long id, EstadoMensaje nuevoEstado) {
        if (nuevoEstado == null) throw new NegocioException("estado", "El estado es obligatorio.");
        if (nuevoEstado == EstadoMensaje.RESPONDIDO) {
            throw new NegocioException("estado", "Para marcar como respondido usa el endpoint /responder.");
        }
        MensajeContacto mensaje = obtenerPorId(id);
        mensaje.setEstado(nuevoEstado);
        return mensajeRepository.save(mensaje);
    }

    /**
     * El personal escribe una respuesta: el mensaje queda RESPONDIDO.
     * Si el mensaje se envió con sesión iniciada, se avisa al usuario por
     * notificacion-service (WebClient) para que lo vea en la campanita.
     */
    public MensajeContacto responder(Long id, String respuesta, String correoPersonal, String authHeader) {
        if (respuesta == null || respuesta.trim().length() < 5) {
            throw new NegocioException("respuesta", "La respuesta debe tener al menos 5 caracteres.");
        }
        if (respuesta.trim().length() > 1000) {
            throw new NegocioException("respuesta", "La respuesta no puede superar los 1000 caracteres.");
        }
        MensajeContacto mensaje = obtenerPorId(id);
        if (mensaje.getEstado() == EstadoMensaje.ARCHIVADO) {
            throw new NegocioException("estado", "No se puede responder un mensaje archivado.", HttpStatus.CONFLICT);
        }
        mensaje.setRespuesta(respuesta.trim());
        mensaje.setRespondidoPor(correoPersonal);
        mensaje.setFechaRespuesta(LocalDateTime.now());
        mensaje.setEstado(EstadoMensaje.RESPONDIDO);
        MensajeContacto guardado = mensajeRepository.save(mensaje);

        // Comunicación entre microservicios: contacto-service → notificacion-service
        if (guardado.getCorreoUsuario() != null) {
            notificacionClient.crearNotificacion(
                guardado.getCorreoUsuario(),
                "Respondimos tu mensaje",
                "La clínica respondió tu consulta sobre '" + guardado.getAsunto() + "'. Revisa la respuesta en Mis mensajes.",
                "CONTACTO",
                "/mis-mensajes",
                authHeader);
        }
        return guardado;
    }

    /* ---------------- ELIMINAR ---------------- */

    public void eliminarMensaje(Long id) {
        mensajeRepository.delete(obtenerPorId(id));
    }

    /* ---------------- ESTADÍSTICAS (dashboard / report-service) ---------------- */

    @Transactional(readOnly = true)
    public Map<String, Long> estadisticas() {
        Map<String, Long> kpis = new LinkedHashMap<>();
        kpis.put("total", mensajeRepository.count());
        for (EstadoMensaje estado : EstadoMensaje.values()) {
            kpis.put(estado.name().toLowerCase(), mensajeRepository.countByEstado(estado));
        }
        return kpis;
    }

    /* ---------------- Validaciones (iguales al frontend) ---------------- */

    void validar(MensajeContacto d) {
        String nombre = texto(d.getNombre());
        if (nombre.isEmpty()) throw new NegocioException("nombre", "Por favor, ingresa tu nombre.");
        if (nombre.length() < 3) throw new NegocioException("nombre", "El nombre debe tener al menos 3 caracteres.");
        if (nombre.length() > 60) throw new NegocioException("nombre", "El nombre no puede superar los 60 caracteres.");
        if (!nombre.matches(REGEX_NOMBRE)) throw new NegocioException("nombre", "El nombre solo puede contener letras y espacios.");

        String correo = texto(d.getCorreo());
        if (correo.isEmpty()) throw new NegocioException("correo", "Por favor, ingresa tu correo electrónico.");
        if (correo.length() > 100 || !correo.matches(REGEX_CORREO)) {
            throw new NegocioException("correo", "Ingresa un correo electrónico válido.");
        }

        String telefono = texto(d.getTelefono()).replaceAll("[\\s-]", "");
        if (telefono.isEmpty()) throw new NegocioException("telefono", "Por favor, ingresa tu teléfono.");
        if (!telefono.matches(REGEX_CELULAR)) {
            throw new NegocioException("telefono", "Ingresa un celular chileno válido. Ejemplo: +56 9 1234 5678.");
        }

        String asunto = texto(d.getAsunto());
        if (asunto.isEmpty()) throw new NegocioException("asunto", "Selecciona el motivo de tu consulta.");
        if (!ASUNTOS_PERMITIDOS.contains(asunto)) throw new NegocioException("asunto", "Selecciona una opción válida.");

        String mensaje = texto(d.getMensaje());
        if (mensaje.isEmpty()) throw new NegocioException("mensaje", "Por favor, escribe tu mensaje.");
        if (mensaje.length() < 10) throw new NegocioException("mensaje", "Tu mensaje debe tener al menos 10 caracteres.");
        if (mensaje.length() > 1000) throw new NegocioException("mensaje", "Tu mensaje no puede superar los 1000 caracteres.");
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
