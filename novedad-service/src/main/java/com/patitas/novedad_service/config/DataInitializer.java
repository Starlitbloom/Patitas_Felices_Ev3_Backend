package com.patitas.novedad_service.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.patitas.novedad_service.model.Novedad;
import com.patitas.novedad_service.repository.NovedadRepository;

/**
 * Carga las novedades de ejemplo la primera vez que se inicia el servicio
 * (las mismas publicaciones que mostraba el sitio antes de conectarse al backend).
 * Si la tabla ya tiene datos no hace nada, así no se duplican al reiniciar.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final NovedadRepository repository;

    public DataInitializer(NovedadRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        repository.save(crear(
            "La importancia de mantener las vacunas al día",
            "Mantener actualizado el calendario de vacunación ayuda a proteger a nuestras mascotas frente a distintas enfermedades y contribuye a cuidar su salud durante todas las etapas de su vida.",
            "/img/perrito-inyecion.png",
            "Salud",
            LocalDateTime.of(2026, 9, 5, 10, 0)
        ));
        repository.save(crear(
            "¿Por qué son importantes los controles preventivos?",
            "Las revisiones periódicas ayudan a observar cambios en la salud de tu mascota y permiten mantener un seguimiento de su bienestar.",
            "/img/control-gatito.png",
            "Prevención",
            LocalDateTime.of(2026, 9, 2, 10, 0)
        ));
        repository.save(crear(
            "Consejos para una alimentación equilibrada",
            "Una alimentación adecuada es una parte fundamental del cuidado diario y puede contribuir al bienestar de nuestras mascotas.",
            "/img/alimentacion.png",
            "Bienestar",
            LocalDateTime.of(2026, 8, 28, 10, 0)
        ));
        repository.save(crear(
            "Cómo preparar a tu mascota para su consulta",
            "Algunas acciones sencillas pueden ayudar a que la visita al veterinario sea una experiencia más tranquila para tu mascota.",
            "/img/consulta.png",
            "Consejos",
            LocalDateTime.of(2026, 8, 22, 10, 0)
        ));
    }

    private Novedad crear(String titulo, String descripcion, String imagenUrl,
                          String categoria, LocalDateTime fecha) {
        Novedad novedad = new Novedad();
        novedad.setTitulo(titulo);
        novedad.setDescripcion(descripcion);
        novedad.setImagenUrl(imagenUrl);
        novedad.setCategoria(categoria);
        novedad.setFechaPublicacion(fecha);
        novedad.setActiva(true);
        return novedad;
    }
}
