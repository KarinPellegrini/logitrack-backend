package com.logitrack.logitrack_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnvioRequestDTO {

    @NotBlank
    private String dni;

    @NotBlank
    private String nombre;

    private String apellido;

    @NotBlank
    private String direccion;

    @NotBlank
    private String codigoPostal;

    @NotNull
    private Double peso;

}