package com.logitrack.logitrack_api.repository;

import com.logitrack.logitrack_api.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EnvioRepository extends JpaRepository<Envio, Long> {
    Optional<Envio> findByTrackingId(String trackingId);

    @Query("SELECT e FROM Envio e WHERE " +
           "LOWER(e.trackingId) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(e.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(e.apellido) LIKE LOWER(CONCAT('%', :termino, '%'))")
    List<Envio> buscarPorTermino(@Param("termino") String termino);

    List<Envio> findByFechaCreacionBetween(LocalDateTime desde, LocalDateTime hasta);

    List<Envio> findByAnonimizadoTrue();
}
