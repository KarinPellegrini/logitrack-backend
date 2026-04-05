package com.logitrack.logitrack_api.service;

import com.logitrack.logitrack_api.dto.EnvioRequestDTO;
import com.logitrack.logitrack_api.dto.EnvioResponseDTO;
import com.logitrack.logitrack_api.model.Envio;
import com.logitrack.logitrack_api.model.EstadoEnvio;
import com.logitrack.logitrack_api.repository.EnvioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@Service
public class EnvioService {

    private final EnvioRepository repository;

    // Lee la URL desde application.properties (que a su vez lee la env var IA_SERVICE_URL)
    @Value("${ia.service.url}")
    private String iaServiceUrl;

    public EnvioService(EnvioRepository repository) {
        this.repository = repository;
    }

    public EnvioResponseDTO crearEnvio(EnvioRequestDTO dto) {
        validarCodigoPostal(dto.getCodigoPostalOrigen(), "origen");
        validarCodigoPostal(dto.getCodigoPostalDestino(), "destino");

        Envio envio = new Envio();
        envio.setTrackingId(UUID.randomUUID().toString());
        envio.setEstado(EstadoEnvio.REGISTRADO);
        envio.setDni(dto.getDni());
        envio.setNombre(dto.getNombre());
        envio.setApellido(dto.getApellido());
        envio.setDireccion(dto.getDireccion());
        envio.setCodigoPostalDestino(dto.getCodigoPostalDestino());
        envio.setCodigoPostalOrigen(dto.getCodigoPostalOrigen());
        envio.setPeso(dto.getPeso());

        Map<String, Object> resultadoIA = consultarIA(dto);
        String prioridad = (String) resultadoIA.getOrDefault("prioridad", "BAJA");
        envio.setPrioridad(prioridad);
        Object distancia = resultadoIA.get("distanciaKm");
        Double distanciaKm = null;
        if (distancia != null) {
            distanciaKm = ((Number) distancia).doubleValue();
            envio.setDistanciaKm(distanciaKm);
        }
        String tipoEnvio = dto.getTipoEnvio() != null ? dto.getTipoEnvio() : "Estandar";
        envio.setTipoEnvio(tipoEnvio);
        envio.setFechaCreacion(LocalDateTime.now());
        envio.setMotivoPrioridad(generarMotivoPrioridad(distanciaKm, dto.getPeso(), tipoEnvio, prioridad));

        repository.save(envio);
        return mapToResponse(envio);
    }

    private Map<String, Object> consultarIA(EnvioRequestDTO dto) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("prioridad", "BAJA");
        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();

            String tipoEnvio = dto.getTipoEnvio() != null ? dto.getTipoEnvio() : "Estandar";

            Map<String, Object> data = new HashMap<>();
            data.put("cp_origen", dto.getCodigoPostalOrigen());
            data.put("cp_destino", dto.getCodigoPostalDestino());
            data.put("peso", dto.getPeso());
            data.put("tipo_envio", tipoEnvio);

