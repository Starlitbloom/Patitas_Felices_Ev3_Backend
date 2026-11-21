
package com.patitas.novedad_service.service;

import com.patitas.novedad_service.model.Novedad;
import com.patitas.novedad_service.repository.NovedadRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
public class NovedadService {

    private final NovedadRepository repository;

    public NovedadService(NovedadRepository repository) {
        this.repository = repository;
    }

    // Listar todas las novedades
    public List<Novedad> listar() {
        return repository.findAll();
    }

    // Buscar una novedad por ID
    public Novedad buscarPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Novedad no encontrada"
            ));
    }

    // Registrar una nueva novedad
    public Novedad crear(Novedad novedad) {
        novedad.setId(null);
        return repository.save(novedad);
    }

    // Actualizar una novedad
    public Novedad actualizar(Long id, Novedad datos) {
        Novedad novedad = buscarPorId(id);

        novedad.setTitulo(datos.getTitulo());
        novedad.setDescripcion(datos.getDescripcion());
        novedad.setImagenUrl(datos.getImagenUrl());
        novedad.setActiva(datos.getActiva());

        return repository.save(novedad);
    }

    // Eliminar una novedad
    public void eliminar(Long id) {
        Novedad novedad = buscarPorId(id);
        repository.delete(novedad);
    }
}
