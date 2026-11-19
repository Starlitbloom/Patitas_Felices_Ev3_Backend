package com.patitas.contacto_service.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.patitas.contacto_service.model.EstadoMensaje;
import com.patitas.contacto_service.model.MensajeContacto;
import com.patitas.contacto_service.repository.MensajeContactoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Carga mensajes de ejemplo la primera vez (tabla vacía),
 * para que la bandeja del admin no parta vacía en la demo.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MensajeContactoRepository mensajeRepository;

    @Override
    public void run(String... args) {
        if (mensajeRepository.count() > 0) return;

        crear("Camila Rojas", "camila.rojas@gmail.com", "+56912345678", "Información sobre citas",
            "Hola, ¿atienden conejos los días sábado? Quiero agendar un control para mi conejo.",
            "camila.rojas@gmail.com", EstadoMensaje.PENDIENTE, null, null);

        crear("Pedro Muñoz", "pedro.munoz@gmail.com", "+56987654321", "Servicios veterinarios",
            "¿Realizan esterilización de gatos? Me gustaría saber el valor aproximado.",
            null, EstadoMensaje.RESPONDIDO,
            "¡Hola Pedro! Sí, realizamos esterilización felina. Puedes agendarla desde la sección Solicitar cita.",
            "ma.calfileo@duoc.cl");

        crear("Benjamín Soto", "be.soto@duoc.cl", "+56987654321", "Consulta general",
            "Quería felicitar al equipo por la atención que recibió mi perro la semana pasada.",
            "be.soto@duoc.cl", EstadoMensaje.LEIDO, null, null);
    }

    private void crear(String nombre, String correo, String telefono, String asunto, String mensaje,
            String correoUsuario, EstadoMensaje estado, String respuesta, String respondidoPor) {
        MensajeContacto m = new MensajeContacto();
        m.setNombre(nombre);
        m.setCorreo(correo);
        m.setTelefono(telefono);
        m.setAsunto(asunto);
        m.setMensaje(mensaje);
        m.setCorreoUsuario(correoUsuario);
        m.setEstado(estado);
        m.setRespuesta(respuesta);
        m.setRespondidoPor(respondidoPor);
        if (respuesta != null) m.setFechaRespuesta(LocalDateTime.now());
        mensajeRepository.save(m);
    }
}