            String jsonBody = mapper.writeValueAsString(data);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(iaServiceUrl + "/predict"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> resultMap = mapper.readValue(response.body(), Map.class);
            return resultMap;

        } catch (Exception e) {
            System.err.println("Error de conexión con la IA: " + e.getMessage());
            return fallback;
        }
    }

    private EnvioResponseDTO mapToResponse(Envio envio) {
        EnvioResponseDTO dto = new EnvioResponseDTO();
        dto.setTrackingId(envio.getTrackingId());
        dto.setDni(envio.getDni());
        dto.setNombre(envio.getNombre());
        dto.setApellido(envio.getApellido());
        dto.setDireccion(envio.getDireccion());
        dto.setCodigoPostalOrigen(envio.getCodigoPostalOrigen());
        dto.setCodigoPostalDestino(envio.getCodigoPostalDestino());
        dto.setPeso(envio.getPeso());
        dto.setTipoEnvio(envio.getTipoEnvio());
        dto.setEstado(envio.getEstado());
        dto.setPrioridad(envio.getPrioridad());
        dto.setMotivoPrioridad(envio.getMotivoPrioridad());
        dto.setDistanciaKm(envio.getDistanciaKm());
        dto.setAnonimizado(envio.getAnonimizado());
        dto.setFechaAnonimizacion(envio.getFechaAnonimizacion());
        return dto;
    }

    public List<Envio> obtenerTodos() {
        return repository.findAll();
    }

    public Envio getEnvioByTrackingId(String trackingId) {
        return repository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Envio no encontrado"));
    }

    public Envio actualizarEstado(String trackingId, EstadoEnvio nuevoEstado, String usuario) {
        Envio envio = repository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Envio no encontrado"));
        EstadoEnvio estadoActual = envio.getEstado();
        if (!esTransicionValida(estadoActual, nuevoEstado)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Transicion de estado invalida: " + estadoActual + " -> " + nuevoEstado);
        }
        envio.setEstado(nuevoEstado);
        envio.setFechaCambioEstado(LocalDateTime.now());
        envio.setUsuarioCambioEstado(usuario);
        return repository.save(envio);
    }

    public List<Envio> buscarPorNombre(String termino) {
        return repository.buscarPorTermino(termino);
    }

    public List<Envio> buscarPorRangoFechas(LocalDateTime desde, LocalDateTime hasta) {
        return repository.findByFechaCreacionBetween(desde, hasta);
    }

    private boolean esTransicionValida(EstadoEnvio actual, EstadoEnvio nuevo) {
        return switch (actual) {
            case REGISTRADO -> nuevo == EstadoEnvio.EN_TRANSITO;
            case EN_TRANSITO -> nuevo == EstadoEnvio.EN_SUCURSAL;
            case EN_SUCURSAL -> nuevo == EstadoEnvio.ENTREGADO;
            case ENTREGADO -> false;
        };
    }

    public Map<String, Object> anonimizarDatos(String trackingId) {
        Envio envio = repository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Envío no encontrado."));
        if (Boolean.TRUE.equals(envio.getAnonimizado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Los datos de este envío ya fueron anonimizados.");
        }
        envio.setNombre("[DATO ELIMINADO]");
        envio.setApellido("[DATO ELIMINADO]");
        envio.setDni("[DATO ELIMINADO]");
        envio.setDireccion("[DATO ELIMINADO]");
        envio.setAnonimizado(true);
        envio.setFechaAnonimizacion(LocalDateTime.now());
        repository.save(envio);
        return Map.of("mensaje", "Datos personales anonimizados. El supervisor fue notificado.", "trackingId", trackingId);
    }

    public List<Envio> obtenerSolicitudesBorrado() {
        return repository.findByAnonimizadoTrue();
    }

    private void validarCodigoPostal(String cp, String campo) {
        try {
            int n = Integer.parseInt(cp);
            if (n < 1000 || n > 9499) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El CP de " + campo + " (" + cp + ") está fuera del rango válido argentino (1000-9499).");
            }
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El CP de " + campo + " debe ser numérico.");
        }
    }

    private String generarMotivoPrioridad(Double distanciaKm, Double peso, String tipoEnvio, String prioridad) {
        String distStr = distanciaKm != null ? String.format("%.1f km", distanciaKm) : "distancia no calculada";
        String pesoStr = peso != null ? String.format("%.1f kg", peso) : "peso desconocido";
        boolean esMedico = "Medica".equalsIgnoreCase(tipoEnvio) || "Urgente".equalsIgnoreCase(tipoEnvio);

        String descPeso = (peso == null || peso < 5) ? "liviano" : (peso < 15 ? "moderado" : "de gran volumen");
        String descDist = (distanciaKm == null || distanciaKm < 50) ? "corta distancia" : (distanciaKm < 200 ? "distancia intermedia" : "destino lejano");

        return switch (prioridad != null ? prioridad : "BAJA") {
            case "ALTA" -> esMedico
                ? String.format("Envío de tipo médico/urgente (%s) con %s (%s). Requiere atención prioritaria.", pesoStr, descDist, distStr)
                : String.format("Envío %s (%s) con %s (%s). Requiere atención prioritaria.", descPeso, pesoStr, descDist, distStr);
            case "MEDIA" -> esMedico
                ? String.format("Envío de tipo médico/urgente (%s) a %s (%s). Atención preferente.", pesoStr, descDist, distStr)
                : String.format("Envío %s (%s) a %s (%s). Prioridad estándar.", descPeso, pesoStr, descDist, distStr);
            default -> esMedico
                ? String.format("Envío de tipo médico/urgente (%s) de %s (%s). Requiere manejo especial.", pesoStr, descDist, distStr)
                : String.format("Envío %s (%s) de %s (%s). Sin urgencia especial.", descPeso, pesoStr, descDist, distStr);
        };
    }
}