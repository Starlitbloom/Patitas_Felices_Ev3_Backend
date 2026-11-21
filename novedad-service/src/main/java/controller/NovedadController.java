
package com.patitas.novedad_service.controller;

import com.patitas.novedad_service.model.Novedad;
import com.patitas.novedad_service.service.NovedadService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/novedades")
public class NovedadController {

    private final NovedadService service;

    public NovedadController(NovedadService service) {
        this.service = service;
    }

    // Listar novedades
    @GetMapping
    public List<Novedad> listar() {
        return service.listar();
    }

    // Buscar novedad por ID
    @GetMapping("/{id}")
    public Novedad buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    // Crear novedad
    @PostMapping
    public ResponseEntity<Novedad> crear(
            @Valid @RequestBody Novedad novedad) {

        Novedad nueva = service.crear(novedad);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nueva);
    }

    // Actualizar novedad
    @PutMapping("/{id}")
    public Novedad actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Novedad novedad) {

        return service.actualizar(id, novedad);
    }

    // Eliminar novedad
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
