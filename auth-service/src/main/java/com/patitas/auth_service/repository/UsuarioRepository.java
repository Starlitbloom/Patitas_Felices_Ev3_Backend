package com.patitas.auth_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.patitas.auth_service.model.Usuario;

@Repository // Se comunica con la base de datos
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo); // Login y búsqueda por correo
    boolean existsByCorreoIgnoreCase(String correo); // MEtodo para buscar por correo electrónico
    boolean existsByRun(String run); // El RUN ya está registrado
    List<Usuario> findAllByOrderByFechaRegistroDesc(); // Más recientes primero (panel admin)
    List<Usuario> findByRol_Nombre(String nombreRol); // Usuarios de un rol
    long countByActivoTrue(); // KPI: usuarios activos
    long countByFechaRegistroAfter(LocalDateTime fecha); // KPI: nuevos esta semana
}
