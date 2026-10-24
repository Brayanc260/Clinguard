package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Paciente;
import com.mycompany.clinica.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pacientes")
public class PacienteViewController {

        private static final int PACIENTES_POR_PAGINA = 40;
        private final PacienteService pacienteService;

        public PacienteViewController(PacienteService pacienteService) {
                this.pacienteService = pacienteService;
        }

        // ADMIN, MEDICO y ENFERMERO pueden consultar pacientes.
        @GetMapping({ "", "/" })
        public String listarPacientes(
                        @RequestParam(name = "pagina", defaultValue = "0") int pagina,
                        @RequestParam(name = "buscar", defaultValue = "") String buscar,
                        Authentication authentication,
                        Model model) {

                if (!autenticado(authentication))
                        return "redirect:/login";
                if (!puedeConsultar(authentication))
                        return "redirect:/error/403";

                pagina = Math.max(0, pagina);
                buscar = buscar == null ? "" : buscar.trim();
                Page<Paciente> resultado = pacienteService.listarPacientes(pagina, PACIENTES_POR_PAGINA, buscar);
                if (resultado.getTotalPages() > 0 && pagina >= resultado.getTotalPages()) {
                        return "redirect:/pacientes?pagina=" + (resultado.getTotalPages() - 1)
                                        + "&buscar="
                                        + java.net.URLEncoder.encode(buscar, java.nio.charset.StandardCharsets.UTF_8);
                }

                model.addAttribute("pacientes", resultado.getContent());
                model.addAttribute("buscar", buscar);
                model.addAttribute("paginaActual", pagina);
                model.addAttribute("totalPaginas", resultado.getTotalPages());
                model.addAttribute("totalPacientes", resultado.getTotalElements());
                model.addAttribute("tieneAnterior", resultado.hasPrevious());
                model.addAttribute("tieneSiguiente", resultado.hasNext());
                model.addAttribute("numeroInicial", resultado.isEmpty() ? 0 : pagina * PACIENTES_POR_PAGINA + 1);
                model.addAttribute("numeroFinal", Math.min((long) (pagina + 1) * PACIENTES_POR_PAGINA,
                                resultado.getTotalElements()));
                return "pacientes";
        }

        // ADMIN y MEDICO pueden registrar y actualizar datos de pacientes.
        @GetMapping("/nuevo")
        public String nuevoPaciente(Authentication authentication, Model model) {
                if (!autenticado(authentication))
                        return "redirect:/login";
                if (!puedeCrearEditar(authentication))
                        return "redirect:/error/403";
                model.addAttribute("paciente", new Paciente());
                model.addAttribute("modoEdicion", false);
                return "paciente-form";
        }

        @PostMapping("/guardar")
        public String guardarPaciente(
                        @Valid @ModelAttribute("paciente") Paciente paciente,
                        BindingResult bindingResult,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {

                if (!autenticado(authentication))
                        return "redirect:/login";
                if (!puedeCrearEditar(authentication))
                        return "redirect:/error/403";
                if (bindingResult.hasErrors()) {
                        model.addAttribute("modoEdicion", false);
                        return "paciente-form";
                }
                try {
                        // El servicio debe validar duplicados y los campos gestionados por el servidor.
                        pacienteService.guardarPaciente(paciente);
                        redirectAttributes.addFlashAttribute("exito", "Paciente registrado correctamente.");
                        return "redirect:/pacientes";
                } catch (IllegalArgumentException | IllegalStateException e) {
                        model.addAttribute("error", e.getMessage());
                        model.addAttribute("modoEdicion", false);
                        return "paciente-form";
                }
        }

        @GetMapping("/ver/{id}")
        public String verPaciente(@PathVariable String id,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {
                if (!autenticado(authentication))
                        return "redirect:/login";
                if (!puedeConsultar(authentication))
                        return "redirect:/error/403";
                try {
                        model.addAttribute("paciente", pacienteService.buscarPorId(id));
                        return "paciente-detalle";
                } catch (IllegalArgumentException | IllegalStateException e) {
                        redirectAttributes.addFlashAttribute("error", e.getMessage());
                        return "redirect:/pacientes";
                }
        }

        @GetMapping("/editar/{id}")
        public String editarPaciente(@PathVariable String id,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {
                if (!autenticado(authentication))
                        return "redirect:/login";
                if (!puedeCrearEditar(authentication))
                        return "redirect:/error/403";
                try {
                        model.addAttribute("paciente", pacienteService.buscarPorId(id));
                        model.addAttribute("modoEdicion", true);
                        return "paciente-form";
                } catch (IllegalArgumentException | IllegalStateException e) {
                        redirectAttributes.addFlashAttribute("error", e.getMessage());
                        return "redirect:/pacientes";
                }
        }

        @PostMapping("/actualizar/{id}")
        public String actualizarPaciente(@PathVariable String id,
                        @Valid @ModelAttribute("paciente") Paciente paciente,
                        BindingResult bindingResult,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {
                if (!autenticado(authentication))
                        return "redirect:/login";
                if (!puedeCrearEditar(authentication))
                        return "redirect:/error/403";

                // El ID de la ruta es la referencia de la actualizaciÃƒÆ’Ã‚Â³n, no el enviado por el
                // formulario.
                paciente.setId(id);
                if (bindingResult.hasErrors()) {
                        model.addAttribute("modoEdicion", true);
                        return "paciente-form";
                }
                try {
                        pacienteService.actualizarPaciente(id, paciente);
                        redirectAttributes.addFlashAttribute("exito", "Paciente actualizado correctamente.");
                        return "redirect:/pacientes";
                } catch (IllegalArgumentException | IllegalStateException e) {
                        model.addAttribute("error", e.getMessage());
                        model.addAttribute("modoEdicion", true);
                        return "paciente-form";
                }
        }

        // Solo ADMIN puede eliminar (recomendable que el servicio haga baja lÃƒÆ’Ã‚Â³gica).
        @PostMapping("/eliminar/{id}")
        public String eliminarPaciente(@PathVariable String id,
                        @RequestParam(name = "pagina", defaultValue = "0") int pagina,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
                if (!autenticado(authentication))
                        return "redirect:/login";
                if (!tieneRol(authentication, "ROLE_ADMIN"))
                        return "redirect:/error/403";
                try {
                        pacienteService.eliminarPaciente(id);
                        redirectAttributes.addFlashAttribute("exito", "OperaciÃƒÆ’Ã‚Â³n realizada correctamente.");
                } catch (IllegalArgumentException | IllegalStateException e) {
                        redirectAttributes.addFlashAttribute("error", e.getMessage());
                }
                return "redirect:/pacientes?pagina=" + Math.max(0, pagina);
        }

        private boolean autenticado(Authentication authentication) {
                return authentication != null
                                && authentication.isAuthenticated()
                                && !(authentication instanceof AnonymousAuthenticationToken);
        }

        private boolean tieneRol(Authentication authentication, String rol) {
                return autenticado(authentication)
                                && authentication.getAuthorities().stream()
                                                .anyMatch(a -> rol.equals(a.getAuthority()));
        }

        private boolean puedeConsultar(Authentication authentication) {
                return tieneRol(authentication, "ROLE_ADMIN")
                                || tieneRol(authentication, "ROLE_MEDICO")
                                || tieneRol(authentication, "ROLE_ENFERMERO");
        }

        private boolean puedeCrearEditar(Authentication authentication) {
                return tieneRol(authentication, "ROLE_ADMIN")
                                || tieneRol(authentication, "ROLE_MEDICO");
        }
}
