
package com.mycompany.clinica.controller;

import com.mycompany.clinica.dto.LoginRequest;
import com.mycompany.clinica.dto.LoginResponse;
import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.security.JwtRevocacionService;
import com.mycompany.clinica.security.JwtService;
import com.mycompany.clinica.service.UsuarioService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;
    private final JwtRevocacionService jwtRevocacionService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioService usuarioService,
            JwtRevocacionService jwtRevocacionService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
        this.jwtRevocacionService = jwtRevocacionService;
    }

    // ==========================================
    // INICIO DE SESION MEDIANTE JWT
    // POST /api/auth/login
    // ==========================================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        request.getNombreUsuario(),
                        request.getPassword()
                    )
                );

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(
            new LoginResponse(token)
        );
    }

    // ==========================================
    // CONSULTAR USUARIO AUTENTICADO
    // GET /api/auth/me
    // ==========================================

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            Authentication authentication) {

        return ResponseEntity.ok(Map.of(
            "nombreUsuario",
            authentication.getName(),

            "roles",
            authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .toList()
        ));
    }

    // ==========================================
    // CIERRE DE SESION JWT POR API
    // POST /api/auth/logout
    // ==========================================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(
                value = HttpHeaders.AUTHORIZATION,
                required = false
            ) String authorization) {

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "mensaje",
                    "Se requiere un token JWT Bearer"
                ));
        }

        String token = authorization.substring(7).trim();

        if (token.isBlank()) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "mensaje",
                    "Token JWT no proporcionado"
                ));
        }

        // Registrar el JTI como revocado en MongoDB.
        // El filtro JWT debe haber validado el token
        // antes de que se ejecute este metodo.
        jwtRevocacionService.revocarToken(token);

        return ResponseEntity.ok(Map.of(
            "mensaje",
            "Sesion API cerrada correctamente"
        ));
    }

    // ==========================================
    // REGISTRO PUBLICO
    // POST /api/auth/registro
    // ==========================================

    @PostMapping("/registro")
    public ResponseEntity<?> registro(
            @RequestBody LoginRequest request,
            @RequestParam(required = false) Rol rol) {

        // El registro publico solo permite USUARIO.
        if (rol != null && rol != Rol.USUARIO) {

            return ResponseEntity.badRequest().body(
                Map.of(
                    "mensaje",
                    "El registro publico solo permite el rol USUARIO"
                )
            );
        }

        try {
            usuarioService.registrar(
                request.getNombreUsuario(),
                request.getPassword(),
                Rol.USUARIO
            );

            return ResponseEntity.ok(Map.of(
                "mensaje",
                "Usuario registrado correctamente"
            ));

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                Map.of(
                    "mensaje",
                    "No fue posible completar el registro"
                )
            );
        }
    }
}
