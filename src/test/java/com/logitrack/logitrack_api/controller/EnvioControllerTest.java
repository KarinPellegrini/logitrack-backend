package com.logitrack.logitrack_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.logitrack_api.dto.EnvioRequestDTO;
import com.logitrack.logitrack_api.dto.EnvioResponseDTO;
import com.logitrack.logitrack_api.model.Envio;
import com.logitrack.logitrack_api.model.EstadoEnvio;
import com.logitrack.logitrack_api.service.EnvioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnvioController.class)
@AutoConfigureMockMvc
class EnvioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnvioService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("CP-20: Creación exitosa de envío con datos completos y Tracking ID único")
    void CP20_crearEnvio_DebeRetornarOk_ConIdDinamico() throws Exception {
        EnvioRequestDTO request = new EnvioRequestDTO();
        request.setDni("32456789");
        request.setNombre("Valentina");
        request.setApellido("Fernández");
        request.setDireccion("Av. San Martín 2456");
        request.setCodigoPostalOrigen("S2000KQE");
        request.setCodigoPostalDestino("C1406GQB");
        request.setPeso(4.85);

        String trackingIdDinamico = UUID.randomUUID().toString();

        EnvioResponseDTO response = new EnvioResponseDTO();
        response.setTrackingId(trackingIdDinamico);
        response.setEstado(EstadoEnvio.REGISTRADO);

        Mockito.when(service.crearEnvio(Mockito.any(EnvioRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingId").value(trackingIdDinamico))
                .andExpect(jsonPath("$.estado").value("REGISTRADO"));
    }

    @Test
    @DisplayName("CP-21: Visualización de datos correctos al buscar envío recién creado")
    void CP21_getEnvioByTrackingId_DebeRetornarEnvio_CuandoIdEsValido() throws Exception {
        String trackingIdDinamico = UUID.randomUUID().toString();

        Envio envioMock = new Envio();
        envioMock.setTrackingId(trackingIdDinamico);
        envioMock.setDni("32456789");
        envioMock.setNombre("Valentina");

        Mockito.when(service.getEnvioByTrackingId(trackingIdDinamico)).thenReturn(envioMock);

        mockMvc.perform(get("/api/envios/" + trackingIdDinamico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingId").value(trackingIdDinamico))
                .andExpect(jsonPath("$.nombre").value("Valentina"));
    }

    @Test
    @DisplayName("CP-23: Error de validación cuando el DNI tiene formato inválido (letras)")
    void CP23_crearEnvio_DniConLetras_DebeRetornarBadRequest() throws Exception {
        EnvioRequestDTO request = new EnvioRequestDTO();
        request.setDni("DNI12345");
        request.setNombre("Test");
        request.setPeso(1.0);

        mockMvc.perform(post("/api/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-25: El sistema marca los campos como requeridos ante datos en blanco")
    void CP25_crearEnvio_Vacio_DebeRetornarBadRequest() throws Exception {
        EnvioRequestDTO requestVacio = new EnvioRequestDTO();

        mockMvc.perform(post("/api/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVacio)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-28: Generación automática de Tracking ID único al guardar")
    void CP28_crearEnvio_DebeRetornar200_YTrackingIdDinamico() throws Exception {
        EnvioRequestDTO request = new EnvioRequestDTO();
        request.setDni("40123456");
        request.setNombre("Karin");
        request.setApellido("Pellegrini"); // Opcional

        // ESTOS CAMPOS SON LOS QUE FALTABAN Y CAUSABAN EL ERROR 400:
        request.setDireccion("Av Siempre Viva 742");
        request.setCodigoPostalOrigen("1667");
        request.setCodigoPostalDestino("1665");

        request.setPeso(2.5);

        String dynamicUuid = UUID.randomUUID().toString();
        EnvioResponseDTO response = new EnvioResponseDTO();
        response.setTrackingId(dynamicUuid);
        response.setEstado(EstadoEnvio.REGISTRADO);

        Mockito.when(service.crearEnvio(Mockito.any(EnvioRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // Ahora sí devolverá 200 porque el DTO es válido
                .andExpect(jsonPath("$.trackingId").value(dynamicUuid));
    }

    @Test
    @DisplayName("CP-33: Cambio de estado de Creado a En Tránsito reflejado correctamente")
    void CP33_actualizarEstado_DebeActualizar_ConIdDinamico() throws Exception {
        String trackingIdDinamico = UUID.randomUUID().toString();

        Envio envioActualizado = new Envio();
        envioActualizado.setTrackingId(trackingIdDinamico);
        envioActualizado.setEstado(EstadoEnvio.EN_TRANSITO);

        Mockito.when(service.actualizarEstado(trackingIdDinamico, EstadoEnvio.EN_TRANSITO))
                .thenReturn(envioActualizado);

        mockMvc.perform(put("/api/envios/" + trackingIdDinamico + "/estado")
                        .param("estado", "EN_TRANSITO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_TRANSITO"));
    }

    @Test
    @DisplayName("CP-34: Bloqueo de cambio de estado no unidireccional (En Tránsito a Creado)")
    void CP34_actualizarEstado_FlujoInvalido_DebeRetornarError() throws Exception {
        String trackingId = UUID.randomUUID().toString();

        Mockito.when(service.actualizarEstado(trackingId, EstadoEnvio.REGISTRADO))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El flujo debe ser unidireccional"));

        mockMvc.perform(put("/api/envios/" + trackingId + "/estado")
                        .param("estado", "REGISTRADO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-35: Actualización exitosa de estado a En Sucursal")
    void CP35_actualizarEstado_AEnSucursal_DebeSerExitoso() throws Exception {
        String trackingId = UUID.randomUUID().toString();
        Envio envio = new Envio();
        envio.setTrackingId(trackingId);
        envio.setEstado(EstadoEnvio.EN_SUCURSAL);

        Mockito.when(service.actualizarEstado(trackingId, EstadoEnvio.EN_SUCURSAL))
                .thenReturn(envio);

        mockMvc.perform(put("/api/envios/" + trackingId + "/estado")
                        .param("estado", "EN_SUCURSAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_SUCURSAL"));
    }

    @Test
    @DisplayName("CP-38: Actualización exitosa de estado de En Sucursal a Entregado")
    void CP38_actualizarEstado_AEntregado_DebeSerExitoso() throws Exception {
        String dynamicUuid = UUID.randomUUID().toString();
        Envio envioEntregado = new Envio();
        envioEntregado.setTrackingId(dynamicUuid);
        envioEntregado.setEstado(EstadoEnvio.ENTREGADO);

        Mockito.when(service.actualizarEstado(dynamicUuid, EstadoEnvio.ENTREGADO))
                .thenReturn(envioEntregado);

        mockMvc.perform(put("/api/envios/" + dynamicUuid + "/estado")
                        .param("estado", "ENTREGADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));
    }

    @Test
    @DisplayName("CP-39: Muestra mensaje de Envió no encontrado ante Tracking ID inexistente")
    void CP39_getEnvioByTrackingId_Inexistente_DebeRetornar404() throws Exception {
        String idFalso = UUID.randomUUID().toString();

        Mockito.when(service.getEnvioByTrackingId(idFalso))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Envio no encontrado"));

        mockMvc.perform(get("/api/envios/" + idFalso))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CP-40: Normalización de Tracking ID a mayúsculas al buscar (Case Insensitive)")
    void CP40_buscarTrackingId_Minusculas_DebeRetornarEnvio() throws Exception {
        String trackingIdUpper = UUID.randomUUID().toString().toUpperCase();
        String trackingIdLower = trackingIdUpper.toLowerCase();

        Envio envioMock = new Envio();
        envioMock.setTrackingId(trackingIdUpper);

        Mockito.when(service.getEnvioByTrackingId(trackingIdLower)).thenReturn(envioMock);

        mockMvc.perform(get("/api/envios/" + trackingIdLower))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingId").value(trackingIdUpper));
    }

    @Test
    @DisplayName("CP-42: Recarga de lista completa al buscar con campos vacíos")
    void CP42_obtenerTodos_DebeRetornarListaCompleta() throws Exception {
        Envio e1 = new Envio();
        e1.setTrackingId(UUID.randomUUID().toString());

        Mockito.when(service.obtenerTodos()).thenReturn(Arrays.asList(e1));

        mockMvc.perform(get("/api/envios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("CP-43: Filtrado de grilla con resultados coincidentes para búsqueda parcial")
    void CP43_buscarPorNombre_Parcial_DebeRetornarListaFiltrada() throws Exception {
        Envio e1 = new Envio();
        e1.setNombre("Valentina");

        Mockito.when(service.obtenerTodos()).thenReturn(Arrays.asList(e1));

        mockMvc.perform(get("/api/envios")
                        .param("nombre", "Valen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Valentina"));
    }
}