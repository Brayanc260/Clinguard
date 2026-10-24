
package com.mycompany.clinica.controller;

import com.mycompany.clinica.service.RecepcionistaDashboardService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/recepcionista")
@PreAuthorize("hasRole('RECEPCIONISTA')")
public class RecepcionistaController {

    private final RecepcionistaDashboardService dashboardService;

    public RecepcionistaController(
            RecepcionistaDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        model.addAttribute(
                "username",
                authentication.getName()
        );

        model.addAttribute(
                "rol",
                "RECEPCIONISTA"
        );

        dashboardService.cargarIndicadores(model);

        return "dashboard-recepcionista";
    }
}

