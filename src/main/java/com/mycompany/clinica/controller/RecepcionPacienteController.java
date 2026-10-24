
package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Paciente;
import com.mycompany.clinica.repository.PacienteRepository;
import com.mycompany.clinica.service.PacienteService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/recepcion/pacientes")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
public class RecepcionPacienteController {

    private static final Logger log =
            LoggerFactory.getLogger(RecepcionPacienteController.class);

    private final PacienteRepository pacienteRepository;
    private final PacienteService pacienteService;

    public RecepcionPacienteController(
            PacienteRepository pacienteRepository,
            PacienteService pacienteService) {

        this.pacienteRepository = pacienteRepository;
        this.pacienteService = pacienteService;
    }

    /*
     * PROTECCIÃ“N DE CAMPOS
     *
     * Impide que un formulario modifique directamente
     * campos administrados por el servidor.
     */
    @InitBinder("paciente")
    public void configurarBinder(WebDataBinder binder) {
        binder.setDisallowedFields(
                "id",
                "fechaRegistro"
        );
    }

    /*
     * 1. LISTAR PACIENTES
     */
    @GetMapping
    public String listar(Model model) {

        List<Paciente> pacientes =
                pacienteRepository.findAll();

        // Usar el mismo criterio del administrador: apellidos y nombres.
        pacientes.sort(
                Comparator.comparing(
                        (Paciente p) -> p.getApellidos() == null ? "" : p.getApellidos(),
                        String.CASE_INSENSITIVE_ORDER
                ).thenComparing(
                        p -> p.getNombres() == null ? "" : p.getNombres(),
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        long activos = pacientes.stream()
                .filter(Paciente::isActivo)
                .count();

        model.addAttribute("pacientes", pacientes);
        model.addAttribute("totalPacientes", pacientes.size());
        model.addAttribute("activos", activos);

        return "recepcion-pacientes";
    }

    /*
     * 2. FORMULARIO NUEVO PACIENTE
     */
    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        Paciente paciente = new Paciente();

        model.addAttribute("paciente", paciente);
        model.addAttribute("modoEdicion", false);

        return "recepcion-paciente-form";
    }

    /*
     * 3. FORMULARIO EDITAR PACIENTE
     */
    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable String id,
            Model model,
            RedirectAttributes redirect) {

        try {

            Paciente paciente =
                    pacienteService.buscarPorId(id);

            model.addAttribute("paciente", paciente);
            model.addAttribute("modoEdicion", true);

            return "recepcion-paciente-form";

        } catch (RuntimeException ex) {

            log.warn(
                    "No se pudo consultar el paciente con ID {}",
                    id,
                    ex
            );

            redirect.addFlashAttribute(
                    "error",
                    "No fue posible encontrar el paciente solicitado."
            );

            return "redirect:/recepcion/pacientes";
        }
    }

    /*
     * 4. GUARDAR NUEVO PACIENTE
     */
    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("paciente") Paciente paciente,
            BindingResult errores,
            Model model,
            RedirectAttributes redirect) {

        // Campos controlados por el servidor.
        paciente.setId(null);
        paciente.setActivo(true);

        if (paciente.getFechaNacimiento() == null
                && !errores.hasFieldErrors("fechaNacimiento")) {

            errores.rejectValue(
                    "fechaNacimiento",
                    "fechaNacimiento.obligatoria",
                    "La fecha de nacimiento es obligatoria."
            );
        }

        if (errores.hasErrors()) {

            model.addAttribute("modoEdicion", false);

            return "recepcion-paciente-form";
        }

        try {

            pacienteService.guardarPaciente(paciente);

            redirect.addFlashAttribute(
                    "mensaje",
                    "Paciente registrado correctamente."
            );

            return "redirect:/recepcion/pacientes";

        } catch (RuntimeException ex) {

            log.error(
                    "Error al registrar un paciente",
                    ex
            );

            model.addAttribute("modoEdicion", false);

            model.addAttribute(
                    "error",
                    "No fue posible registrar el paciente. "
                    + "Verifica los datos e intÃ©ntalo nuevamente."
            );

            return "recepcion-paciente-form";
        }
    }

    /*
     * 5. ACTUALIZAR PACIENTE
     */
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable String id,
            @Valid @ModelAttribute("paciente") Paciente datos,
            BindingResult errores,
            Model model,
            RedirectAttributes redirect) {

        if (datos.getFechaNacimiento() == null
                && !errores.hasFieldErrors("fechaNacimiento")) {

            errores.rejectValue(
                    "fechaNacimiento",
                    "fechaNacimiento.obligatoria",
                    "La fecha de nacimiento es obligatoria."
            );
        }

        if (errores.hasErrors()) {

            datos.setId(id);

            model.addAttribute("modoEdicion", true);

            return "recepcion-paciente-form";
        }

        try {

            /*
             * El servicio debe conservar:
             *
             * - ID del paciente
             * - Fecha de registro
             * - Estado activo/inactivo se actualiza desde el formulario
             *
             * Solo se actualizan los datos permitidos.
             */

            pacienteService.actualizarPaciente(id, datos);

            redirect.addFlashAttribute(
                    "mensaje",
                    "Paciente actualizado correctamente."
            );

            return "redirect:/recepcion/pacientes";

        } catch (RuntimeException ex) {

            log.error(
                    "Error al actualizar el paciente con ID {}",
                    id,
                    ex
            );

            datos.setId(id);

            model.addAttribute("modoEdicion", true);

            model.addAttribute(
                    "error",
                    "No fue posible actualizar el paciente. "
                    + "Verifica la informaciÃ³n e intÃ©ntalo nuevamente."
            );

            return "recepcion-paciente-form";
        }
    }
}

