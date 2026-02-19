package com.elaleph.gestion_eventos.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuarios")
@Data // Esto genera automáticamente el getRol(), getId_usuario(), etc.
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_usuario;

    private String nombre_completo;
    private String email;
    private String password;
    
    // Este es el nombre que debe coincidir con la base de datos
    private String rol; 

    // Este es el ID del evento al que pertenece el usuario
    private Long id_evento;
}