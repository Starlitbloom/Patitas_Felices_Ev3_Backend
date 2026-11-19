package com.patitas.report_service.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registro (auditoría) de cada reporte generado: quién lo pidió, cuándo,
 * un resumen de las cifras y qué servicios no respondieron en ese momento.
 */
@Data
@Entity
@Table(name = "reportes_generados")
@NoArgsConstructor
@AllArgsConstructor
public class ReporteGenerado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoReporte tipo;

    @Column(nullable = false, length = 100)
    private String generadoPor; // Correo del admin (sale del token)

    @Column(length = 300)
    private String resumen; // Ej: "8 usuarios · 12 mascotas · 20 citas"

    @Column(length = 300)
    private String serviciosNoDisponibles; // Ej: "mascota-service, cita-service" (vacío si todos respondieron)

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaGeneracion;
}
