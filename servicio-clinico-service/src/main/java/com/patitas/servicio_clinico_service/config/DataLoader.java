// Carpeta (paquete) donde vive este archivo: config
package com.patitas.servicio_clinico_service.config;

// Lista que se puede modificar
import java.util.ArrayList;
// Tipo "lista" de Java
import java.util.List;

// Entidad y repositorio que se llenan con datos de ejemplo
import com.patitas.servicio_clinico_service.model.ServicioClinico;
import com.patitas.servicio_clinico_service.repository.ServicioClinicoRepository;

// CommandLineRunner: su método run() se ejecuta solo, apenas arranca el servicio
import org.springframework.boot.CommandLineRunner;
// @Component: Spring crea esta clase al iniciar
import org.springframework.stereotype.Component;

// Carga el catálogo de ejemplo la primera vez que se levanta el servicio (solo si la tabla está vacía).
// Son los mismos servicios que tenía el frontend de la EP2.
@Component
public class DataLoader implements CommandLineRunner {

    // Repositorio del catálogo
    private final ServicioClinicoRepository repo;

    // Constructor: Spring entrega el repositorio
    public DataLoader(ServicioClinicoRepository repo) {
        this.repo = repo;
    }

    // Se ejecuta al arrancar
    @Override
    public void run(String... args) {
        // Si ya hay datos, no hace nada (así no se duplican al reiniciar)
        if (repo.count() > 0) {
            return;
        }
        // Cirugías (después del nombre van las especies; sin especies = todas)
        crear("Cirugía", "Esterilización hembra canina", "Perro");
        crear("Cirugía", "Esterilización macho canino", "Perro");
        crear("Cirugía", "Esterilización hembra felina", "Gato");
        crear("Cirugía", "Esterilización macho felino", "Gato");
        crear("Cirugía", "Esterilización hembra conejo", "Conejo");
        crear("Cirugía", "Esterilización macho conejo", "Conejo");
        crear("Cirugía", "Extirpación de tumor cutáneo", "Perro", "Gato", "Conejo");
        crear("Cirugía", "Cesárea de urgencia", "Perro", "Gato", "Conejo");
        // Desparasitaciones
        crear("Desparasitación", "Desparasitación interna pequeños", "Perro");
        crear("Desparasitación", "Desparasitación interna medianos", "Perro");
        crear("Desparasitación", "Desparasitación interna grandes", "Perro");
        crear("Desparasitación", "Desparasitación interna felina", "Gato");
        crear("Desparasitación", "Desparasitación interna conejo", "Conejo");
        crear("Desparasitación", "Desparasitación interna ave", "Ave");
        crear("Desparasitación", "Desparasitación externa");
        // Exámenes
        crear("Exámenes", "Hemograma completo");
        crear("Exámenes", "Perfil bioquímico completo");
        crear("Exámenes", "Radiografía");
        crear("Exámenes", "Ecografía abdominal");
        crear("Exámenes", "Test de leishmaniasis", "Perro", "Gato", "Conejo");
        // Otros
        crear("Otros", "Corte de uñas");
        crear("Otros", "Limpieza dental", "Perro", "Gato", "Conejo");
        crear("Otros", "Microchip identificación", "Perro", "Gato");
        crear("Otros", "Hospitalización");
        // Muestra en la consola cuántos servicios quedaron
        System.out.println("[servicio-clinico-service] Servicios de ejemplo cargados: " + repo.count());
    }

    // Ayudante: crea y guarda un servicio. "String... especies" = se pueden pasar 0, 1 o varias especies
    private void crear(String grupo, String nombre, String... especies) {
        ServicioClinico s = new ServicioClinico();               // Objeto vacío (activo = true por defecto)
        s.setGrupo(grupo);                                       // Grupo
        s.setNombre(nombre);                                     // Nombre
        s.setEspecies(new ArrayList<>(List.of(especies)));       // Especies (lista modificable)
        repo.save(s);                                            // Guarda en MySQL
    }
}
