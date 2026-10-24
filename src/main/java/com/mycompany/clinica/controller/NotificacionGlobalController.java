
package com.mycompany.clinica.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.mycompany.clinica.service.NotificacionService;

@ControllerAdvice
public class NotificacionGlobalController {

    private final NotificacionService servicio;

    public NotificacionGlobalController(
            NotificacionService servicio) {

        this.servicio = servicio;
    }

    @ModelAttribute("noLeidas")
    public long notificacionesNoLeidas(
            Principal principal) {

        if (principal == null) {
            return 0L;
        }

        return servicio.contarNoLeidas(
            principal.getName()
        );
    }
}

