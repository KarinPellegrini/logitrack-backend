package com.logitrack.logitrack_api.dto;

import com.logitrack.logitrack_api.model.EstadoEnvio;
import lombok.Data;

@Data
public class EnvioResponseDTO {

    private String trackingId;

    private String dni;

    private String nombre;

    private String apellido;

    private String direccion;

    private String codigoPostalOrigen;

    private String codigoPostalDestino;

    private Double peso;

    private String tipoEnvio;

    private EstadoEnvio estado;

    private String prioridad;

    private String motivoPrioridad;

    private Double distanciaKm;

    private Boolean anonimizado;

    private java.time.LocalDateTime fechaAnonimizacion;

    private java.time.LocalDateTime fechaCreacion;

    private java.time.LocalDateTime fechaCambioEstado;

    private String usuarioCambioEstado;

}