package com.patitas.auth_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.patitas.auth_service.model.Rol;
import com.patitas.auth_service.repository.RolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;

    public List<Rol> listarRoles() {
        return rolRepository.findAllByOrderByIdAsc();
    }

    /** Recibe "admin", "Admin" o "ADMIN" y devuelve el rol de la base de datos. */
    public Rol obtenerPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new NegocioException("rol", "El rol es obligatorio.");
        return rolRepository.findByNombre(nombre.trim().toUpperCase())
            .orElseThrow(() -> new NegocioException("rol", "El rol '" + nombre + "' no existe."));
    }
}
