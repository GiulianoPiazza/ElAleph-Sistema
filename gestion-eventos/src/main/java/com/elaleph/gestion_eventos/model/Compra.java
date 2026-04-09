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

    private String nombre_evento;
    
    @Column(columnDefinition = "TEXT")
    private String lista_invitados;

    private String clasificacion;
    private Integer cantidad;
    private Double monto_total;
    private String forma_pago;

    // NUEVO CAMPO PARA LA FOTO
    @Column(columnDefinition = "LONGTEXT")
    private String comprobante;

    private LocalDateTime fecha_compra = LocalDateTime.now();
    private String estado_pago;
}