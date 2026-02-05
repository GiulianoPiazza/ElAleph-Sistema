package com.elaleph.gestion_eventos.repository;

import com.elaleph.gestion_eventos.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {
    // Aquí podrías agregar luego:
    // List<Compra> findByEstadoPago(String estado); para ver deudores
}