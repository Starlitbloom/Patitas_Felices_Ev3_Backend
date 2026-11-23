
package com.patitas.cita_service.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.patitas.cita_service.model.Cita;
import com.patitas.cita_service.repository.CitaRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de la logica de citas veterinarias.
 *
 * Se comprueban las operaciones de consulta, creacion,
 * actualizacion, cancelacion y cambios de estado.
 *
 * Mockito simula CitaRepository para evitar
 * modificaciones en la base de datos real.
 *
 * La conexion con MySQL se comprueba mediante
 * CitaServiceApplicationTests.
 */
@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    /**
     * Repositorio simulado para las pruebas.
     */
    @Mock
    private CitaRepository citaRepository;

    /**
     * Servicio que contiene la logica a comprobar.
     */
    private CitaService citaService;

    /**
     * Prepara el servicio antes de cada prueba.
     *
     * Se configura la direccion de veterinario-service,
     * pero estas pruebas no realizan llamadas HTTP reales.
     */
    @BeforeEach
    void configurar() {
        citaService = new CitaService(
                citaRepository,
                "http://localhost:8085"
        );
    }

    /**
     * Genera una cita de ejemplo con fecha futura.
     *
     * Se utiliza una fecha dinamica para que
     * las pruebas no dependan de un dia especifico.
     *
     * @return Cita con datos validos.
     */
    private Cita crearCitaEjemplo() {

        Cita cita = new Cita();

        cita.setId(1L);
        cita.setCorreoUsuario("cliente@patitas.cl");
        cita.setNombreUsuario("Cliente de Prueba");

        cita.setDueno("Cliente de Prueba");
        cita.setPropietario("Cliente de Prueba");
        cita.setCorreo("contacto@patitas.cl");
        cita.setTelefono("912345678");

        cita.setMascota("Luna");
        cita.setTipoMascota("Perro");
        cita.setEdadMascota(3.0);

        // Sin veterinario seleccionado para evitar
        // una llamada HTTP durante las pruebas unitarias.
        cita.setVeterinarioId(null);

        cita.setServicio("Consulta general");

        cita.setFecha(
                LocalDate.now(
                        ZoneId.of("America/Santiago")
                ).plusDays(2)
        );

        cita.setHora(LocalTime.of(10, 0));
        cita.setMotivo("Control preventivo");
        cita.setDuracionMinutos(30);
        cita.setEstado("Pendiente");

        return cita;
    }

    /**
     * TEST 1:
     * Verifica que el administrador pueda
     * consultar todas las citas.
     */
    @Test
    void listarTodasRetornaCitas() {

        Cita cita = crearCitaEjemplo();

        when(citaRepository.findAll())
                .thenReturn(List.of(cita));

        List<Cita> resultado = citaService.listarTodas();

        assertEquals(1, resultado.size());
        assertEquals("Luna", resultado.get(0).getMascota());

        verify(citaRepository).findAll();
    }

    /**
     * TEST 2:
     * Comprueba que se consulten las citas
     * asociadas al correo autenticado.
     */
    @Test
    void listarMisCitasPorCorreo() {

        Cita cita = crearCitaEjemplo();

        when(citaRepository
                .findByCorreoUsuarioIgnoreCaseOrderByFechaDescHoraDesc(
                        "cliente@patitas.cl"
                ))
                .thenReturn(List.of(cita));

        List<Cita> resultado = citaService.listarMisCitas(
                " CLIENTE@PATITAS.CL "
        );

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getId());
    }

    /**
     * TEST 3:
     * Verifica que un cliente pueda
     * consultar una cita que le pertenece.
     */
    @Test
    void obtenerCitaPropiaCorrectamente() {

        Cita cita = crearCitaEjemplo();

        when(citaRepository.findById(1L))
                .thenReturn(Optional.of(cita));

        Cita resultado = citaService.obtenerCita(
                1L,
                "cliente@patitas.cl",
                false
        );

        assertEquals("Luna", resultado.getMascota());
        assertEquals(1L, resultado.getId());
    }

    /**
     * TEST 4:
     * Comprueba que un cliente no pueda
     * consultar las citas de otra cuenta.
     */
    @Test
    void obtenerCitaAjenaRetorna403() {

        Cita cita = crearCitaEjemplo();

        when(citaRepository.findById(1L))
                .thenReturn(Optional.of(cita));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> citaService.obtenerCita(
                        1L,
                        "otrocliente@patitas.cl",
                        false
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                error.getStatusCode()
        );
    }

    /**
     * TEST 5:
     * Comprueba que un administrador pueda
     * consultar una cita de otro usuario.
     */
    @Test
    void administradorPuedeConsultarCitaAjena() {

        Cita cita = crearCitaEjemplo();

        when(citaRepository.findById(1L))
                .thenReturn(Optional.of(cita));

        Cita resultado = citaService.obtenerCita(
                1L,
                "admin@patitas.cl",
                true
        );

        assertEquals(1L, resultado.getId());
    }

    /**
     * TEST 6:
     * Verifica que una cita inexistente
     * genere HTTP 404.
     */
    @Test
    void citaInexistenteRetorna404() {

        when(citaRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> citaService.obtenerCita(
                        99L,
                        "cliente@patitas.cl",
                        false
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );
    }

    /**
     * TEST 7:
     * Comprueba la creacion de una cita sin
     * veterinario asignado.
     *
     * El servicio debe:
     * - Asignar la cuenta autenticada.
     * - Establecer el estado Pendiente.
     * - Asignar una duracion predeterminada.
     * - Indicar que el profesional esta por confirmar.
     */
    @Test
    void crearCitaCorrectamente() {

        Cita datos = crearCitaEjemplo();

        // Comprobar que el servicio no confie
        // en el correo recibido dentro del JSON.
        datos.setCorreoUsuario("falso@patitas.cl");
        datos.setDuracionMinutos(null);
        datos.setEstado("Confirmada");

        when(citaRepository.save(any(Cita.class)))
                .thenAnswer(invocacion -> {

                    Cita guardada = invocacion.getArgument(0);

                    // Simular el ID generado por MySQL.
                    guardada.setId(2L);

                    return guardada;
                });

        Cita resultado = citaService.crearCita(
                datos,
                " CLIENTE@PATITAS.CL ",
                "Cliente de Prueba"
        );

        assertEquals(2L, resultado.getId());
        assertEquals(
                "cliente@patitas.cl",
                resultado.getCorreoUsuario()
        );
        assertEquals("Pendiente", resultado.getEstado());
        assertEquals(30, resultado.getDuracionMinutos());
        assertTrue(resultado.getProfesionalPorConfirmar());
        assertEquals(
                "Por confirmar",
                resultado.getVeterinarioNombre()
        );

        verify(citaRepository).save(any(Cita.class));
    }

    /**
     * TEST 8:
     * Verifica que no se puedan reservar
     * fechas anteriores al dia actual.
     */
    @Test
    void crearCitaConFechaPasadaRetorna400() {

        Cita datos = crearCitaEjemplo();

        datos.setFecha(
                LocalDate.now(
                        ZoneId.of("America/Santiago")
                ).minusDays(1)
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> citaService.crearCita(
                        datos,
                        "cliente@patitas.cl",
                        "Cliente de Prueba"
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        verify(citaRepository, never())
                .save(any(Cita.class));
    }

    /**
     * TEST 9:
     * Comprueba que un cliente pueda
     * actualizar una reserva propia pendiente.
     */
    @Test
    void actualizarCitaPropiaCorrectamente() {

        Cita existente = crearCitaEjemplo();
        Cita datos = crearCitaEjemplo();

        datos.setMascota("Luna Actualizada");
        datos.setMotivo("Nueva consulta");

        when(citaRepository.findById(1L))
                .thenReturn(Optional.of(existente));

        when(citaRepository.save(any(Cita.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );

        Cita resultado = citaService.actualizarCita(
                1L,
                datos,
                "cliente@patitas.cl",
                false
        );

        assertEquals(
                "Luna Actualizada",
                resultado.getMascota()
        );

        assertEquals("Nueva consulta", resultado.getMotivo());
        assertEquals("Pendiente", resultado.getEstado());

        verify(citaRepository).save(existente);
    }

    /**
     * TEST 10:
     * Comprueba que una cita pendiente
     * pueda cancelarse sin eliminarse.
     */
    @Test
    void cancelarCitaPropiaCorrectamente() {

        Cita cita = crearCitaEjemplo();

        when(citaRepository.findById(1L))
                .thenReturn(Optional.of(cita));

        when(citaRepository.save(any(Cita.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );

        Cita resultado = citaService.cancelarCita(
                1L,
                "cliente@patitas.cl",
                false
        );

        assertEquals("Cancelada", resultado.getEstado());

        verify(citaRepository).save(cita);
        verify(citaRepository, never())
                .delete(any(Cita.class));
    }

    /**
     * TEST 11:
     * Verifica que una cita completada
     * no pueda cancelarse.
     */
    @Test
    void cancelarCitaCompletadaRetorna409() {

        Cita cita = crearCitaEjemplo();
        cita.setEstado("Completada");

        when(citaRepository.findById(1L))
                .thenReturn(Optional.of(cita));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> citaService.cancelarCita(
                        1L,
                        "cliente@patitas.cl",
                        false
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        verify(citaRepository, never())
                .save(any(Cita.class));
    }

    /**
     * TEST 12:
     * Verifica que una cita Pendiente
     * pueda pasar a Confirmada.
     */
    @Test
    void cambiarEstadoPendienteAConfirmada() {

        Cita cita = crearCitaEjemplo();

        when(citaRepository.findById(1L))
                .thenReturn(Optional.of(cita));

        when(citaRepository.save(any(Cita.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0)
                );

        Cita resultado = citaService.cambiarEstado(
                1L,
                "Confirmada"
        );

        assertEquals("Confirmada", resultado.getEstado());

        verify(citaRepository).save(cita);
    }

    /**
     * TEST 13:
     * Comprueba que no se permita pasar
     * directamente de Pendiente a Completada.
     */
    @Test
    void cambioDeEstadoNoPermitidoRetorna409() {

        Cita cita = crearCitaEjemplo();

        when(citaRepository.findById(1L))
                .thenReturn(Optional.of(cita));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> citaService.cambiarEstado(
                        1L,
                        "Completada"
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        verify(citaRepository, never())
                .save(any(Cita.class));
    }

    /**
     * TEST 14:
     * Comprueba que se pueda eliminar
     * una cita existente.
     *
     * El acceso exclusivo para administradores
     * se configura en CitaController.
     */
    @Test
    void eliminarCitaCorrectamente() {

        Cita cita = crearCitaEjemplo();

        when(citaRepository.findById(1L))
                .thenReturn(Optional.of(cita));

        citaService.eliminarCita(1L);

        verify(citaRepository).delete(cita);
    }
}
