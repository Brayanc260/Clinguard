package com.mycompany.clinica.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorController {

    // =========================================
    // ERROR 403 - ACCESO DENEGADO
    // =========================================
    @GetMapping("/error/403")
    public String error403() {
        return "error/403";
    }


    // =========================================
    // ERROR 404 - PÃGINA NO ENCONTRADA
    // =========================================
    @GetMapping("/error/404")
    public String error404() {
        return "error/404";
    }


    // =========================================
    // ERROR 500 - ERROR INTERNO
    // =========================================
    @GetMapping("/error/500")
    public String error500() {
        return "error/500";
    }
}
