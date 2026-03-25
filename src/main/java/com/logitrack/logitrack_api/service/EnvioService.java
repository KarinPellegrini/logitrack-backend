package com.logitrack.logitrack_api.service;

import com.logitrack.logitrack_api.model.Envio;
import com.logitrack.logitrack_api.model.EstadoEnvio;
import com.logitrack.logitrack_api.repository.EnvioRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
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
        envio.setEstado(EstadoEnvio.REGISTRADO);
        return repository.save(envio);
    }

    public List<Envio> obtenerTodos() {
        return repository.findAll();
    }

    public Envio getEnvioByTrackingId(String trackingId) {
        return repository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Envio no encontrado"
                ));
    }

    public Envio actualizarEstado(String trackingId, EstadoEnvio nuevoEstado){
        Envio envio = repository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Envio no encontrado"
                ));
        EstadoEnvio estadoActual = envio.getEstado();
        if(!esTransicionValida(estadoActual, nuevoEstado)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Transicion de estado invalida: " + estadoActual + " -> " + nuevoEstado
            );
        }
        envio.setEstado(nuevoEstado);
        return repository.save(envio);
    }

    public List<Envio> buscarPorNombre(String nombre) {
        return repository.findByNombreContainingIgnoreCase(nombre);
    }

    private boolean esTransicionValida(EstadoEnvio actual, EstadoEnvio nuevo) {

        return switch (actual) {
            case REGISTRADO -> nuevo == EstadoEnvio.EN_TRANSITO;
            case EN_TRANSITO -> nuevo == EstadoEnvio.EN_SUCURSAL;
            case EN_SUCURSAL -> nuevo == EstadoEnvio.ENTREGADO;
            case ENTREGADO -> false;
        };
    }
}
