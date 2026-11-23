
package com.patitas.novedad_service.repository;

import com.patitas.novedad_service.model.Novedad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NovedadRepository
        extends JpaRepository<Novedad, Long> {
}
