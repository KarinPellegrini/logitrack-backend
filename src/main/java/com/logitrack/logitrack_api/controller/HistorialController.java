package com.logitrack.logitrack_api.controller;

import com.logitrack.logitrack_api.model.HistorialEstado;
import com.logitrack.logitrack_api.repository.HistorialEstadoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historial")
@CrossOrigin(origins = "https://logitrack-prototype.vercel.app")
public class HistorialController {

    private final HistorialEstadoRepository repository;

    public HistorialController(HistorialEstadoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<HistorialEstado>> buscar(
            @RequestParam(required = false, defaultValue = "") String usuario,
            @RequestParam(required = false, defaultValue = "") String accion) {
        return ResponseEntity.ok(repository.buscarPorFiltros(usuario, accion));
    }
}
