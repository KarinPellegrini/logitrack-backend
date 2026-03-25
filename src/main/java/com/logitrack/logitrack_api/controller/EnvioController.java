package com.logitrack.logitrack_api.controller;

import com.logitrack.logitrack_api.model.Envio;
import com.logitrack.logitrack_api.model.EstadoEnvio;
import com.logitrack.logitrack_api.service.EnvioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/envios")
public class EnvioController {
    private final EnvioService service;

    public EnvioController(EnvioService service) {
        this.service = service;
    }
    @PostMapping
    public Envio crearEnvio( @Valid @RequestBody Envio envio) {
        return service.crearEnvio(envio);

    }
    @GetMapping
    public List<Envio> obtenerTodos() {
        return service.obtenerTodos();
    }

    @GetMapping("/{trackingId}")
    public Envio getEnvioByTrackingId(@PathVariable String trackingId) {
        return service.getEnvioByTrackingId(trackingId);
    }

    @PutMapping("/{trackingId}/estado")
    public Envio actualizarEstado(
            @PathVariable String trackingId,
            @RequestParam EstadoEnvio estado
    ) {
        return service.actualizarEstado(trackingId, estado);
    }

    @GetMapping("/buscar")
    public List<Envio> buscarPorNombre(@RequestParam String nombre){
        return service.buscarPorNombre(nombre);
    }
}
