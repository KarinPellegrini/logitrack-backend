package com.logitrack.logitrack_api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String usuario;

    @Column(nullable = false)
    private String clave;

    private String nombre;
    private String apellido;

    @Column(nullable = false)
    private String rol;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;
}
