package com.elaleph.gestion_eventos.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "compras")
@Data
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_compra;
    
    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;
    
    private String clasificacion; // Adulto, Menor, etc.
    private Integer cantidad;
    private Double monto_total;
    private String forma_pago;
    private LocalDateTime fecha_compra = LocalDateTime.now();
    private String estado_pago = "PENDIENTE"; 
}