package com.elaleph.gestion_eventos.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_usuario;
    
    private String nombre_completo;
    private String email;
    private String password;
    
    @ManyToOne
    @JoinColumn(name = "id_evento")
    private Evento evento;
}