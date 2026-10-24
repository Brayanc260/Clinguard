
package com.mycompany.clinica.controller;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mycompany.clinica.entity.Notificacion;
import com.mycompany.clinica.service.NotificacionService;

@Controller
public class NotificacionController {

    // =====================================================
    // SERVICIO
    // =====================================================

    private final NotificacionService servicio;

    public NotificacionController(
            NotificacionService servicio) {

        this.servicio = servicio;
    }

    // =====================================================
    // VISTA DE NOTIFICACIONES
    // IDENTIFICAR ROL Y CARGAR NOTIFICACIONES
    // =====================================================

    @GetMapping("/notificaciones")
    public String listar(
            Authentication authentication,
            @RequestParam(defaultValue = "todas") String filtro,
            @RequestParam(defaultValue = "") String tipo,
            @RequestParam(defaultValue = "") String q,
            Model model) {

        // =============================================
        // VALIDAR AUTENTICACIÃ“N
        // =============================================

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return "redirect:/login";
        }

        String username = authentication.getName();

        // =============================================
        // IDENTIFICAR LOS CUATRO ROLES
        // =============================================

        boolean esAdmin = tieneRol(
            authentication, "ROLE_ADMIN"
        );

        boolean esMedico = tieneRol(
            authentication, "ROLE_MEDICO"
        );

        boolean esEnfermero = tieneRol(
            authentication, "ROLE_ENFERMERO"
        );

        boolean esUsuario = tieneRol(
            authentication, "ROLE_USUARIO"
        );

        // Evitar acceso a roles desconocidos
        if (!esAdmin && !esMedico
                && !esEnfermero && !esUsuario) {

            return "redirect:/error/403";
        }

        // =============================================
        // NOMBRE DEL ROL PARA EL SIDEBAR
        // =============================================

        String rol;

        if (esAdmin) {
            rol = "Administrador";

        } else if (esMedico) {
            rol = "MÃ©dico";

        } else if (esEnfermero) {
            rol = "Enfermero";

        } else {
            rol = "Usuario";
        }

        boolean esPersonalClinico =
            esAdmin || esMedico || esEnfermero;

        // =============================================
        // CONSULTAR NOTIFICACIONES PROPIAS
        // =============================================

        List<Notificacion> notificaciones =
            servicio.obtenerPorUsuario(username);

        // =============================================
        // FILTRO POR ESTADO
        // =============================================

        if ("no-leidas".equalsIgnoreCase(filtro)) {

            filtro = "no-leidas";

            notificaciones = notificaciones.stream()
                .filter(n -> !n.isLeida())
                .toList();

        } else if ("leidas".equalsIgnoreCase(filtro)) {

            filtro = "leidas";

            notificaciones = notificaciones.stream()
                .filter(Notificacion::isLeida)
                .toList();

        } else {

            filtro = "todas";
        }

        // =============================================
        // FILTRO POR CATEGORÃA
        // =============================================

        if (tipo != null && !tipo.isBlank()) {

            String tipoSeleccionado = tipo.trim();

            notificaciones = notificaciones.stream()
                .filter(n ->
                    n.getTipo() != null
                    && n.getTipo().equalsIgnoreCase(
                        tipoSeleccionado
                    )
                )
                .toList();
        }

        // =============================================
        // BUSCADOR
        // =============================================

        if (q != null && !q.isBlank()) {

            String busqueda = q.trim()
                .toLowerCase(Locale.ROOT);

            notificaciones = notificaciones.stream()
                .filter(n ->
                    contiene(n.getTitulo(), busqueda)
                    || contiene(n.getMensaje(), busqueda)
                    || contiene(n.getTipo(), busqueda)
                )
                .toList();
        }

        // =============================================
        // DATOS DEL USUARIO
        // =============================================

        model.addAttribute("username", username);
        model.addAttribute("rol", rol);

        // =============================================
        // VARIABLES PARA EL SIDEBAR POR ROL
        // =============================================

        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("esMedico", esMedico);
        model.addAttribute("esEnfermero", esEnfermero);
        model.addAttribute("esUsuario", esUsuario);

        model.addAttribute(
            "esPersonalClinico",
            esPersonalClinico
        );

        // =============================================
        // NOTIFICACIONES
        // =============================================

        model.addAttribute(
            "notificaciones",
            notificaciones
        );

        // =============================================
        // CONTADORES REALES DESDE MONGODB
        // =============================================

        model.addAttribute(
            "totalNotificaciones",
            servicio.contarTodas(username)
        );

        model.addAttribute(
            "noLeidas",
            servicio.contarNoLeidas(username)
        );

        model.addAttribute(
            "leidas",
            servicio.contarLeidas(username)
        );

        // =============================================
        // FILTROS Y BÃšSQUEDA
        // =============================================

        model.addAttribute("filtro", filtro);
        model.addAttribute("tipo", tipo);
        model.addAttribute("q", q);

        return "notificaciones";
    }

    // =====================================================
    // MARCAR UNA NOTIFICACIÃ“N COMO LEÃDA
    // =====================================================

    @PostMapping("/notificaciones/{id}/leer")
    public String marcarLeida(
            @PathVariable String id,
            Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        servicio.marcarComoLeida(
            id,
            principal.getName()
        );

        return "redirect:/notificaciones";
    }

    // =====================================================
    // MARCAR TODAS LAS NOTIFICACIONES COMO LEÃDAS
    // =====================================================

    @PostMapping("/notificaciones/marcar-todas")
    public String marcarTodas(
            Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        servicio.marcarTodasComoLeidas(
            principal.getName()
        );

        return "redirect:/notificaciones";
    }

    // =====================================================
    // ELIMINAR UNA NOTIFICACIÃ“N
    // =====================================================

    @PostMapping("/notificaciones/{id}/eliminar")
    public String eliminar(
            @PathVariable String id,
            Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        servicio.eliminar(
            id,
            principal.getName()
        );

        return "redirect:/notificaciones";
    }

    // =====================================================
    // COMPROBAR ROL DEL USUARIO AUTENTICADO
    // =====================================================

    private boolean tieneRol(
            Authentication authentication,
            String rol) {

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
            .stream()
            .anyMatch(authority ->
                authority.getAuthority().equals(rol)
            );
    }

    // =====================================================
    // FUNCIÃ“N AUXILIAR PARA BUSCAR TEXTO
    // =====================================================

    private boolean contiene(
            String valor,
            String busqueda) {

        return valor != null
            && valor.toLowerCase(Locale.ROOT)
                .contains(busqueda);
    }
}

