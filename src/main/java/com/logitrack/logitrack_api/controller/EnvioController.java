package com.logitrack.logitrack_api.controller;

import com.logitrack.logitrack_api.dto.EnvioRequestDTO;
import com.logitrack.logitrack_api.dto.EnvioResponseDTO;
import com.logitrack.logitrack_api.model.Envio;
import com.logitrack.logitrack_api.model.EstadoEnvio;
import com.logitrack.logitrack_api.service.EnvioService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/envios")
@CrossOrigin(origins = "https://logitrack-prototype.vercel.app")
public class EnvioController {
    private final EnvioService service;
    // http://localhost:8080/swagger-ui/index.html

    public EnvioController(EnvioService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Operation(summary = "Crear un nuevo envío")
    @PostMapping
    public EnvioResponseDTO crearEnvio(@Valid @RequestBody EnvioRequestDTO dto) {
        return service.crearEnvio(dto);
    }

    @GetMapping
    public List<EnvioResponseDTO> obtenerTodos() {
        return service.obtenerTodosDTO();
    }

    @Operation(summary = "Obtener envío por trackingId")
    @GetMapping("/{trackingId}")
    public EnvioResponseDTO getEnvioByTrackingId(@PathVariable String trackingId) {
        return service.getEnvioByTrackingIdDTO(trackingId);
    }

    @Operation(summary = "Actualizar el estado de un envío")
    @PutMapping("/{trackingId}/estado")
    public EnvioResponseDTO actualizarEstado(
            @PathVariable String trackingId,
            @RequestParam EstadoEnvio estado,
            @RequestParam(required = false, defaultValue = "sistema") String usuario) {
        return service.actualizarEstadoDTO(trackingId, estado, usuario);
    }

    @Operation(summary = "Buscar envíos por nombre")
    @GetMapping("/buscar")
    public List<EnvioResponseDTO> buscarPorNombre(@RequestParam String nombre) {
        return service.buscarPorNombreDTO(nombre);
    }

    @Operation(summary = "Obtener historial de cambios de estado de un envío")
    @GetMapping("/{trackingId}/historial")
    public ResponseEntity<?> obtenerHistorial(@PathVariable String trackingId) {
        return ResponseEntity.ok(service.obtenerHistorial(trackingId));
    }

    @Operation(summary = "Anonimizar datos personales de un envío (borrado lógico)")
    @PostMapping("/{trackingId}/anonimizar")
    public ResponseEntity<Map<String, Object>> anonimizarDatos(@PathVariable String trackingId) {
        return ResponseEntity.ok(service.anonimizarDatos(trackingId));
    }

    @Operation(summary = "Obtener solicitudes de borrado lógico (supervisor)")
    @GetMapping("/solicitudes-borrado")
    public List<Envio> obtenerSolicitudesBorrado() {
        return service.obtenerSolicitudesBorrado();
    }

    @Operation(summary = "Filtrar envíos por rango de fechas de creación")
    @GetMapping("/por-fecha")
    public List<EnvioResponseDTO> buscarPorFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.buscarPorFechasDTO(desde.atStartOfDay(), hasta.atTime(23, 59, 59));
    }
}
