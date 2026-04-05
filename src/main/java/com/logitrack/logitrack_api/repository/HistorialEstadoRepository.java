package com.logitrack.logitrack_api.repository;

import com.logitrack.logitrack_api.model.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {
    List<HistorialEstado> findByTrackingIdOrderByFechaHoraAsc(String trackingId);

    List<HistorialEstado> findTop10ByOrderByFechaHoraDesc();

    @Query("SELECT h.usuario, COUNT(h) FROM HistorialEstado h GROUP BY h.usuario ORDER BY COUNT(h) DESC")
    List<Object[]> contarPorUsuario();

    @Query("SELECT h FROM HistorialEstado h WHERE " +
           "(:usuario IS NULL OR :usuario = '' OR LOWER(h.usuario) LIKE LOWER(CONCAT('%', :usuario, '%'))) AND " +
           "(:accion IS NULL OR :accion = '' OR h.estadoNuevo = :accion) " +
           "ORDER BY h.fechaHora DESC")
    List<HistorialEstado> buscarPorFiltros(@Param("usuario") String usuario, @Param("accion") String accion);
}
