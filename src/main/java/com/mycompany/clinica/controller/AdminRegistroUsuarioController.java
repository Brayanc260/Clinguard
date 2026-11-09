package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;
import com.mycompany.clinica.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRegistroUsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminRegistroUsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/nuevo")
    public String formulario(Model model) {
        model.addAttribute("roles", Rol.values());
        return "admin-usuario-nuevo";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam("nombre") String nombre, @RequestParam("username") String username,
            @RequestParam("correo") String correo, @RequestParam("password") String password,
            @RequestParam("confirmar_password") String confirmarPassword, @RequestParam("rol") String rol,
            RedirectAttributes redirectAttributes) {

        if (nombre.isBlank() || username.isBlank() || correo.isBlank() || password.isBlank()) {
            return error(redirectAttributes, "Todos los campos son obligatorios.");
        }

        Rol rolSeleccionado;
        try {
            rolSeleccionado = Rol.valueOf(rol.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return error(redirectAttributes, "El rol seleccionado no es válido.");
        }

        if (password.length() < 8) return error(redirectAttributes, "La contraseña debe tener al menos 8 caracteres.");
        if (!password.equals(confirmarPassword)) return error(redirectAttributes, "Las contraseñas no coinciden.");
        if (usuarioRepository.existsByNombreUsuario(username.trim())) return error(redirectAttributes, "El nombre de usuario ya existe.");
        if (usuarioRepository.existsByEmail(correo.trim().toLowerCase())) return error(redirectAttributes, "El correo ya está registrado.");

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre.trim());
        usuario.setNombreUsuario(username.trim());
        usuario.setEmail(correo.trim().toLowerCase());
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol(rolSeleccionado);
        usuario.setActivo(true);

        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("mensajeExito", "El usuario fue registrado correctamente.");
        return "redirect:/admin/usuarios/nuevo";
    }

    private String error(RedirectAttributes redirectAttributes, String mensaje) {
        redirectAttributes.addFlashAttribute("errorRegistro", mensaje);
        return "redirect:/admin/usuarios/nuevo";
    }
}