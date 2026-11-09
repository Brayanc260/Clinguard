package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Cita;
import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;
import com.mycompany.clinica.entity.Paciente;
import com.mycompany.clinica.repository.CitaRepository;
import com.mycompany.clinica.repository.UsuarioRepository;
import com.mycompany.clinica.repository.PacienteRepository;
import com.mycompany.clinica.service.CitaService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/citas")
public class CitaController {

    private final CitaService citaService;
    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository; // <-- NUEVO

    public CitaController(CitaService citaService, CitaRepository citaRepository, 
                          UsuarioRepository usuarioRepository, PacienteRepository pacienteRepository) {
        this.citaService = citaService;
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
    }

    @GetMapping
    public String listarCitas(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";

        String username = authentication.getName();
        boolean esAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean esMedico = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEDICO"));
        boolean esUsuario = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USUARIO"));

        List<Cita> citas;
        if (esAdmin || esMedico) {
            citas = citaService.listarTodas();
        } else if (esUsuario) {
            citas = citaService.buscarPorUsuario(username);
        } else {
            citas = citaService.listarTodas();
        }

        // CREAMOS UN MAPA DE NOMBRES PARA QUE EL HTML LOS MUESTRE
        Map<String, String> mapaPacientes = new HashMap<>();
        pacienteRepository.findAll().forEach(p -> {
            String nombreCompleto = (p.getNombres() + " " + p.getApellidos()).trim();
            mapaPacientes.put(p.getId(), nombreCompleto);
        });

        model.addAttribute("citas", citas);
        model.addAttribute("mapaPacientes", mapaPacientes); // Enviamos el mapa
        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("esMedico", esMedico);
        model.addAttribute("esUsuario", esUsuario);
        model.addAttribute("username", username);

        return "citas";
    }

    @GetMapping("/nueva")
    public String nuevaCita(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";

        Cita cita = new Cita();
        cita.setFecha(LocalDate.now(ZoneId.of("America/Bogota")).toString());
        cita.setHora(LocalTime.now(ZoneId.of("America/Bogota")).withSecond(0).withNano(0).toString());

        model.addAttribute("cita", cita);
        model.addAttribute("pacientes", pacienteRepository.findAll());
        model.addAttribute("medicos", usuarioRepository.findByRol(Rol.MEDICO));
        model.addAttribute("modoEdicion", false);

        return "cita-form";
    }

    @PostMapping("/guardar")
    public String guardarCita(@ModelAttribute("cita") Cita cita, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        try {
            cita.setEstado("PROGRAMADA");
            citaService.guardar(cita);
            redirectAttributes.addFlashAttribute("exito", "Cita registrada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar la cita: " + e.getMessage());
            return "redirect:/citas/nueva";
        }
        return "redirect:/citas";
    }

    @GetMapping("/editar/{id}")
    public String editarCita(@PathVariable(name = "id") String id, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        Optional<Cita> citaOptional = citaRepository.findById(id);
        if (citaOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La cita no existe.");
            return "redirect:/citas";
        }

        model.addAttribute("cita", citaOptional.get());
        model.addAttribute("pacientes", pacienteRepository.findAll());
        model.addAttribute("medicos", usuarioRepository.findByRol(Rol.MEDICO));
        model.addAttribute("modoEdicion", true);

        return "cita-form";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizarCita(@PathVariable(name = "id") String id, @ModelAttribute("cita") Cita citaActualizada,
                                 Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        try {
            Optional<Cita> originalOpt = citaRepository.findById(id);
            if (originalOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "La cita no existe.");
                return "redirect:/citas";
            }

            Cita original = originalOpt.get();
            original.setFecha(citaActualizada.getFecha());
            original.setHora(citaActualizada.getHora());
            original.setMotivo(citaActualizada.getMotivo());
            original.setObservaciones(citaActualizada.getObservaciones());
            original.setMedicoUsername(citaActualizada.getMedicoUsername());
            original.setEstado(citaActualizada.getEstado());

            citaService.guardar(original);
            redirectAttributes.addFlashAttribute("exito", "Cita actualizada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar la cita: " + e.getMessage());
            return "redirect:/citas/editar/" + id;
        }
        return "redirect:/citas";
    }

    @PostMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable(name = "id") String id, @RequestParam(name = "estado") String estado,
                                Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        try {
            Optional<Cita> citaOpt = citaRepository.findById(id);
            if (citaOpt.isPresent()) {
                Cita cita = citaOpt.get();
                cita.setEstado(estado.toUpperCase());
                citaRepository.save(cita);
                redirectAttributes.addFlashAttribute("exito", "Estado de la cita actualizado a " + estado);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cambiar el estado: " + e.getMessage());
        }
        return "redirect:/citas";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarCita(@PathVariable(name = "id") String id, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null) return "redirect:/login";

        try {
            citaRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("exito", "Cita eliminada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la cita.");
        }
        return "redirect:/citas";
    }
}