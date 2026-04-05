package com.logitrack.logitrack_api.service;

import com.logitrack.logitrack_api.dto.EnvioRequestDTO;
import com.logitrack.logitrack_api.dto.EnvioResponseDTO;
import com.logitrack.logitrack_api.model.Envio;
import com.logitrack.logitrack_api.model.EstadoEnvio;
import com.logitrack.logitrack_api.model.HistorialEstado;
import com.logitrack.logitrack_api.repository.EnvioRepository;
import com.logitrack.logitrack_api.repository.HistorialEstadoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@Service
public class EnvioService {

    private final EnvioRepository repository;
    private final HistorialEstadoRepository historialRepository;

    // Lee la URL desde application.properties (que a su vez lee la env var IA_SERVICE_URL)
    @Value("${ia.service.url}")
    private String iaServiceUrl;

    public EnvioService(EnvioRepository repository, HistorialEstadoRepository historialRepository) {
        this.repository = repository;
        this.historialRepository = historialRepository;
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
        fallback.put("distanciaKm", 300.0);
        try {
            System.out.println("[IA] iaServiceUrl configurado: " + iaServiceUrl);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            ObjectMapper mapper = new ObjectMapper();

            String tipoEnvio = dto.getTipoEnvio() != null ? dto.getTipoEnvio() : "Estandar";

            Map<String, Object> data = new HashMap<>();
            data.put("cp_origen", dto.getCodigoPostalOrigen());
            data.put("cp_destino", dto.getCodigoPostalDestino());
            data.put("peso", dto.getPeso());
            data.put("tipo_envio", tipoEnvio);

            String jsonBody = mapper.writeValueAsString(data);
            String urlBase = iaServiceUrl.trim();
            System.out.println("[IA] Llamando a: " + urlBase + "/predict");
            System.out.println("[IA] Body enviado: " + jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlBase + "/predict"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[IA] HTTP status: " + response.statusCode());
            System.out.println("[IA] Respuesta: " + response.body());

            Map<String, Object> resultMap = mapper.readValue(response.body(), Map.class);
            return resultMap;

        } catch (Exception e) {
            System.err.println("[IA] ERROR tipo: " + e.getClass().getName());
            System.err.println("[IA] ERROR mensaje: " + e.getMessage());
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
        dto.setFechaCreacion(envio.getFechaCreacion());
        dto.setFechaCambioEstado(envio.getFechaCambioEstado());
        dto.setUsuarioCambioEstado(envio.getUsuarioCambioEstado());
        dto.setProbabilidadRetraso(calcularProbabilidadRetraso(envio));
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
        LocalDateTime ahora = LocalDateTime.now();
        envio.setEstado(nuevoEstado);
        envio.setFechaCambioEstado(ahora);
        envio.setUsuarioCambioEstado(usuario);
        repository.save(envio);

        HistorialEstado registro = new HistorialEstado();
        registro.setTrackingId(trackingId);
        registro.setEstadoAnterior(estadoActual.name());
        registro.setEstadoNuevo(nuevoEstado.name());
        registro.setUsuario(usuario);
        registro.setFechaHora(ahora);
        historialRepository.save(registro);

        return envio;
    }

    public EnvioResponseDTO actualizarEstadoDTO(String trackingId, EstadoEnvio nuevoEstado, String usuario) {
        return mapToResponse(actualizarEstado(trackingId, nuevoEstado, usuario));
    }

    public List<EnvioResponseDTO> obtenerTodosDTO() {
        return repository.findAll().stream().map(this::mapToResponse).toList();
    }

    public EnvioResponseDTO getEnvioByTrackingIdDTO(String trackingId) {
        return mapToResponse(getEnvioByTrackingId(trackingId));
    }

    public List<EnvioResponseDTO> buscarPorNombreDTO(String termino) {
        return repository.buscarPorTermino(termino).stream().map(this::mapToResponse).toList();
    }

    public List<EnvioResponseDTO> buscarPorFechasDTO(LocalDateTime desde, LocalDateTime hasta) {
        return repository.findByFechaCreacionBetween(desde, hasta).stream().map(this::mapToResponse).toList();
    }

    public List<HistorialEstado> obtenerHistorial(String trackingId) {
        return historialRepository.findByTrackingIdOrderByFechaHoraAsc(trackingId);
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

    private int calcularProbabilidadRetraso(Envio envio) {
        if (envio.getEstado() == EstadoEnvio.ENTREGADO) return 0;

        int prob = 10;

        // Prioridad asignada por IA
        String prioridad = envio.getPrioridad() != null ? envio.getPrioridad() : "BAJA";
        prob += switch (prioridad) {
            case "ALTA"  -> 35;
            case "MEDIA" -> 20;
            default      -> 5;
        };

        // Peso del paquete
        double peso = envio.getPeso() != null ? envio.getPeso() : 0;
        if      (peso > 50) prob += 15;
        else if (peso > 15) prob += 10;
        else if (peso > 5)  prob += 5;

        // Distancia estimada
        double dist = envio.getDistanciaKm() != null ? envio.getDistanciaKm() : 300;
        if      (dist > 500) prob += 15;
        else if (dist > 200) prob += 10;
        else if (dist > 50)  prob += 5;

        // Tipo de envío (mayor complejidad operativa)
        String tipo = envio.getTipoEnvio() != null ? envio.getTipoEnvio() : "";
        if      ("Peligrosa".equalsIgnoreCase(tipo)) prob += 15;
        else if ("Medica".equalsIgnoreCase(tipo))    prob += 10;
        else if ("Fragil".equalsIgnoreCase(tipo))    prob += 5;

        // Estado actual (EN_SUCURSAL implica espera adicional)
        if      (envio.getEstado() == EstadoEnvio.EN_SUCURSAL) prob += 10;
        else if (envio.getEstado() == EstadoEnvio.EN_TRANSITO) prob += 5;

        return Math.min(95, Math.max(5, prob));
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