package com.elaleph.gestion_eventos.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "eventos")
@Data // Esto genera Getters y Setters automáticamente gracias a Lombok
public class Evento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_evento;
    
    private String nombre;
    private LocalDate fecha;
    private LocalTime hora;
    private String lugar;
}