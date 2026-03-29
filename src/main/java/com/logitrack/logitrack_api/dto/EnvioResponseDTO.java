package com.logitrack.logitrack_api.dto;

import com.logitrack.logitrack_api.model.EstadoEnvio;
import lombok.Data;

@Data
public class EnvioResponseDTO {

    private String trackingId;

    private String nombre;

    private String apellido;

    private String direccion;

    private EstadoEnvio estado;

    private String prioridad;

}