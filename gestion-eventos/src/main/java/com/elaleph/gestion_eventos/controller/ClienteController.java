// Aquí es donde definimos los 3 menús

package com.elaleph.gestion_eventos.controller;

import com.elaleph.gestion_eventos.repository.CompraRepository;
import com.elaleph.gestion_eventos.repository.UsuarioRepository;
import com.elaleph.gestion_eventos.repository.EventoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    // 1. Buscamos el evento (ID 1 por ahora)
    model.addAttribute("evento", eventoRepository.findById(1L).orElse(null));
    
    // 2. Buscamos todas las compras (esto luego lo filtraremos por usuario)
    model.addAttribute("misCompras", compraRepository.findAll());
    
    return "dashboard";
}

// --- AGREGA ESTO DEBAJO DEL MÉTODO DASHBOARD ---

    @PostMapping("/cliente/comprar")
    public String procesarCompra(
            @RequestParam Long idUsuario,
            @RequestParam String clasificacion,
            @RequestParam Integer cantidad,
            @RequestParam String formaPago,
            @RequestParam Double precioUnitario) {

        com.elaleph.gestion_eventos.model.Compra nuevaCompra = new com.elaleph.gestion_eventos.model.Compra();
        
        // Buscamos el usuario, si no existe (null), el sistema igual intentará guardar
        // aunque lo ideal es que exista.
        usuarioRepository.findById(idUsuario).ifPresent(nuevaCompra::setUsuario);
        
        nuevaCompra.setClasificacion(clasificacion);
        nuevaCompra.setCantidad(cantidad);
        nuevaCompra.setForma_pago(formaPago);
        nuevaCompra.setMonto_total(cantidad * precioUnitario);
        
        // Agregamos un try-catch para ver el error exacto en la consola si falla
        try {
            compraRepository.save(nuevaCompra);
        } catch (Exception e) {
            System.out.println("ERROR AL GUARDAR: " + e.getMessage());
            return "redirect:/cliente/dashboard?error=db";
        }

        return "redirect:/cliente/dashboard";
    }
}