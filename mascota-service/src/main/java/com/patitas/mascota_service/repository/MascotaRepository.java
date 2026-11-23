package com.patitas.mascota_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.patitas.mascota_service.model.Mascota;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    //Mascotas de un dueno (las busca por su correo).
    List<Mascota> findByCorreoDuenoIgnoreCase(String correoDueno);


}
