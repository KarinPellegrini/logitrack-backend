package com.logitrack.logitrack_api.repository;

import com.logitrack.logitrack_api.model.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {
    List<HistorialEstado> findByTrackingIdOrderByFechaHoraAsc(String trackingId);
}
