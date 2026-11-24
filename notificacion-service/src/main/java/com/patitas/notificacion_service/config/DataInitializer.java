package com.patitas.notificacion_service.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.patitas.notificacion_service.model.Notificacion;
import com.patitas.notificacion_service.model.TipoNotificacion;
import com.patitas.notificacion_service.repository.NotificacionRepository;

import lombok.RequiredArgsConstructor;

/** Carga avisos de ejemplo la primera vez (tabla vacía) para la demo. */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final NotificacionRepository notificacionRepository;

    @Override
    public void run(String... args) {
        if (notificacionRepository.count() > 0) return;

        crear("camila.rojas@gmail.com", "¡Bienvenida a Patitas Felices!",
            "Ya puedes registrar a tus mascotas y solicitar citas desde tu perfil.", TipoNotificacion.SISTEMA, "/perfil");
        crear("camila.rojas@gmail.com", "Recordatorio de vacuna",
            "A una de tus mascotas le corresponde un refuerzo de vacuna este mes.", TipoNotificacion.MASCOTA, "/mascotas");
        crear("ma.calfileo@duoc.cl", "Nuevo mensaje de contacto",
            "Hay mensajes pendientes en la bandeja de contacto.", TipoNotificacion.CONTACTO, "/admin/mensajes");
    }

    private void crear(String correo, String titulo, String mensaje, TipoNotificacion tipo, String enlace) {
        Notificacion n = new Notificacion();
        n.setCorreoDestinatario(correo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setTipo(tipo);
        n.setEnlace(enlace);
        n.setLeida(false);
        n.setCreadaPor("sistema");
        notificacionRepository.save(n);
    }
}