// Carpeta (paquete) donde vive este archivo: config
package com.patitas.mascota_service.config;

// Trae la entidad Mascota para poder crear objetos de ese tipo
import com.patitas.mascota_service.model.Mascota;
// Trae el repositorio, que es el que guarda en MySQL
import com.patitas.mascota_service.repository.MascotaRepository;

// CommandLineRunner: interfaz de Spring Boot. Lo que pongas en su método run() se ejecuta una vez, justo cuando arranca el servicio
import org.springframework.boot.CommandLineRunner;
// @Component: Spring crea esta clase solo al iniciar (si no, nunca se ejecutaría)
import org.springframework.stereotype.Component;

/**
 * Carga datos de ejemplo la primera vez que se levanta el servicio (si las tablas estan vacias).
 * Es el mismo patron que usa DataInitializer en auth-service.
 */
// Le dice a Spring "crea y ejecuta esta clase automáticamente"
@Component
// Clase que carga las mascotas de ejemplo. "implements CommandLineRunner" = se ejecuta al arrancar la aplicación
public class DataLoader implements CommandLineRunner {

    // Repositorio de mascotas: lo usa para contar y guardar en la tabla "mascota"
    private final MascotaRepository repo;

    // Constructor: Spring entrega el repositorio automáticamente (inyección de dependencias)
    public DataLoader(MascotaRepository repo) {
        // Guarda el repositorio para usarlo en los demás métodos
        this.repo = repo;
    }

    // Método que Spring ejecuta solo al iniciar el servicio. "args" son argumentos de arranque (no se usan aquí)
    @Override
    public void run(String... args) {
        // Cuenta cuántas mascotas hay en la base de datos; si ya hay alguna...
        if (repo.count() > 0) {
            // ...termina aquí y no hace nada, así no se duplican los datos cada vez que reinicias
            return; // ya hay datos: no se duplican
        }
        // Crea y guarda a Rocky, el perro Golden de María. El orden de los datos es el del método crear() de más abajo (nombre, especie, raza, edad, sexo, peso, dueño, correo, completo, imagen, estado, descripción)
        crear("Rocky", "Perro", "Golden Retriever", "3 años", "Macho", "28 kg", "Maria Calfileo Ceballos", "ma.calfileo@duoc.cl", true, "/img/golden.jpg", "al-dia", "Rocky es un perro tranquilo y juguetón. Mantiene sus controles y vacunas al día, sin antecedentes de alergias conocidas.");
        // Crea y guarda a Luna, la gata de María, con vacuna pendiente
        crear("Luna", "Gato", "Común europeo", "2 años", "Hembra", "4 kg", "Maria Calfileo Ceballos", "ma.calfileo@duoc.cl", true, "/img/gataeuropea.jpg", "pendiente", "Luna es una gata independiente y curiosa. Le falta aplicarse el refuerzo anual de su calendario de vacunación.");
        // Crea y guarda a Max, el labrador de Rocío, que está en tratamiento
        crear("Max", "Perro", "Labrador", "5 años", "Macho", "32 kg", "Rocio Cruces", "ro.cruces@duoc.cl", true, "/img/labrador.jpg", "tratamiento", "Max se encuentra en tratamiento por una dermatitis leve. Requiere control de seguimiento en las próximas semanas.");
        // Crea y guarda a Michi, la gata persa de María
        crear("Michi", "Gato", "Persa", "1 año", "Hembra", "3.2 kg", "Maria Calfileo Ceballos", "ma.calfileo@duoc.cl", true, "/img/gatopersa.jpg", "al-dia", "Michi es una gatita joven y sana. Al ser de raza persa, se recomienda vigilar su cuidado respiratorio y ocular.");
        // Crea y guarda a Bella, la coneja de Rocío
        crear("Bella", "Conejo", "Toy enano", "3 meses", "Hembra", "1.1 kg", "Rocio Cruces", "ro.cruces@duoc.cl", true, "/img/conejo.jpg", "al-dia", "Bella es una coneja joven y sana. Al ser de raza toy enano, se recomienda vigilar su cuidado dental y digestivo.");
        // Crea y guarda a Nicanor, la cacatúa de Rocío
        crear("Nicanor", "Ave", "Cacatúa", "1 año", "Macho", "800 gr", "Rocio Cruces", "ro.cruces@duoc.cl", true, "/img/Cacatua.jpg", "al-dia", "Nicanor es un loro curioso y activo. No requiere vacunas dentro del protocolo actual de la clínica; se recomienda control veterinario aviar cada 6 meses.");
        // Escribe en la consola cuántas mascotas quedaron guardadas (para confirmar que funcionó)
        System.out.println("[mascota-service] Mascotas de ejemplo cargadas: " + repo.count());
    }

    // Método ayudante (privado): recibe los 12 datos de una mascota, arma el objeto y lo guarda. Evita repetir el mismo código 6 veces
    private void crear(String nombre, String especie, String raza, String edad, String sexo, String peso, String nombreDueno, String correoDueno, boolean completo, String imagen, String estado, String descripcion) {
        // Crea una mascota vacía (todavía no está en la base de datos)
        Mascota m = new Mascota();
        // Le pone el nombre
        m.setNombre(nombre);
        // Le pone la especie
        m.setEspecie(especie);
        // Le pone la raza
        m.setRaza(raza);
        // Le pone la edad
        m.setEdad(edad);
        // Le pone el sexo
        m.setSexo(sexo);
        // Le pone el peso
        m.setPeso(peso);
        // Le pone el nombre del dueño
        m.setNombreDueno(nombreDueno);
        // Le pone el correo del dueño (con él se vincula con su usuario del login)
        m.setCorreoDueno(correoDueno);
        // Marca si la ficha está completa
        m.setCompleto(completo);
        // Le pone la ruta de la imagen
        m.setImagen(imagen);
        // Le pone el estado de salud
        m.setEstado(estado);
        // Le pone la descripción
        m.setDescripcion(descripcion);
        // Guarda la mascota en MySQL (INSERT). MySQL le asigna el id automáticamente
        repo.save(m);
    }
}