package com.mycompany.clinica.controller;

import com.mycompany.clinica.dto.LoginRequest;
import com.mycompany.clinica.dto.LoginResponse;
import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.security.JwtService;
import com.mycompany.clinica.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UsuarioService usuarioService; // Para poder registrar

    public AuthController(AuthenticationManager authenticationManager, 
                          UserDetailsService userDetailsService, 
                          JwtService jwtService,
                          UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getNombreUsuario(), request.getPassword())
        );
        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getNombreUsuario());
        final String token = jwtService.generateToken(userDetails);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    // ESTO ES LO QUE HACE QUE EL BOTÓN "CREAR CUENTA" FUNCIONE
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody LoginRequest request, @RequestParam Rol rol) {
        try {
            usuarioService.registrar(request.getNombreUsuario(), request.getPassword(), rol);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}