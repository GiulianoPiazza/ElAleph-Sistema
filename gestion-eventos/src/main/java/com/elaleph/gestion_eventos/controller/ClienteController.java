// Aquí es donde definimos los 3 menús

package com.elaleph.gestion_eventos.controller;

import com.elaleph.gestion_eventos.repository.CompraRepository;
import com.elaleph.gestion_eventos.repository.UsuarioRepository;
import com.elaleph.gestion_eventos.repository.EventoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClienteController {


    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private CompraRepository compraRepository; // Al escribir esto, el amarillo de arriba se va

    @Autowired
    private UsuarioRepository usuarioRepository; // Al escribir esto, el otro amarillo se va


    @GetMapping("/cliente/dashboard")
    public String dashboard(Model model) {
        // Por ahora simulamos que el cliente está en el evento ID 1
        model.addAttribute("evento", eventoRepository.findById(1L).orElse(null));
        return "dashboard"; // Esto buscará un archivo HTML llamado dashboard
    }
}