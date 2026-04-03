package com.logitrack.logitrack_api.controller;

import com.logitrack.logitrack_api.dto.EnvioRequestDTO;
import com.logitrack.logitrack_api.dto.EnvioResponseDTO;
import com.logitrack.logitrack_api.model.Envio;
import com.logitrack.logitrack_api.model.EstadoEnvio;
import com.logitrack.logitrack_api.service.EnvioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@RestController
@RequestMapping("/api/envios")
@CrossOrigin(origins = "http://localhost:5173")
public class EnvioController {
    private final EnvioService service;
    // http://localhost:8080/swagger-ui/index.html

    public EnvioController(EnvioService service) {
        this.service = service;
    }

    @Operation(summary = "Crear un nuevo envío")
    @PostMapping
    public EnvioResponseDTO crearEnvio(@Valid @RequestBody EnvioRequestDTO dto) {
        return service.crearEnvio(dto);
    }

    @GetMapping
    public List<Envio> obtenerTodos() {
        return service.obtenerTodos();
    }

    @Operation(summary = "Obtener envío por trackingId")
    @GetMapping("/{trackingId}")
    public Envio getEnvioByTrackingId(@PathVariable String trackingId) {
        return service.getEnvioByTrackingId(trackingId);
    }

    @Operation(summary = "Actualizar el estado de un envío")
    @PutMapping("/{trackingId}/estado")
    public Envio actualizarEstado(
            @PathVariable String trackingId,
            @RequestParam EstadoEnvio estado) {
        return service.actualizarEstado(trackingId, estado);
    }

    @Operation(summary = "Buscar envíos por nombre")
    @GetMapping("/buscar")
    public List<Envio> buscarPorNombre(@RequestParam String nombre) {
        return service.buscarPorNombre(nombre);
    }
}
