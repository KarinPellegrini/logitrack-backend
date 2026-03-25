package com.logitrack.logitrack_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String trackingId;
    private String dni;
    private String nombre;
    private String apellido;
    private String direccion;
    private String codigoPostal;
    private Double peso;
    private String estado;
}
