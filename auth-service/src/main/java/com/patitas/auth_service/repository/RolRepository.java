package com.patitas.auth_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.patitas.auth_service.model.Rol;

@Repository // Se comunica con la base de datos
public interface RolRepository extends JpaRepository<Rol, Long> {
    List<Rol> findAllByOrderByIdAsc(); // Roles ordenados por id
    Optional<Rol> findByNombre(String nombre); // Busca un rol por su nombre (ADMIN, CLIENTE...)
}

