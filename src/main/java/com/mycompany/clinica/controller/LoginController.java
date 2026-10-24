package com.mycompany.clinica.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {


    // =========================================
    // MOSTRAR LOGIN
    // =========================================

    @GetMapping("/login")
    public String login() {

        return "login";
    }


    // =========================================
    // SESIÃ“N EXPIRADA
    // =========================================

    @GetMapping("/session-expired")
    public String sessionExpired() {

        return "redirect:/login?expired=true";
    }

}
