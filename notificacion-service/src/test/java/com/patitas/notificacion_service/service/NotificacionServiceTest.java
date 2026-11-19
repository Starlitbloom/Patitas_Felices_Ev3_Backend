package com.patitas.notificacion_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.patitas.notificacion_service.model.Notificacion;
import com.patitas.notificacion_service.model.TipoNotificacion;
import com.patitas.notificacion_service.repository.NotificacionRepository;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock private NotificacionRepository notificacionRepository;

    @InjectMocks private NotificacionService notificacionService;

    private Notificacion datos(String destino) {
        Notificacion n = new Notificacion();
        n.setCorreoDestinatario(destino);
        n.setTitulo("Cita confirmada");
        n.setMensaje("Tu cita quedó confirmada para el lunes.");
        n.setTipo(TipoNotificacion.CITA);
        n.setEnlace("/mis-citas");
        return n;
    }

    private Notificacion guardada(Long id, String destino, boolean leida) {
        Notificacion n = datos(destino);
        n.setId(id);
        n.setLeida(leida);
        return n;
    }

    @Test
    void personalCreaAvisoParaOtroUsuario_quedaNoLeida() {
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Notificacion n = notificacionService.crear(datos("Camila.Rojas@gmail.com"), "jo.medina@duoc.cl", true);

        assertFalse(n.getLeida());
        assertEquals("camila.rojas@gmail.com", n.getCorreoDestinatario()); // Se guarda en minúsculas
        assertEquals("jo.medina@duoc.cl", n.getCreadaPor());
    }

    @Test
    void clienteNoPuedeCrearAvisoParaOtro() {
        NegocioException e = assertThrows(NegocioException.class,
            () -> notificacionService.crear(datos("otra@gmail.com"), "camila.rojas@gmail.com", false));

        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void clientePuedeCrearAvisoParaSiMismo() {
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Notificacion n = notificacionService.crear(datos("camila.rojas@gmail.com"), "camila.rojas@gmail.com", false);
        assertEquals("camila.rojas@gmail.com", n.getCorreoDestinatario());
    }

    @Test
    void sinTipo_quedaComoSistema() {
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));
        Notificacion d = datos("camila.rojas@gmail.com");
        d.setTipo(null);

        assertEquals(TipoNotificacion.SISTEMA, notificacionService.crear(d, "ma.calfileo@duoc.cl", true).getTipo());
    }

    @Test
    void enlaceExterno_noSePermite() {
        Notificacion d = datos("camila.rojas@gmail.com");
        d.setEnlace("https://sitio-malicioso.com");

        NegocioException e = assertThrows(NegocioException.class, () -> notificacionService.crear(d, "ma.calfileo@duoc.cl", true));
        assertEquals("enlace", e.getCampo());
    }

    @Test
    void marcarLeida_guardaFechaDeLectura() {
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(guardada(1L, "camila.rojas@gmail.com", false)));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        Notificacion n = notificacionService.marcarLeida(1L, "camila.rojas@gmail.com");

        assertTrue(n.getLeida());
        assertNotNull(n.getFechaLectura());
    }

    @Test
    void marcarLeida_notificacionAjena_lanza403() {
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(guardada(1L, "camila.rojas@gmail.com", false)));

        NegocioException e = assertThrows(NegocioException.class,
            () -> notificacionService.marcarLeida(1L, "be.soto@duoc.cl"));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    void marcarTodasLeidas_devuelveCuantasCambio() {
        when(notificacionRepository.findByCorreoDestinatarioIgnoreCaseAndLeidaFalseOrderByFechaCreacionDesc("camila.rojas@gmail.com"))
            .thenReturn(List.of(guardada(1L, "camila.rojas@gmail.com", false), guardada(2L, "camila.rojas@gmail.com", false)));

        assertEquals(2, notificacionService.marcarTodasLeidas("camila.rojas@gmail.com"));
    }

    @Test
    void adminPuedeEliminarCualquiera() {
        Notificacion n = guardada(1L, "camila.rojas@gmail.com", false);
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(n));

        notificacionService.eliminar(1L, "ma.calfileo@duoc.cl", true);
        verify(notificacionRepository).delete(n);
    }
}
