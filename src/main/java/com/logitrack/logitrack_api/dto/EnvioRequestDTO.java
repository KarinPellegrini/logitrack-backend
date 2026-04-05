package com.logitrack.logitrack_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EnvioRequestDTO {

    @NotBlank(message = "El DNI es obligatorio.")
    @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos numéricos.")
    private String dni;

    @NotBlank(message = "El nombre es obligatorio.")
    @Pattern(regexp = "[a-zA-ZÁÉÍÓÚáéíóúÑñ\\s]+", message = "El nombre solo puede contener letras.")
    private String nombre;

    @Pattern(regexp = "[a-zA-ZÁÉÍÓÚáéíóúÑñ\\s]+", message = "El apellido solo puede contener letras.")
    private String apellido;

    @NotBlank(message = "La dirección es obligatoria.")
    private String direccion;

    @NotBlank(message = "El código postal de destino es obligatorio.")
    @Pattern(regexp = "\\d{4}", message = "El CP debe tener exactamente 4 dígitos.")
    private String codigoPostalDestino;

    @NotBlank(message = "El código postal de origen es obligatorio.")
    @Pattern(regexp = "\\d{4}", message = "El CP debe tener exactamente 4 dígitos.")
    private String codigoPostalOrigen;

    @NotNull(message = "El peso es obligatorio.")
    private Double peso;

    private String tipoEnvio;

    private String prioridad;

}
