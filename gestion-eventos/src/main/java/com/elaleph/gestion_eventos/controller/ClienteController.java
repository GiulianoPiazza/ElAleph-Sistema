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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Comparator;

@Controller
public class ClienteController {

    @Autowired private EventoRepository eventoRepository;
    @Autowired private CompraRepository compraRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @GetMapping("/")
    public String index(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario != null) {
            return "ADMIN".equals(usuario.getRol()) ? "redirect:/admin/dashboard" : "redirect:/cliente/dashboard";
        }
        return "index";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        Usuario usuario = usuarioRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst().orElse(null);
        if (usuario != null) {
            session.setAttribute("usuarioLogueado", usuario);
            return "ADMIN".equals(usuario.getRol()) ? "redirect:/admin/dashboard" : "redirect:/cliente/dashboard";
        }
        model.addAttribute("error", "Usuario o clave incorrectos. Solicite acceso al administrador.");
        return "index";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); 
        return "redirect:/?logout";
    }

    // --- DASHBOARD CLIENTE ---
    @GetMapping("/cliente/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"CLIENTE".equals(user.getRol())) return "redirect:/";

        Evento evento = eventoRepository.findById(user.getId_evento()).orElse(null);
        model.addAttribute("usuario", user);
        model.addAttribute("evento", evento);
        model.addAttribute("misCompras", compraRepository.findAll().stream()
                .filter(c -> c.getUsuario() != null && c.getUsuario().getId_usuario().equals(user.getId_usuario()))
                .sorted((c1, c2) -> c2.getFecha_compra().compareTo(c1.getFecha_compra()))
                .toList());
        return "dashboard";
    }

    // AHORA RECIBE EL ARCHIVO DE FOTO
    @PostMapping("/cliente/comprar")
    public String procesarCompra(@RequestParam Long idUsuario, 
                                 @RequestParam(defaultValue = "0") Integer cantAdultos,
                                 @RequestParam(defaultValue = "0") Integer cantAdolescentes,
                                 @RequestParam(defaultValue = "0") Integer cantMenores, 
                                 @RequestParam(defaultValue = "0") Integer cantFiesta,
                                 @RequestParam String formaPago, 
                                 @RequestParam(required = false) MultipartFile archivoComprobante,
                                 HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null) return "redirect:/";
        Evento evento = eventoRepository.findById(user.getId_evento()).orElse(null);
        
        double total = (cantAdultos * evento.getPrecio_adulto()) + 
                       (cantAdolescentes * evento.getPrecio_adolescente()) + 
                       (cantMenores * evento.getPrecio_menor()) + 
                       (cantFiesta * evento.getPrecio_fiesta());
        
        if (total <= 0) return "redirect:/cliente/dashboard?error=vacio";

        Compra nuevaCompra = new Compra();
        nuevaCompra.setUsuario(user);
        nuevaCompra.setNombre_evento(evento.getNombre()); 
        nuevaCompra.setClasificacion(String.format("A:%d, AD:%d, M:%d, F:%d", cantAdultos, cantAdolescentes, cantMenores, cantFiesta));
        nuevaCompra.setCantidad(cantAdultos + cantAdolescentes + cantMenores + cantFiesta);
        nuevaCompra.setForma_pago(formaPago);
        nuevaCompra.setMonto_total(total);
        nuevaCompra.setEstado_pago("PENDIENTE");

        // SI HAY FOTO, LA GUARDAMOS
        if (archivoComprobante != null && !archivoComprobante.isEmpty()) {
            try {
                String base64Image = Base64.getEncoder().encodeToString(archivoComprobante.getBytes());
                nuevaCompra.setComprobante("data:" + archivoComprobante.getContentType() + ";base64," + base64Image);
            } catch (Exception e) {
                System.out.println("Error guardando comprobante: " + e.getMessage());
            }
        }

        compraRepository.save(nuevaCompra);
        return "redirect:/cliente/dashboard";
    }

    @PostMapping("/cliente/eliminar-compra")
    public String eliminarCompraCliente(@RequestParam Long idCompra, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        compraRepository.findById(idCompra).ifPresent(c -> {
            if ("PENDIENTE".equals(c.getEstado_pago()) && c.getUsuario().getId_usuario().equals(user.getId_usuario())) {
                compraRepository.delete(c);
            }
        });
        return "redirect:/cliente/dashboard";
    }

    // --- ADMIN DASHBOARD ---
    @GetMapping("/admin/dashboard")
    public String adminDashboard(@RequestParam(required = false) Long idEvento, Model model, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";

        List<Compra> filtradas = compraRepository.findAll().stream()
                .filter(c -> idEvento == null || idEvento == 0 || (c.getUsuario() != null && c.getUsuario().getId_evento().equals(idEvento)))
                .sorted((c1, c2) -> {
                    if (c1.getFecha_compra() == null) return 1;
                    if (c2.getFecha_compra() == null) return -1;
                    return c2.getFecha_compra().compareTo(c1.getFecha_compra());
                })
                .toList();

        model.addAttribute("todasLasCompras", filtradas);
        model.addAttribute("total", filtradas.stream().filter(c -> "APROBADO".equals(c.getEstado_pago()) && c.getMonto_total() != null).mapToDouble(Compra::getMonto_total).sum());
        model.addAttribute("eventos", eventoRepository.findAll()); 
        model.addAttribute("idEventoSeleccionado", idEvento);
        return "admin_dashboard";
    }

    @PostMapping("/admin/aprobar-pago")
    public String aprobarPago(@RequestParam Long idCompra, @RequestParam(required = false) Long filtroEvento) {
        compraRepository.findById(idCompra).ifPresent(c -> { c.setEstado_pago("APROBADO"); compraRepository.save(c); });
        return "redirect:/admin/dashboard" + (filtroEvento != null && filtroEvento > 0 ? "?idEvento=" + filtroEvento : "");
    }

    @PostMapping("/admin/revertir-pago")
    public String revertirPago(@RequestParam Long idCompra, @RequestParam(required = false) Long filtroEvento) {
        compraRepository.findById(idCompra).ifPresent(c -> { c.setEstado_pago("PENDIENTE"); compraRepository.save(c); });
        return "redirect:/admin/dashboard" + (filtroEvento != null && filtroEvento > 0 ? "?idEvento=" + filtroEvento : "");
    }

    @PostMapping("/admin/eliminar-pago")
    public String eliminarPagoAdmin(@RequestParam Long idCompra, @RequestParam(required = false) Long filtroEvento) {
        compraRepository.deleteById(idCompra);
        return "redirect:/admin/dashboard" + (filtroEvento != null && filtroEvento > 0 ? "?idEvento=" + filtroEvento : "");
    }

    // --- RENDICIÓN MENSUAL (CON HISTÓRICO COMPLETO) ---
    @GetMapping("/admin/rendicion")
    public String verRendicion(@RequestParam(required = false) Integer mes, @RequestParam(required = false) Long idEvento, Model model, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";

        int mesBusqueda = (mes == null) ? 0 : mes;
        int mesReal = (mesBusqueda == 0) ? LocalDate.now().getMonthValue() : mesBusqueda;

        List<Compra> filtradas = compraRepository.findAll().stream()
                .filter(c -> mesBusqueda == -1 || (c.getFecha_compra() != null && c.getFecha_compra().getMonthValue() == mesReal))
                .filter(c -> idEvento == null || idEvento == 0 || (c.getUsuario() != null && c.getUsuario().getId_evento().equals(idEvento)))
                .filter(c -> "APROBADO".equals(c.getEstado_pago())) 
                .toList();

        int tA = 0, tAD = 0, tM = 0, tF = 0;
        for(Compra c : filtradas) {
            if (c.getClasificacion() != null && !c.getClasificacion().isEmpty()) {
                String[] partes = c.getClasificacion().split(",");
                for(String p : partes) {
                    try {
                        p = p.trim();
                        if(p.startsWith("A:")) tA += Integer.parseInt(p.substring(2).trim());
                        else if(p.startsWith("AD:")) tAD += Integer.parseInt(p.substring(3).trim());
                        else if(p.startsWith("M:")) tM += Integer.parseInt(p.substring(2).trim());
                        else if(p.startsWith("F:")) tF += Integer.parseInt(p.substring(2).trim());
                    } catch (Exception e) {}
                }
            }
        }

        model.addAttribute("totalPersonas", (tA+tM+tAD+tF));
        model.addAttribute("adultos", tA); model.addAttribute("adolescentes", tAD);
        model.addAttribute("menores", tM); model.addAttribute("fiesta", tF);
        
        double totalMes = filtradas.stream().filter(c -> c.getMonto_total() != null).mapToDouble(Compra::getMonto_total).sum();
        model.addAttribute("totalRecaudado", totalMes);
        
        model.addAttribute("eventos", eventoRepository.findAll());
        model.addAttribute("mesSeleccionado", mesBusqueda);
        model.addAttribute("idEventoSeleccionado", idEvento);
        
        return "admin_rendicion";
    }

    // --- GESTIÓN DE EVENTOS ---
    @GetMapping("/admin/eventos")
    public String gestionarEventos(@RequestParam(required = false) String buscarTexto, 
                                   @RequestParam(required = false) String buscarFecha, 
                                   Model model, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";
        
        List<Evento> eventos = eventoRepository.findAll();

        if (buscarTexto != null && !buscarTexto.isEmpty()) {
            String textoClean = buscarTexto.toLowerCase();
            eventos = eventos.stream()
                    .filter(e -> e.getNombre().toLowerCase().contains(textoClean) || 
                                 e.getLugar().toLowerCase().contains(textoClean))
                    .toList();
        }

        if (buscarFecha != null && !buscarFecha.isEmpty()) {
            eventos = eventos.stream()
                    .filter(e -> e.getFecha() != null && e.getFecha().toString().equals(buscarFecha))
                    .toList();
        }

        eventos = eventos.stream()
                .sorted(Comparator.comparing((Evento e) -> e.getActivo() != null ? e.getActivo() : false).reversed()
                        .thenComparing(Evento::getFecha))
                .toList();

        model.addAttribute("eventos", eventos);
        model.addAttribute("buscarTexto", buscarTexto);
        model.addAttribute("buscarFecha", buscarFecha);
        return "admin_eventos";
    }

    @PostMapping("/admin/eventos/archivar")
    public String archivarEvento(@RequestParam Long idEvento, RedirectAttributes ra) {
        eventoRepository.findById(idEvento).ifPresent(e -> {
            boolean estadoActual = e.getActivo() != null ? e.getActivo() : true;
            e.setActivo(!estadoActual);
            eventoRepository.save(e);
            if(e.getActivo()) {
                ra.addFlashAttribute("successMsg", "El evento fue DESARCHIVADO y vuelve a estar activo.");
            } else {
                ra.addFlashAttribute("successMsg", "El evento fue ARCHIVADO. Sus precios fueron bloqueados y no le afectarán los aumentos globales.");
            }
        });
        return "redirect:/admin/eventos";
    }

    @PostMapping("/admin/eventos/aumento-general")
    public String aplicarAumentoGeneral(@RequestParam Double porcentaje, RedirectAttributes ra, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";

        List<Evento> eventos = eventoRepository.findAll();
        java.util.Map<Long, Double[]> backupPrecios = new java.util.HashMap<>();
        double multiplicador = 1.0 + (porcentaje / 100.0);
        int aumentados = 0;

        for (Evento e : eventos) {
            if (e.getActivo() != null && e.getActivo()) {
                backupPrecios.put(e.getId_evento(), new Double[]{ e.getPrecio_adulto(), e.getPrecio_adolescente(), e.getPrecio_menor(), e.getPrecio_fiesta() });

                if (e.getPrecio_adulto() != null && e.getPrecio_adulto() > 0) e.setPrecio_adulto((double) Math.round(e.getPrecio_adulto() * multiplicador));
                if (e.getPrecio_adolescente() != null && e.getPrecio_adolescente() > 0) e.setPrecio_adolescente((double) Math.round(e.getPrecio_adolescente() * multiplicador));
                if (e.getPrecio_menor() != null && e.getPrecio_menor() > 0) e.setPrecio_menor((double) Math.round(e.getPrecio_menor() * multiplicador));
                if (e.getPrecio_fiesta() != null && e.getPrecio_fiesta() > 0) e.setPrecio_fiesta((double) Math.round(e.getPrecio_fiesta() * multiplicador));
                
                eventoRepository.save(e);
                aumentados++;
            }
        }
        
        session.setAttribute("backupPrecios", backupPrecios);
        ra.addFlashAttribute("successMsg", "¡Éxito! Se aplicó un aumento del " + porcentaje + "% a " + aumentados + " eventos ACTIVOS. Los eventos archivados fueron protegidos.");
        return "redirect:/admin/eventos";
    }

    @PostMapping("/admin/eventos/deshacer-aumento")
    public String deshacerAumento(HttpSession session, RedirectAttributes ra) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";

        @SuppressWarnings("unchecked")
        java.util.Map<Long, Double[]> backup = (java.util.Map<Long, Double[]>) session.getAttribute("backupPrecios");
        
        if (backup != null) {
            List<Evento> eventos = eventoRepository.findAll();
            for (Evento e : eventos) {
                Double[] preciosViejos = backup.get(e.getId_evento());
                if (preciosViejos != null) {
                    e.setPrecio_adulto(preciosViejos[0]); e.setPrecio_adolescente(preciosViejos[1]);
                    e.setPrecio_menor(preciosViejos[2]); e.setPrecio_fiesta(preciosViejos[3]);
                    eventoRepository.save(e);
                }
            }
            session.removeAttribute("backupPrecios");
            ra.addFlashAttribute("successMsg", "Salvados: Se ha deshecho el último aumento. Los precios volvieron a la normalidad.");
        } else {
            ra.addFlashAttribute("errorMsg", "No hay ningún aumento reciente en la memoria para deshacer.");
        }
        return "redirect:/admin/eventos";
    }

    @PostMapping("/admin/eventos/crear")
    public String crearEvento(@RequestParam String nombre, @RequestParam String fecha, @RequestParam String hora, @RequestParam String lugar,
                             @RequestParam(required = false) Double precioAdulto, @RequestParam(required = false) Double precioAdolescente, 
                             @RequestParam(required = false) Double precioMenor, @RequestParam(required = false) Double precioFiesta) {
        Evento nuevo = new Evento();
        nuevo.setNombre(nombre); nuevo.setFecha(java.time.LocalDate.parse(fecha)); nuevo.setHora(java.time.LocalTime.parse(hora)); nuevo.setLugar(lugar);
        nuevo.setPrecio_adulto(precioAdulto != null ? precioAdulto : 0.0); nuevo.setPrecio_adolescente(precioAdolescente != null ? precioAdolescente : 0.0);
        nuevo.setPrecio_menor(precioMenor != null ? precioMenor : 0.0); nuevo.setPrecio_fiesta(precioFiesta != null ? precioFiesta : 0.0);
        nuevo.setActivo(true);
        eventoRepository.save(nuevo);
        return "redirect:/admin/eventos";
    }

    @PostMapping("/admin/eventos/modificar")
    public String modificarEvento(@RequestParam Long idEvento, @RequestParam String nombre, @RequestParam String fecha, @RequestParam String hora, @RequestParam String lugar,
                                 @RequestParam(required = false) Double precioAdulto, @RequestParam(required = false) Double precioAdolescente, 
                                 @RequestParam(required = false) Double precioMenor, @RequestParam(required = false) Double precioFiesta) {
        eventoRepository.findById(idEvento).ifPresent(e -> {
            e.setNombre(nombre); e.setFecha(java.time.LocalDate.parse(fecha)); e.setHora(java.time.LocalTime.parse(hora)); e.setLugar(lugar);
            e.setPrecio_adulto(precioAdulto != null ? precioAdulto : 0.0); e.setPrecio_adolescente(precioAdolescente != null ? precioAdolescente : 0.0);
            e.setPrecio_menor(precioMenor != null ? precioMenor : 0.0); e.setPrecio_fiesta(precioFiesta != null ? precioFiesta : 0.0);
            eventoRepository.save(e);
        });
        return "redirect:/admin/eventos";
    }

    @PostMapping("/admin/eventos/eliminar")
    public String eliminarEvento(@RequestParam Long idEvento, RedirectAttributes ra) {
        try { eventoRepository.deleteById(idEvento); } catch (Exception e) { ra.addFlashAttribute("errorMsg", "No se puede eliminar: existen usuarios vinculados. Archivalo en su lugar."); }
        return "redirect:/admin/eventos";
    }

    // --- GESTIÓN DE USUARIOS ---
    @GetMapping("/admin/usuarios")
    public String gestionarUsuarios(@RequestParam(required = false) Long idEvento, Model model, HttpSession session) {
        Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
        if (user == null || !"ADMIN".equals(user.getRol())) return "redirect:/";
        List<Usuario> filtrados = usuarioRepository.findAll().stream()
                .filter(u -> idEvento == null || idEvento == 0 || u.getId_evento().equals(idEvento))
                .sorted(Comparator.comparing(Usuario::getId_evento)).toList();
        model.addAttribute("usuarios", filtrados);
        model.addAttribute("eventos", eventoRepository.findAll());
        model.addAttribute("idEventoSeleccionado", idEvento);
        return "admin_usuarios";
    }

    @PostMapping("/admin/usuarios/crear")
    public String crearUsuario(@RequestParam String nombre, @RequestParam String username, @RequestParam String password, @RequestParam Long idEvento) {
        Usuario nuevo = new Usuario(); nuevo.setNombre_completo(nombre); nuevo.setUsername(username); nuevo.setPassword(password); nuevo.setId_evento(idEvento); nuevo.setRol("CLIENTE");
        usuarioRepository.save(nuevo);
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/admin/usuarios/eliminar")
    public String eliminarUsuario(@RequestParam Long idUsuario, @RequestParam(required = false) Long filtroEvento, RedirectAttributes ra) {
        try { 
            List<Compra> comprasDelUsuario = compraRepository.findAll().stream()
                    .filter(c -> c.getUsuario() != null && c.getUsuario().getId_usuario().equals(idUsuario))
                    .toList();
            compraRepository.deleteAll(comprasDelUsuario);
            usuarioRepository.deleteById(idUsuario); 
            ra.addFlashAttribute("successMsg", "El cliente y todo su historial de pagos fueron eliminados correctamente.");
        } catch (Exception e) { 
            ra.addFlashAttribute("errorMsg", "Ocurrió un error inesperado al intentar borrar el usuario."); 
        }
        return "redirect:/admin/usuarios" + (filtroEvento != null && filtroEvento > 0 ? "?idEvento=" + filtroEvento : "");
    }
}