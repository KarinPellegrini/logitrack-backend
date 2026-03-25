package com.logitrack.logitrack_api.service;

import com.logitrack.logitrack_api.model.Envio;
import com.logitrack.logitrack_api.repository.EnvioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EnvioService {
    private final EnvioRepository repository;

    public EnvioService(EnvioRepository repository) {
        this.repository = repository;
    }

    public Envio crearEnvio(Envio envio) {
        envio.setTrackingId(UUID.randomUUID().toString());
        envio.setEstado("Registrado");

        return repository.save(envio);
    }

    public List<Envio> obtenerTodos() {
        return repository.findAll();
    }

    public Envio getEnvioByTrackingId(String trackingId) {
        return repository.findByTrackingId(trackingId)
                .orElseThrow(() -> new RuntimeException("Envío no encontrado"));
    }
}
