package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Usuario;
import com.mycompany.clinica.repository.UsuarioRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PerfilController {

    private final UsuarioRepository usuarioRepository;

    public PerfilController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // =========================================================
    // MOSTRAR PERFIL
    // =========================================================
    @GetMapping("/perfil")
    public String mostrarPerfil(
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();

        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElse(null);

        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);

        return "perfil";
    }

    // =========================================================
    // ACTUALIZAR PERFIL
    // =========================================================
    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(

            Authentication authentication,

            @RequestParam String nombre,

            @RequestParam(required = false)
            String email,

            @RequestParam(required = false)
            String telefono,

            @RequestParam(required = false)
            String documento,

            @RequestParam(required = false)
            String fechaNacimiento,

            @RequestParam(required = false)
            String direccion,

            @RequestParam(required = false)
            String ciudad,

            @RequestParam(required = false)
            String genero,

            @RequestParam(required = false)
            String biografia,

            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {

            String username = authentication.getName();

            Usuario usuario = usuarioRepository
                    .findByUsername(username)
                    .orElse(null);

            // =====================================================
            // VALIDAR USUARIO
            // =====================================================
            if (usuario == null) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "No fue posible encontrar el usuario."
                );

                return "redirect:/perfil";
            }

            // =====================================================
            // VALIDAR NOMBRE
            // =====================================================
            if (nombre == null || nombre.trim().isEmpty()) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "El nombre no puede estar vacÃ­o."
                );

                return "redirect:/perfil";
            }

            // =====================================================
            // ACTUALIZAR DATOS
            // =====================================================
            usuario.setNombre(
                    nombre.trim()
            );

            usuario.setEmail(
                    email != null
                            ? email.trim()
                            : ""
            );

            usuario.setTelefono(
                    telefono != null
                            ? telefono.trim()
                            : ""
            );

            usuario.setDocumento(
                    documento != null
                            ? documento.trim()
                            : ""
            );

            usuario.setFechaNacimiento(
                    fechaNacimiento != null
                            ? fechaNacimiento.trim()
                            : ""
            );

            usuario.setDireccion(
                    direccion != null
                            ? direccion.trim()
                            : ""
            );

            usuario.setCiudad(
                    ciudad != null
                            ? ciudad.trim()
                            : ""
            );

            usuario.setGenero(
                    genero != null
                            ? genero.trim()
                            : ""
            );

            usuario.setBiografia(
                    biografia != null
                            ? biografia.trim()
                            : ""
            );

            // =====================================================
            // GUARDAR EN MONGODB
            // =====================================================
            usuarioRepository.save(usuario);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Perfil actualizado correctamente."
            );

            return "redirect:/perfil";

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No fue posible actualizar el perfil."
            );

            return "redirect:/perfil";
        }
    }
}
