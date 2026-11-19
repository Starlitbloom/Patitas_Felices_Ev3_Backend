package com.patitas.contacto_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.patitas.contacto_service.model.EstadoMensaje;
import com.patitas.contacto_service.model.MensajeContacto;
import com.patitas.contacto_service.repository.MensajeContactoRepository;

@ExtendWith(MockitoExtension.class)
class MensajeServiceTest {

    @Mock private MensajeContactoRepository mensajeRepository;

    @InjectMocks private MensajeService mensajeService;

    private MensajeContacto datosValidos() {
        MensajeContacto m = new MensajeContacto();
        m.setNombre("Camila Rojas");
        m.setCorreo("camila.rojas@gmail.com");
        m.setTelefono("+56 9 1234 5678");
        m.setAsunto("Información sobre citas");
        m.setMensaje("¿Atienden conejos los sábados?");
        return m;
    }

    @Test
    void enviarMensaje_quedaPendienteYConTelefonoLimpio() {
        when(mensajeRepository.save(any(MensajeContacto.class))).thenAnswer(inv -> inv.getArgument(0));

        MensajeContacto guardado = mensajeService.enviarMensaje(datosValidos(), "camila.rojas@gmail.com");

        assertEquals(EstadoMensaje.PENDIENTE, guardado.getEstado());
        assertEquals("+56912345678", guardado.getTelefono());
        assertEquals("camila.rojas@gmail.com", guardado.getCorreoUsuario());
    }

    @Test
    void enviarMensaje_nombreConNumeros_lanzaError() {
        MensajeContacto datos = datosValidos();
        datos.setNombre("Camila123");

        NegocioException e = assertThrows(NegocioException.class, () -> mensajeService.enviarMensaje(datos, null));
        assertEquals("nombre", e.getCampo());
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    void enviarMensaje_telefonoNoChileno_lanzaError() {
        MensajeContacto datos = datosValidos();
        datos.setTelefono("12345");

        NegocioException e = assertThrows(NegocioException.class, () -> mensajeService.enviarMensaje(datos, null));
        assertEquals("telefono", e.getCampo());
    }

    @Test
    void enviarMensaje_asuntoNoPermitido_lanzaError() {
        MensajeContacto datos = datosValidos();
        datos.setAsunto("Spam");

        NegocioException e = assertThrows(NegocioException.class, () -> mensajeService.enviarMensaje(datos, null));
        assertEquals("asunto", e.getCampo());
    }

    @Test
    void enviarMensaje_mensajeMuyCorto_lanzaError() {
        MensajeContacto datos = datosValidos();
        datos.setMensaje("Hola");

        NegocioException e = assertThrows(NegocioException.class, () -> mensajeService.enviarMensaje(datos, null));
        assertEquals("mensaje", e.getCampo());
    }

    @Test
    void responder_quedaRespondidoConAutorYFecha() {
        MensajeContacto m = datosValidos();
        m.setId(1L);
        m.setEstado(EstadoMensaje.PENDIENTE);
        when(mensajeRepository.findById(1L)).thenReturn(Optional.of(m));
        when(mensajeRepository.save(any(MensajeContacto.class))).thenAnswer(inv -> inv.getArgument(0));

        MensajeContacto r = mensajeService.responder(1L, "Sí, atendemos conejos.", "jo.medina@duoc.cl");

        assertEquals(EstadoMensaje.RESPONDIDO, r.getEstado());
        assertEquals("jo.medina@duoc.cl", r.getRespondidoPor());
        assertNotNull(r.getFechaRespuesta());
    }

    @Test
    void responder_mensajeArchivado_lanzaConflicto() {
        MensajeContacto m = datosValidos();
        m.setEstado(EstadoMensaje.ARCHIVADO);
        when(mensajeRepository.findById(2L)).thenReturn(Optional.of(m));

        NegocioException e = assertThrows(NegocioException.class,
            () -> mensajeService.responder(2L, "Respuesta tardía", "ma.calfileo@duoc.cl"));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void cambiarEstado_aRespondidoDirecto_noSePermite() {
        assertThrows(NegocioException.class, () -> mensajeService.cambiarEstado(1L, EstadoMensaje.RESPONDIDO));
    }

    @Test
    void obtenerPorId_noExiste_lanza404() {
        when(mensajeRepository.findById(99L)).thenReturn(Optional.empty());

        NegocioException e = assertThrows(NegocioException.class, () -> mensajeService.obtenerPorId(99L));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }
}
