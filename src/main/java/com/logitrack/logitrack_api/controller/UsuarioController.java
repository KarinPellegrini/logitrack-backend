package com.logitrack.logitrack_api.controller;

import com.logitrack.logitrack_api.dto.LoginRequestDTO;
import com.logitrack.logitrack_api.dto.RecuperarPasswordRequestDTO;
import com.logitrack.logitrack_api.dto.ResetPasswordRequestDTO;
import com.logitrack.logitrack_api.dto.UsuarioResponseDTO;
import com.logitrack.logitrack_api.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "https://logitrack-prototype.vercel.app")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/api/auth/login")
    public UsuarioResponseDTO login(@RequestBody LoginRequestDTO dto) {
        return service.login(dto);
    }

    @GetMapping("/api/usuarios")
    public List<UsuarioResponseDTO> obtenerTodos() {
        return service.obtenerTodos();
    }

    @PutMapping("/api/usuarios/{id}/rol")
    public UsuarioResponseDTO cambiarRol(@PathVariable Long id, @RequestParam String rol) {
        return service.cambiarRol(id, rol);
    }

    @PostMapping("/api/auth/recuperar")
    public Map<String, String> recuperarPassword(@RequestBody RecuperarPasswordRequestDTO dto) {
        return service.generarTokenRecuperacion(dto);
    }

    @PostMapping("/api/auth/reset-password")
    public ResponseEntity<Void> resetearPassword(@RequestBody ResetPasswordRequestDTO dto) {
        service.resetearPassword(dto);
        return ResponseEntity.ok().build();
    }
}
