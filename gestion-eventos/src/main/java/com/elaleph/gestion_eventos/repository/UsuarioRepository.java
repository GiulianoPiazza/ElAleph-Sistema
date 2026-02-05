package com.elaleph.gestion_eventos.repository;

import com.elaleph.gestion_eventos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Aquí podrías agregar en el futuro: 
    // Usuario findByEmail(String email); para el login
}