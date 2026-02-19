package com.elaleph.gestion_eventos.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "eventos")
@Data 
public class Evento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_evento;
    
    private String nombre;
    private LocalDate fecha;
    private LocalTime hora;
    private String lugar;
    private Boolean activo = true;

    // Nuevos campos para precios dinámicos
    private Double precio_adulto;
    private Double precio_menor;
    private Double precio_fiesta;
}