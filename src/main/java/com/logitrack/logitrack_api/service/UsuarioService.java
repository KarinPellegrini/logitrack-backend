package com.logitrack.logitrack_api.service;

import com.logitrack.logitrack_api.dto.LoginRequestDTO;
import com.logitrack.logitrack_api.dto.RecuperarPasswordRequestDTO;
import com.logitrack.logitrack_api.dto.ResetPasswordRequestDTO;
import com.logitrack.logitrack_api.dto.UsuarioResponseDTO;
import com.logitrack.logitrack_api.model.Usuario;
import com.logitrack.logitrack_api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public UsuarioResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = repository.findByUsuario(dto.getUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));

        if (!encoder.matches(dto.getClave(), usuario.getClave())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        return mapToResponse(usuario);
    }

    public List<UsuarioResponseDTO> obtenerTodos() {
        return repository.findAll().stream().map(this::mapToResponse).toList();
    }

    public UsuarioResponseDTO cambiarRol(Long id, String nuevoRol) {
        if (!nuevoRol.equals("Operador") && !nuevoRol.equals("Supervisor")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol invalido: " + nuevoRol);
        }
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        usuario.setRol(nuevoRol);
        return mapToResponse(repository.save(usuario));
    }

    public Map<String, String> generarTokenRecuperacion(RecuperarPasswordRequestDTO dto) {
        Usuario usuario = repository.findByUsuario(dto.getUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String token = UUID.randomUUID().toString();
        usuario.setResetToken(token);
        usuario.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        repository.save(usuario);

        // En producción aquí se enviaría el email. Para el TP se retorna el token.
        return Map.of("token", token, "expiraEn", "30 minutos");
    }

    public void resetearPassword(ResetPasswordRequestDTO dto) {
        Usuario usuario = repository.findAll().stream()
                .filter(u -> dto.getToken().equals(u.getResetToken()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido"));

        if (usuario.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token expiró");
        }

        usuario.setClave(encoder.encode(dto.getNuevaClave()));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiry(null);
        repository.save(usuario);
    }

    private UsuarioResponseDTO mapToResponse(Usuario u) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(u.getId());
        dto.setUsuario(u.getUsuario());
        dto.setNombre(u.getNombre());
        dto.setApellido(u.getApellido());
        dto.setRol(u.getRol());
        return dto;
    }
}
