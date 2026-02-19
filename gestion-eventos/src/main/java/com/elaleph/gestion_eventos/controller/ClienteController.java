package com.elaleph.gestion_eventos.controller;

import com.elaleph.gestion_eventos.model.Compra;
import com.elaleph.gestion_eventos.model.Evento;
import com.elaleph.gestion_eventos.model.Usuario;
import com.elaleph.gestion_eventos.repository.CompraRepository;
import com.elaleph.gestion_eventos.repository.UsuarioRepository;
import com.elaleph.gestion_eventos.repository.EventoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ClienteController {

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/")
    public String index(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario != null) {
            return "ADMIN".equals(usuario.getRol()) ? "redirect:/admin/dashboard" : "redirect:/cliente/dashboard";
        }
        return "index";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        Usuario usuario = usuarioRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(email) && u.getPassword().equals(password))
                .findFirst().orElse(null);

        if (usuario != null) {
            session.setAttribute("usuarioLogueado", usuario);
            return "ADMIN".equals(usuario.getRol()) ? "redirect:/admin/dashboard" : "redirect:/cliente/dashboard";
        } else {
            model.addAttribute("error", "Email o clave incorrectos");
            return "index";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); 
        return "redirect:/?logout";
    }

    // --- REGISTRO DE INVITADOS ---

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("eventos", eventoRepository.findAll());
        return "registro";
    }

    // ESTE MÉTODO ES EL QUE TE FALTABA Y DABA ERROR 500
    @PostMapping("/registrar-invitado")
    public String registrarInvitado(@RequestParam String nombre, 
                                    @RequestParam String email, 
                                    @RequestParam String password, 
                                    @RequestParam Long idEvento, 
                                    HttpSession session) {
        Usuario nuevo = new Usuario();
        nuevo.setNombre_completo(nombre);
        nuevo.setEmail(email);
        nuevo.setPassword(password);
        nuevo.setId_evento(idEvento);
        nuevo.setRol("CLIENTE"); // Importante: por defecto son clientes
        
        usuarioRepository.save(nuevo);
        
        // Iniciamos sesión automáticamente
        session.setAttribute("usuarioLogueado", nuevo);
        return "redirect:/cliente/dashboard";
    }

    // --- PROTECCIÓN CLIENTE ---
    @GetMapping("/cliente/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"CLIENTE".equals(user.getRol())) return "redirect:/";

        Evento evento = eventoRepository.findById(user.getId_evento()).orElse(null);
        model.addAttribute("usuario", user);
        model.addAttribute("evento", evento);
        model.addAttribute("misCompras", compraRepository.findAll().stream()
                .filter(c -> c.getUsuario() != null && c.getUsuario().getId_usuario().equals(user.getId_usuario()))
                .toList());
        return "dashboard";
    }

    @PostMapping("/cliente/comprar")
    public String procesarCompra(@RequestParam Long idUsuario, @RequestParam(defaultValue = "0") Integer cantAdultos,
                                 @RequestParam(defaultValue = "0") Integer cantMenores, @RequestParam(defaultValue = "0") Integer cantFiesta,
                                 @RequestParam String formaPago, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"CLIENTE".equals(user.getRol())) return "redirect:/";

        Evento evento = eventoRepository.findById(user.getId_evento()).orElse(null);
        double total = (cantAdultos * evento.getPrecio_adulto()) + (cantMenores * evento.getPrecio_menor()) + (cantFiesta * evento.getPrecio_fiesta());
        
        if (total <= 0) return "redirect:/cliente/dashboard?error=vacio";

        Compra nuevaCompra = new Compra();
        nuevaCompra.setUsuario(user);
        nuevaCompra.setNombre_evento(evento.getNombre()); 
        nuevaCompra.setClasificacion(String.format("A:%d, M:%d, F:%d", cantAdultos, cantMenores, cantFiesta));
        nuevaCompra.setCantidad(cantAdultos + cantMenores + cantFiesta);
        nuevaCompra.setForma_pago(formaPago);
        nuevaCompra.setMonto_total(total);
        nuevaCompra.setEstado_pago("PENDIENTE");
        
        compraRepository.save(nuevaCompra);
        return "redirect:/cliente/dashboard";
    }

    // --- PROTECCIÓN ADMIN ---
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";

        List<Compra> todas = compraRepository.findAll();
        double totalRecaudado = todas.stream().filter(c -> "APROBADO".equals(c.getEstado_pago())).mapToDouble(Compra::getMonto_total).sum();
        model.addAttribute("todasLasCompras", todas);
        model.addAttribute("total", totalRecaudado);
        return "admin_dashboard";
    }

    @PostMapping("/admin/aprobar-pago")
    public String aprobarPago(@RequestParam Long idCompra, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";

        compraRepository.findById(idCompra).ifPresent(c -> {
            c.setEstado_pago("APROBADO");
            compraRepository.save(c);
        });
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/eventos")
    public String gestionarEventos(Model model, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";
        model.addAttribute("eventos", eventoRepository.findAll());
        return "admin_eventos";
    }

    @PostMapping("/admin/eventos/crear")
    public String crearEvento(@RequestParam String nombre, @RequestParam String fecha, 
                             @RequestParam String hora, @RequestParam String lugar,
                             @RequestParam(required = false) Double precioAdulto, 
                             @RequestParam(required = false) Double precioMenor,
                             @RequestParam(required = false) Double precioFiesta, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";

        Evento nuevo = new Evento();
        nuevo.setNombre(nombre);
        nuevo.setFecha(java.time.LocalDate.parse(fecha));
        nuevo.setHora(java.time.LocalTime.parse(hora));
        nuevo.setLugar(lugar);
        nuevo.setPrecio_adulto(precioAdulto != null ? precioAdulto : 0.0);
        nuevo.setPrecio_menor(precioMenor != null ? precioMenor : 0.0);
        nuevo.setPrecio_fiesta(precioFiesta != null ? precioFiesta : 0.0);
        eventoRepository.save(nuevo);
        return "redirect:/admin/eventos";
    }

    @PostMapping("/admin/eventos/modificar")
    public String modificarEvento(@RequestParam Long idEvento, @RequestParam String nombre, 
                                 @RequestParam String fecha, @RequestParam String hora, @RequestParam String lugar,
                                 @RequestParam(required = false) Double precioAdulto, 
                                 @RequestParam(required = false) Double precioMenor,
                                 @RequestParam(required = false) Double precioFiesta, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";

        eventoRepository.findById(idEvento).ifPresent(e -> {
            e.setNombre(nombre);
            e.setFecha(java.time.LocalDate.parse(fecha));
            e.setHora(java.time.LocalTime.parse(hora));
            e.setLugar(lugar);
            e.setPrecio_adulto(precioAdulto != null ? precioAdulto : 0.0);
            e.setPrecio_menor(precioMenor != null ? precioMenor : 0.0);
            e.setPrecio_fiesta(precioFiesta != null ? precioFiesta : 0.0);
            eventoRepository.save(e);
        });
        return "redirect:/admin/eventos";
    }

    @PostMapping("/admin/eventos/eliminar")
    public String eliminarEvento(@RequestParam Long idEvento, RedirectAttributes ra, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";

        try {
            eventoRepository.deleteById(idEvento);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "No se puede eliminar el evento: existen usuarios o compras vinculadas.");
        }
        return "redirect:/admin/eventos";
    }

    @GetMapping("/admin/usuarios")
    public String gestionarUsuarios(Model model, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("eventos", eventoRepository.findAll());
        return "admin_usuarios";
    }

    @PostMapping("/admin/usuarios/crear")
    public String crearUsuario(@RequestParam String nombre, @RequestParam String email, 
                               @RequestParam String password, @RequestParam Long idEvento, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";

        Usuario nuevo = new Usuario();
        nuevo.setNombre_completo(nombre);
        nuevo.setEmail(email);
        nuevo.setPassword(password);
        nuevo.setId_evento(idEvento);
        nuevo.setRol("CLIENTE");
        usuarioRepository.save(nuevo);
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/admin/usuarios/eliminar")
    public String eliminarUsuario(@RequestParam Long idUsuario, RedirectAttributes ra, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";

        try {
            usuarioRepository.deleteById(idUsuario);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "No se puede eliminar el usuario: tiene compras registradas en el sistema.");
        }
        return "redirect:/admin/usuarios";
    }
}