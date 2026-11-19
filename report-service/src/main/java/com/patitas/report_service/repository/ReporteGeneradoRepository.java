package com.patitas.report_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.patitas.report_service.model.ReporteGenerado;

@Repository
public interface ReporteGeneradoRepository extends JpaRepository<ReporteGenerado, Long> {
    List<ReporteGenerado> findAllByOrderByFechaGeneracionDesc(); // Historial: más recientes primero
}
