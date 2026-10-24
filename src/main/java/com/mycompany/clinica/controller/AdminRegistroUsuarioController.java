
package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;
import com.mycompany.clinica.repository.UsuarioRepository;

import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRegistroUsuarioController {

    // =====================================================
    // DEPENDENCIAS
    // =====================================================

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminRegistroUsuarioController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =====================================================
    // FORMULARIO DE REGISTRO
    // =====================================================

    @GetMapping("/nuevo")
    public String formulario(Model model) {

        model.addAttribute("roles", Rol.values());

        return "admin-usuario-nuevo";
    }

    // =====================================================
    // GUARDAR NUEVO USUARIO
    // =====================================================

    @PostMapping("/guardar")
    public String guardar(

            @RequestParam("nombre") String nombre,

            @RequestParam("username") String username,

            @RequestParam("correo") String correo,

            @RequestParam("password") String password,

            @RequestParam("confirmar_password")
            String confirmarPassword,

            @RequestParam("rol") String rol,

            RedirectAttributes redirectAttributes) {

        // =============================================
        // VALIDAR CAMPOS
        // =============================================

        if (nombre == null || nombre.isBlank()
                || username == null || username.isBlank()
                || correo == null || correo.isBlank()
                || password == null || password.isBlank()
                || confirmarPassword == null
                || confirmarPassword.isBlank()) {

            return error(
                    redirectAttributes,
                    "Todos los campos son obligatorios."
            );
        }

        String nombreLimpio = nombre.trim();

        String usernameLimpio = username.trim();

        String correoLimpio = correo.trim()
                .toLowerCase(Locale.ROOT);

        // =============================================
        // VALIDAR ROL
        // =============================================

        Rol rolSeleccionado;

        try {

            rolSeleccionado = Rol.valueOf(
                    rol.trim().toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException |
                 NullPointerException ex) {

            return error(
                    redirectAttributes,
                    "El rol seleccionado no es vÃ¡lido."
            );
        }

        // =============================================
        // VALIDAR CONTRASEÃ‘A
        // =============================================

        if (password.length() < 8) {

            return error(
                    redirectAttributes,
                    "La contraseÃ±a debe tener al menos 8 caracteres."
            );
        }

        if (!password.equals(confirmarPassword)) {

            return error(
                    redirectAttributes,
                    "Las contraseÃ±as no coinciden."
            );
        }

        // =============================================
        // VALIDAR USUARIO DUPLICADO
        // =============================================

        if (usuarioRepository.existsByUsername(usernameLimpio)) {

            return error(
                    redirectAttributes,
                    "El nombre de usuario ya existe."
            );
        }

        // =============================================
        // VALIDAR EMAIL DUPLICADO
        // =============================================

        if (usuarioRepository.existsByEmail(correoLimpio)) {

            return error(
                    redirectAttributes,
                    "El correo electrÃ³nico ya estÃ¡ registrado."
            );
        }

        // =============================================
        // CREAR USUARIO
        // =============================================

        Usuario usuario = new Usuario();

        usuario.setNombre(nombreLimpio);
        usuario.setUsername(usernameLimpio);
        usuario.setEmail(correoLimpio);

        usuario.setPassword(
                passwordEncoder.encode(password)
        );

        usuario.setRol(rolSeleccionado);
        usuario.setActivo(true);

        // =============================================
        // GUARDAR EN MONGODB
        // =============================================

        usuarioRepository.save(usuario);

        // =============================================
        // CONFIRMACIÃ“N
        // =============================================

        redirectAttributes.addFlashAttribute(
                "mensajeExito",
                "El usuario fue registrado correctamente."
        );

        return "redirect:/admin/usuarios/nuevo";
    }

    // =====================================================
    // MENSAJES DE ERROR
    // =====================================================

    private String error(
            RedirectAttributes redirectAttributes,
            String mensaje) {

        redirectAttributes.addFlashAttribute(
                "errorRegistro",
                mensaje
        );

        return "redirect:/admin/usuarios/nuevo";
    }
}

