package com.logitrack.logitrack_api.service;

import com.logitrack.logitrack_api.dto.EnvioRequestDTO;
import com.logitrack.logitrack_api.dto.EnvioResponseDTO;
import com.logitrack.logitrack_api.model.Envio;
import com.logitrack.logitrack_api.model.EstadoEnvio;
import com.logitrack.logitrack_api.repository.EnvioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvioServiceTest {

    @Mock
    private EnvioRepository repository;

    @InjectMocks
    private EnvioService service;

    private EnvioRequestDTO requestValido;
    private Envio envioMock;
    private final String TRACKING_ID_TEST = "e7d1ed34-d877-45c3-8315-ae9e5bd0136e";

    @BeforeEach
    void setUp() {
        // Inicializamos el Request con datos de Argentina
        requestValido = new EnvioRequestDTO();
        requestValido.setDni("32456789");
        requestValido.setNombre("Valentina");
        requestValido.setApellido("Fernández");
        requestValido.setDireccion("Av. San Martín 2456");
        requestValido.setCodigoPostalOrigen("S2000"); // Rosario, Santa Fe
        requestValido.setCodigoPostalDestino("B1640"); // Martínez, Buenos Aires
        requestValido.setPeso(4.85);

        // Inicializamos el objeto de dominio que usaremos en los mocks
        envioMock = new Envio();
        envioMock.setId(1L);
        envioMock.setTrackingId(TRACKING_ID_TEST);
        envioMock.setDni("32456789");
        envioMock.setNombre("Valentina");
        envioMock.setApellido("Fernández");
        envioMock.setDireccion("Av. San Martín 2456");
        envioMock.setCodigoPostalOrigen("S2000");
        envioMock.setCodigoPostalDestino("B1640");
        envioMock.setPeso(4.85);
        envioMock.setEstado(EstadoEnvio.REGISTRADO);
    }

    @Test
    @DisplayName("CP-20: Registro de envío con datos completos")
    void CP20_crearEnvio_DebeRetornarResponseCorrecto() {
        // Arrange
        when(repository.save(any(Envio.class))).thenAnswer(invocation -> {
            Envio envio = invocation.getArgument(0);
            // Simulamos que la DB o el Service genera el UUID si no existe
            if (envio.getTrackingId() == null) {
                envio.setTrackingId(UUID.randomUUID().toString());
            }
            return envio;
        });

        // Act
        EnvioResponseDTO response = service.crearEnvio(requestValido);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getTrackingId());
        // Validamos formato UUID
        assertTrue(response.getTrackingId().matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
        assertEquals("Valentina", response.getNombre());
        verify(repository, times(1)).save(any(Envio.class));
    }

    @Test
    @DisplayName("CP-41: Consulta de envío por Tracking ID válido")
    void CP41_getEnvio_DebeRetornarDetalle() {
        // Arrange
        when(repository.findByTrackingId(TRACKING_ID_TEST)).thenReturn(Optional.of(envioMock));

        // Act
        Envio resultado = service.getEnvioByTrackingId(TRACKING_ID_TEST);

        // Assert
        assertNotNull(resultado);
        assertEquals(TRACKING_ID_TEST, resultado.getTrackingId());
        assertEquals("S2000", resultado.getCodigoPostalOrigen());
    }

    @Test
    @DisplayName("CP-39: Consulta de Tracking ID inexistente")
    void CP39_getEnvio_Inexistente_DebeManejarError() {
        // Arrange
        String idFalso = "ID-INEXISTENTE";
        when(repository.findByTrackingId(idFalso)).thenReturn(Optional.empty());

        // Act & Assert
        // Verificamos que se lance la excepción ResponseStatusException
        org.springframework.web.server.ResponseStatusException exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.getEnvioByTrackingId(idFalso)
        );

        // Opcional: Verificamos que el status sea 404 y el mensaje sea el correcto
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Envio no encontrado"));
    }

    @Test
    @DisplayName("CP-50: Modificación del estado del envío")
    void CP50_actualizarEstado_DebeGuardarCambio() {
        // Arrange
        when(repository.findByTrackingId(TRACKING_ID_TEST)).thenReturn(Optional.of(envioMock));
        // El save devuelve el mismo objeto que recibe
        when(repository.save(any(Envio.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Envio resultado = service.actualizarEstado(TRACKING_ID_TEST, EstadoEnvio.EN_TRANSITO);

        // Assert
        assertNotNull(resultado);
        assertEquals(EstadoEnvio.EN_TRANSITO, resultado.getEstado());
        verify(repository).save(any(Envio.class));
    }
}