// Carpeta (paquete) del test: la misma que la del service que se prueba
package com.patitas.historial_service.service;

// Comprobaciones de JUnit
import static org.junit.jupiter.api.Assertions.assertEquals;   // ¿Son iguales?
import static org.junit.jupiter.api.Assertions.assertThrows;   // ¿Lanza este error?
// Herramientas de Mockito
import static org.mockito.ArgumentMatchers.any;                // "Cualquier objeto de este tipo"
import static org.mockito.Mockito.verify;                      // ¿Se llamó este método?
import static org.mockito.Mockito.when;                        // "Cuando se llame X, responde Y"

// Fecha sin hora
import java.time.LocalDate;
// Tipo "lista"
import java.util.List;
// Caja que puede tener un valor o estar vacía (lo devuelve findById)
import java.util.Optional;

// @Test: marca un método como prueba
import org.junit.jupiter.api.Test;
// Conecta JUnit con Mockito
import org.junit.jupiter.api.extension.ExtendWith;
// @InjectMocks: crea el service real con los mocks dentro
import org.mockito.InjectMocks;
// @Mock: crea un objeto falso (no toca la base de datos)
import org.mockito.Mock;
// Extensión de Mockito para JUnit 5
import org.mockito.junit.jupiter.MockitoExtension;

// Clases del proyecto que se usan en las pruebas
import com.patitas.historial_service.dto.AtencionRequest;
import com.patitas.historial_service.dto.VacunaRequest;
import com.patitas.historial_service.exception.ConflictoException;
import com.patitas.historial_service.exception.RecursoNoEncontradoException;
import com.patitas.historial_service.model.RegistroClinico;
import com.patitas.historial_service.model.RegistroVacuna;
import com.patitas.historial_service.repository.RegistroClinicoRepository;
import com.patitas.historial_service.repository.RegistroVacunaRepository;

// Pruebas unitarias del historial: los repositorios son falsos (Mockito), no se necesita MySQL
@ExtendWith(MockitoExtension.class)
class HistorialServiceTest {

    @Mock RegistroClinicoRepository atenciones;  // Repositorio falso de atenciones
    @Mock RegistroVacunaRepository vacunas;      // Repositorio falso de vacunas
    @InjectMocks HistorialService servicio;      // Service real que usa los repositorios falsos

    // Ayudante: una atención válida de ejemplo (el motivo trae espacios de más a propósito)
    private AtencionRequest atencion() {
        return new AtencionRequest(LocalDate.of(2026, 8, 12), "  Control anual ", "Paciente sano", "Sin medicacion",
            "Dra. Paula Vidal", List.of("Hemograma completo"));
    }

    // Prueba 1: crear una atención la asocia a la mascota y limpia los espacios
    @Test
    void crearAtencionAsociaLaMascotaYLimpiaLosTextos() {
        // Cuando se guarde, el repositorio falso devuelve el mismo objeto
        when(atenciones.save(any(RegistroClinico.class))).thenAnswer(inv -> inv.getArgument(0));
        // Crea la atención para la mascota 7
        RegistroClinico r = servicio.crearAtencion(7L, atencion());
        assertEquals(7L, r.getMascotaId());                            // Quedó asociada a la mascota 7
        assertEquals("Control anual", r.getMotivo());                  // Se quitaron los espacios sobrantes
        assertEquals(List.of("Hemograma completo"), r.getServicios()); // Se copiaron los servicios
    }

    // Prueba 2: actualizar una atención cambia sus datos
    @Test
    void actualizarAtencionCambiaLosDatos() {
        RegistroClinico existente = new RegistroClinico();  // Atención que "ya existe"
        existente.setId(3L);
        existente.setMotivo("Antiguo");
        when(atenciones.findById(3L)).thenReturn(Optional.of(existente));                   // Al buscar la 3, la encuentra
        when(atenciones.save(any(RegistroClinico.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("Control anual", servicio.actualizarAtencion(3L, atencion()).getMotivo()); // El motivo cambió
    }

    // Prueba 3: pedir o borrar una atención que no existe lanza 404
    @Test
    void atencionInexistenteLanza404() {
        when(atenciones.findById(99L)).thenReturn(Optional.empty());  // La 99 no existe
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerAtencion(99L));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminarAtencion(99L));
    }

    // Prueba 4: eliminar una atención existente llama a delete del repositorio
    @Test
    void eliminarAtencionExistenteLaBorra() {
        RegistroClinico existente = new RegistroClinico();
        existente.setId(3L);
        when(atenciones.findById(3L)).thenReturn(Optional.of(existente));
        servicio.eliminarAtencion(3L);
        verify(atenciones).delete(existente);  // Comprueba que sí se borró
    }

    // Prueba 5: regla del estado de la vacuna según la fecha del refuerzo
    @Test
    void estadoDeVacunaSegunElRefuerzo() {
        LocalDate hoy = LocalDate.of(2026, 11, 18);
        assertEquals("atrasada", HistorialService.calcularEstado(hoy.minusDays(1), hoy)); // Venció ayer
        assertEquals("proxima", HistorialService.calcularEstado(hoy.plusDays(30), hoy));  // Faltan 30 días
        assertEquals("proxima", HistorialService.calcularEstado(hoy, hoy));               // Vence hoy
        assertEquals("al-dia", HistorialService.calcularEstado(hoy.plusDays(31), hoy));   // Faltan 31 días
    }

    // Prueba 6: no se puede registrar un refuerzo anterior a la aplicación
    @Test
    void vacunaConRefuerzoAnteriorALaAplicacionSeRechaza() {
        VacunaRequest mala = new VacunaRequest("Triple felina", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1));
        assertThrows(ConflictoException.class, () -> servicio.crearVacuna(1L, mala));
    }

    // Prueba 7: una vacuna válida se guarda asociada a la mascota y con el nombre limpio
    @Test
    void crearVacunaValidaSeGuarda() {
        when(vacunas.save(any(RegistroVacuna.class))).thenAnswer(inv -> inv.getArgument(0));
        VacunaRequest ok = new VacunaRequest(" Antirrabica ", LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15));
        RegistroVacuna v = servicio.crearVacuna(2L, ok);
        assertEquals(2L, v.getMascotaId());      // Asociada a la mascota 2
        assertEquals("Antirrabica", v.getVacuna()); // Sin espacios sobrantes
    }

    // Prueba 8: actualizar una vacuna cambia sus fechas y mantiene la mascota
    @Test
    void actualizarVacunaCambiaLosDatos() {
        RegistroVacuna existente = new RegistroVacuna();  // Vacuna que "ya existe"
        existente.setId(4L);
        existente.setMascotaId(2L);
        existente.setVacuna("Triple felina");
        when(vacunas.findById(4L)).thenReturn(Optional.of(existente));
        when(vacunas.save(any(RegistroVacuna.class))).thenAnswer(inv -> inv.getArgument(0));
        VacunaRequest nueva = new VacunaRequest("Triple felina", LocalDate.of(2026, 7, 5), LocalDate.of(2027, 7, 5));
        RegistroVacuna v = servicio.actualizarVacuna(4L, nueva);
        assertEquals(LocalDate.of(2027, 7, 5), v.getProximoRefuerzo()); // Se cambió el refuerzo
        assertEquals(2L, v.getMascotaId());                             // La mascota no cambia
    }

    // Prueba 9: actualizar con fechas inválidas también se rechaza
    @Test
    void actualizarVacunaConFechasInvalidasSeRechaza() {
        VacunaRequest mala = new VacunaRequest("Triple felina", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 1));
        assertThrows(ConflictoException.class, () -> servicio.actualizarVacuna(4L, mala));
    }
}
