package com.mycompany.clinica.controller;

import com.mycompany.clinica.dto.*;
import com.mycompany.clinica.entity.RegistroConversion;
import com.mycompany.clinica.service.ConversionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversiones")
@CrossOrigin(origins = "*") // Permite la comunicación con tu HTML
public class ConversionController {

    private final ConversionService conversionService;

    public ConversionController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @PostMapping
    public ResponseEntity<ConversionResponse> convertir(
            @Valid @RequestBody ConversionRequest req,
            @AuthenticationPrincipal UserDetails user) {

        // Blindaje: Si no hay usuario logueado (sesión local), usamos "Anonimo"
        String username = (user != null) ? user.getUsername() : "Usuario_Local";

        return ResponseEntity.ok(conversionService.convertir(req, username));
    }

    @PostMapping("/validar-dosis")
    public ResponseEntity<String> validarDosis(
            @Valid @RequestBody ValidacionDosisRequest req) {
        String resultado = conversionService.validarDosisClinica(req);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/historial")
    public ResponseEntity<List<RegistroConversion>> historialPropio(
            @AuthenticationPrincipal UserDetails user) {

        String username = (user != null) ? user.getUsername() : "Usuario_Local";
        return ResponseEntity.ok(conversionService.obtenerHistorial(username));
    }

    @GetMapping("/historial/paged")
    public ResponseEntity<org.springframework.data.domain.Page<RegistroConversion>> historialPropioPaged(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String username = (user != null) ? user.getUsername() : "Usuario_Local";
        return ResponseEntity.ok(conversionService.obtenerHistorialPaginado(username, page, size));
    }

    @GetMapping("/historial/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RegistroConversion>> historialCompleto() {
        return ResponseEntity.ok(conversionService.obtenerTodoElHistorial());
    }

    @GetMapping("/historial/all/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<RegistroConversion>> historialCompletoPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(conversionService.obtenerTodoElHistorialPaginado(page, size));
    }
}