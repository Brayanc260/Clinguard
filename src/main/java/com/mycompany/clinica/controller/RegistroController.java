package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;
import com.mycompany.clinica.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistroController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/registro")
    public String registrarUsuario(@RequestParam("nombre") String nombre, @RequestParam("username") String username,
            @RequestParam("correo") String correo, @RequestParam("password") String password,
            @RequestParam("confirmar_password") String confirmarPassword,
            @RequestParam(value = "rol", defaultValue = "USUARIO") String rol,
            Authentication authentication, RedirectAttributes redirectAttributes) {

        Rol rolSeleccionado;
        try {
            rolSeleccionado = Rol.valueOf(rol.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return error(redirectAttributes, "Selecciona un rol de registro válido.");
        }

        boolean esAdmin = authentication != null && authentication.isAuthenticated() &&
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals);

        if (rolSeleccionado != Rol.USUARIO && !esAdmin) {
            return error(redirectAttributes, "No tienes permisos para registrar usuarios con este rol.");
        }

        if (nombre.isBlank() || username.isBlank() || correo.isBlank() || password.isBlank()) {
            return error(redirectAttributes, "Todos los campos son obligatorios.");
        }

        if (password.length() < 8) return error(redirectAttributes, "La contraseña debe tener al menos 8 caracteres.");
        if (!password.equals(confirmarPassword)) return error(redirectAttributes, "Las contraseñas no coinciden.");
        if (usuarioRepository.existsByNombreUsuario(username.trim())) return error(redirectAttributes, "El nombre de usuario ya está registrado.");
        if (usuarioRepository.existsByEmail(correo.trim().toLowerCase())) return error(redirectAttributes, "El correo ya está registrado.");

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre.trim());
        usuario.setNombreUsuario(username.trim());
        usuario.setEmail(correo.trim().toLowerCase());
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(rolSeleccionado);
        usuario.setActivo(true);

        usuarioRepository.save(usuario);

        if (esAdmin) {
            redirectAttributes.addFlashAttribute("mensaje", "Usuario registrado correctamente.");
            return "redirect:/usuarios";
        }
        return "redirect:/login?registro=true";
    }

    private String error(RedirectAttributes redirectAttributes, String mensaje) {
        redirectAttributes.addFlashAttribute("errorRegistro", mensaje);
        return "redirect:/login?tab=registro";
    }
}