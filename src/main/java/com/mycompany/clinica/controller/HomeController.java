package com.mycompany.clinica.controller;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final MongoTemplate mongoTemplate;

    public HomeController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/")
    public String inicio(Model model, Authentication authentication) {
        boolean esAdmin = authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        model.addAttribute("datosReales", esAdmin);
        if (esAdmin) {
            model.addAttribute("totalPacientesLanding", mongoTemplate.count(new Query(), "pacientes"));
            model.addAttribute("totalCitasLanding", mongoTemplate.count(new Query(), "citas"));
            model.addAttribute("totalHistorialesLanding", mongoTemplate.count(new Query(), "historiales"));
            model.addAttribute("totalMedicamentosLanding", mongoTemplate.count(new Query(), "medicamentos"));
        }
        return "index";
    }
}

