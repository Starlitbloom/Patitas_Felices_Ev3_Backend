// Carpeta (paquete) donde vive este archivo: config
package com.patitas.historial_service.config;

// Fecha sin hora; LocalDate.parse convierte "2026-08-12" en fecha
import java.time.LocalDate;

// Entidades y repositorios que se llenan con datos de ejemplo
import com.patitas.historial_service.model.RegistroClinico;
import com.patitas.historial_service.model.RegistroVacuna;
import com.patitas.historial_service.repository.RegistroClinicoRepository;
import com.patitas.historial_service.repository.RegistroVacunaRepository;

// CommandLineRunner: su método run() se ejecuta solo, apenas arranca el servicio
import org.springframework.boot.CommandLineRunner;
// @Component: Spring crea esta clase al iniciar
import org.springframework.stereotype.Component;

// Carga datos de ejemplo la primera vez que se levanta el servicio (solo si las tablas están vacías).
// Los mascotaId (1 a 6) son las mismas mascotas que carga mascota-service.
@Component
public class DataLoader implements CommandLineRunner {

    // Repositorio de atenciones
    private final RegistroClinicoRepository atenciones;
    // Repositorio de vacunas
    private final RegistroVacunaRepository vacunas;

    // Constructor: Spring entrega los repositorios
    public DataLoader(RegistroClinicoRepository atenciones, RegistroVacunaRepository vacunas) {
        this.atenciones = atenciones;
        this.vacunas = vacunas;
    }

    // Se ejecuta al arrancar
    @Override
    public void run(String... args) {
        // Solo carga atenciones si la tabla está vacía (así no se duplican al reiniciar)
        if (atenciones.count() == 0) {
            // Mascota 1: Rocky
            atencion(1L, "2026-08-12", "Control anual", "Paciente sano, sin hallazgos relevantes.", "Sin medicación. Se indica Condrovet forte (condroitín + glucosamina) como suplemento articular preventivo.", "Dra. Paula Vidal");
            atencion(1L, "2026-03-03", "Vómitos ocasionales", "Gastritis leve, probablemente alimentaria.", "Omeprazol 10mg vet cada 24h por 5 días y dieta blanda por 3 días.", "Dr. Sebastian Jimenez");
            // Mascota 2: Luna
            atencion(2L, "2026-07-20", "Control de rutina", "Paciente sana. Pendiente refuerzo de vacuna anual.", "Se agenda vacuna triple felina (Felocell CVR) para el próximo control.", "Dra. Paula Vidal");
            // Mascota 3: Max
            atencion(3L, "2026-08-28", "Picazón y enrojecimiento en la piel", "Dermatitis alérgica leve.", "Apoquel 16mg (oclacitinib) una vez al día y shampoo Malaseb dos veces por semana durante 10 días.", "Dr. Ignacio Rojas");
            atencion(3L, "2026-08-14", "Chequeo previo a tratamiento", "Confirmación de irritación cutánea en zona abdominal.", "Se deriva a control dermatológico y se solicita hemograma completo.", "Dra. Marta López");
            // Mascota 4: Michi
            atencion(4L, "2026-06-02", "Primer control post adopción", "Cachorra sana, buen desarrollo.", "Inicio de plan de vacunación con Nobivac Rabies y desparasitación interna felina.", "Dr. Eduardo Caceres");
            // Mascota 5: Bella
            atencion(5L, "2026-08-18", "Control dental de rutina", "Sin sobrecrecimiento dentario, buen estado general.", "Se recomienda dieta rica en fibra (heno) para desgaste natural de dientes.", "Dra. Marta López");
            // Mascota 6: Nicanor
            atencion(6L, "2026-08-05", "Chequeo aviar general", "Ave activa, plumaje en buen estado, sin signos de enfermedad respiratoria.", "Se indica suplemento vitamínico y control en 6 meses.", "Dr. Sebastian Jimenez");
        }
        // Solo carga vacunas si la tabla está vacía
        if (vacunas.count() == 0) {
            vacuna(1L, "Vacuna sextuple canina (VA002)", "2026-01-10", "2027-01-10");
            vacuna(1L, "Vacuna antirrábica canina (VA001)", "2026-01-15", "2027-01-15");
            vacuna(2L, "Vacuna triple felina (VA004)", "2025-07-05", "2026-07-05");     // Refuerzo vencido -> "atrasada"
            vacuna(3L, "Vacuna sextuple canina (VA002)", "2026-05-20", "2027-05-20");
            vacuna(3L, "Vacuna Bordetella canina (VA005)", "2026-09-18", "2027-09-18");
            vacuna(4L, "Vacuna bivalente felina (VA003)", "2026-06-02", "2026-12-02");  // Vence pronto -> "proxima"
        }
        // Muestra en la consola cuántos registros hay
        System.out.println("[historial-service] Atenciones: " + atenciones.count() + " | Vacunas: " + vacunas.count());
    }

    // Ayudante: crea y guarda una atención
    private void atencion(Long mascotaId, String fecha, String motivo, String diagnostico, String tratamiento, String veterinario) {
        RegistroClinico a = new RegistroClinico();  // Objeto vacío
        a.setMascotaId(mascotaId);                  // Mascota
        a.setFecha(LocalDate.parse(fecha));         // Convierte el texto en fecha
        a.setMotivo(motivo);                        // Motivo
        a.setDiagnostico(diagnostico);              // Diagnóstico
        a.setTratamiento(tratamiento);              // Tratamiento
        a.setVeterinario(veterinario);              // Veterinario
        atenciones.save(a);                         // Guarda en MySQL
    }

    // Ayudante: crea y guarda una vacuna
    private void vacuna(Long mascotaId, String vacuna, String fecha, String proximoRefuerzo) {
        RegistroVacuna v = new RegistroVacuna();                   // Objeto vacío
        v.setMascotaId(mascotaId);                                 // Mascota
        v.setVacuna(vacuna);                                       // Nombre de la vacuna
        v.setFecha(LocalDate.parse(fecha));                        // Fecha de aplicación
        v.setProximoRefuerzo(LocalDate.parse(proximoRefuerzo));    // Fecha del próximo refuerzo
        vacunas.save(v);                                           // Guarda en MySQL
    }
}
