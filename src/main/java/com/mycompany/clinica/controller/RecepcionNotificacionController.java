package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Notificacion;
import com.mycompany.clinica.service.NotificacionService;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recepcion/notificaciones")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
public class RecepcionNotificacionController {

    private final NotificacionService servicio;

    public RecepcionNotificacionController(NotificacionService servicio) {
        this.servicio = servicio;
    }

    // =====================================================
    // 1. LISTAR, FILTRAR Y BUSCAR NOTIFICACIONES
    // =====================================================
    @GetMapping
    public String listar(
            @RequestParam(name = "filtro", defaultValue = "todas") String filtro,
            @RequestParam(name = "q", defaultValue = "") String q,
            Principal principal,
            Model model) {

        String username = principal.getName();
        String estado = normalizarFiltro(filtro);
        String consulta = q == null ? "" : q.strip();
        String busqueda = consulta.toLowerCase(Locale.ROOT);

        List<Notificacion> visibles = servicio.obtenerPorUsuario(username)
                .stream()
                .filter(n -> switch (estado) {
                    case "leidas" -> n.isLeida();
                    case "no-leidas" -> !n.isLeida();
                    default -> true;
                })
                .filter(n -> busqueda.isBlank()
                        || contiene(n.getTitulo(), busqueda)
                        || contiene(n.getMensaje(), busqueda)
                        || contiene(n.getTipo(), busqueda))
                .toList();

        model.addAttribute("notificaciones", visibles);
        model.addAttribute("totalNotificaciones", servicio.contarTodas(username));
        model.addAttribute("noLeidas", servicio.contarNoLeidas(username));
        model.addAttribute("leidas", servicio.contarLeidas(username));
        model.addAttribute("filtro", estado);
        model.addAttribute("q", consulta);

        return "recepcion-notificaciones";
    }

    // =====================================================
    // 2. MARCAR UNA NOTIFICACIÃ“N COMO LEÃDA
    // =====================================================
    @PostMapping("/{id}/leer")
    public String leer(
            @PathVariable String id,
            @RequestParam(name = "filtro", defaultValue = "todas") String filtro,
            @RequestParam(name = "q", defaultValue = "") String q,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        servicio.marcarComoLeida(id, principal.getName());
        conservarFiltros(filtro, q, redirectAttributes);
        return "redirect:/recepcion/notificaciones";
    }

    // =====================================================
    // 3. MARCAR TODAS COMO LEÃDAS
    // =====================================================
    @PostMapping("/marcar-todas")
    public String marcarTodas(
            @RequestParam(name = "filtro", defaultValue = "todas") String filtro,
            @RequestParam(name = "q", defaultValue = "") String q,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        servicio.marcarTodasComoLeidas(principal.getName());
        conservarFiltros(filtro, q, redirectAttributes);
        return "redirect:/recepcion/notificaciones";
    }

    // =====================================================
    // 4. ELIMINAR NOTIFICACIÃ“N
    // =====================================================
    @PostMapping("/{id}/eliminar")
    public String eliminar(
            @PathVariable String id,
            @RequestParam(name = "filtro", defaultValue = "todas") String filtro,
            @RequestParam(name = "q", defaultValue = "") String q,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        servicio.eliminar(id, principal.getName());
        conservarFiltros(filtro, q, redirectAttributes);
        return "redirect:/recepcion/notificaciones";
    }

    // =====================================================
    // 5. UTILIDADES
    // =====================================================
    private String normalizarFiltro(String filtro) {
        if ("leidas".equalsIgnoreCase(filtro)) {
            return "leidas";
        }
        if ("no-leidas".equalsIgnoreCase(filtro)) {
            return "no-leidas";
        }
        return "todas";
    }

    private boolean contiene(String texto, String busqueda) {
        return texto != null
                && texto.toLowerCase(Locale.ROOT).contains(busqueda);
    }

    private void conservarFiltros(String filtro, String q, RedirectAttributes redirectAttributes) {
        String estado = normalizarFiltro(filtro);
        String consulta = q == null ? "" : q.strip();

        if (!"todas".equals(estado)) {
            redirectAttributes.addAttribute("filtro", estado);
        }
        if (!consulta.isBlank()) {
            redirectAttributes.addAttribute("q", consulta);
        }
    }
}

