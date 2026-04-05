package com.logitrack.logitrack_api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String trackingId;

    private String estadoAnterior;

    private String estadoNuevo;

    private String usuario;

    private LocalDateTime fechaHora;
}
