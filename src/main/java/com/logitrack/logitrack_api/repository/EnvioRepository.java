package com.logitrack.logitrack_api.repository;

import com.logitrack.logitrack_api.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnvioRepository extends JpaRepository<Envio, Long> {
    Optional<Envio> findByTrackingId(String trackingId);
}
