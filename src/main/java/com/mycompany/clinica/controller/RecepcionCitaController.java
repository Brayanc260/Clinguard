
package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Cita;
import com.mycompany.clinica.entity.Usuario;
import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.repository.UsuarioRepository;
import com.mycompany.clinica.entity.Paciente;
import com.mycompany.clinica.repository.PacienteRepository;
import com.mycompany.clinica.service.CitaService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/recepcion/citas")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
public class RecepcionCitaController {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RecepcionCitaController.class
            );

    private static final String REDIRECT_CITAS =
            "redirect:/recepcion/citas";

    private static final String VISTA_CITAS =
            "recepcion-citas";

    // ==========================================
    // SERVICIO
    // ==========================================

    private final CitaService citaService;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;

    public RecepcionCitaController(CitaService citaService, PacienteRepository pacienteRepository, UsuarioRepository usuarioRepository) {
        this.citaService = citaService;
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ==========================================
    // 1. LISTAR CITAS
    // GET /recepcion/citas
    // ==========================================

    @GetMapping
    public String listar(Model model) {

        Map<String, String> nombres = nombresPorReferencia();
        List<Map<String, String>> citas =
                citaService.listarOrdenadas()
                        .stream()
                        .map(cita -> convertirParaRecepcion(cita, nombres))
                        .toList();

        model.addAttribute("citas", citas);
        cargarSelectores(model);
        // El HTML usa th:object="${paciente}" en el formulario.
        model.addAttribute("paciente", new Paciente());

        return VISTA_CITAS;
    }

    // ==========================================
    // 2. REGISTRAR NUEVA CITA
    // ==========================================

    @PostMapping("/registrar")
    public String registrar(
            @RequestParam String pacienteId,
            @RequestParam String medicoUsername,
            @RequestParam String fecha,
            @RequestParam String hora,
            RedirectAttributes redirect) {

        try {

            validarObligatorio(
                    pacienteId,
                    "Debes seleccionar un paciente registrado."
            );

            validarObligatorio(
                    medicoUsername,
                    "El mÃ©dico es obligatorio."
            );

            validarObligatorio(
                    fecha,
                    "La fecha es obligatoria."
            );

            validarObligatorio(
                    hora,
                    "La hora es obligatoria."
            );

            citaService.registrarPacienteDesdeRecepcion(
                    pacienteId.trim(),
                    medicoUsername.trim(),
                    fecha.trim(),
                    hora.trim()
            );

            redirect.addFlashAttribute(
                    "mensaje",
                    "La cita fue registrada correctamente."
            );

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            redirect.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );

        } catch (Exception ex) {

            log.error(
                    "Error inesperado al registrar una cita",
                    ex
            );

            redirect.addFlashAttribute(
                    "error",
                    "No fue posible registrar la cita. "
                    + "IntÃ©ntalo nuevamente."
            );
        }

        return REDIRECT_CITAS;
    }

    // ==========================================
    // 3. REPROGRAMAR CITA
    // ==========================================

    @PostMapping("/reprogramar")
    public String reprogramar(
            @RequestParam String id,
            @RequestParam String nuevaFecha,
            @RequestParam String nuevaHora,
            RedirectAttributes redirect) {

        try {

            validarObligatorio(
                    id,
                    "El identificador de la cita es obligatorio."
            );

            validarObligatorio(
                    nuevaFecha,
                    "La nueva fecha es obligatoria."
            );

            validarObligatorio(
                    nuevaHora,
                    "La nueva hora es obligatoria."
            );

            citaService.reprogramarDesdeRecepcion(
                    id.trim(),
                    nuevaFecha.trim(),
                    nuevaHora.trim()
            );

            redirect.addFlashAttribute(
                    "mensaje",
                    "La cita fue reprogramada correctamente."
            );

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            redirect.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );

        } catch (Exception ex) {

            log.error(
                    "Error al reprogramar la cita",
                    ex
            );

            redirect.addFlashAttribute(
                    "error",
                    "No fue posible reprogramar la cita."
            );
        }

        return REDIRECT_CITAS;
    }

    // ==========================================
    // 4. CONFIRMAR CITA
    // ==========================================

    @PostMapping("/confirmar")
    public String confirmar(
            @RequestParam String id,
            RedirectAttributes redirect) {

        try {

            validarObligatorio(
                    id,
                    "El identificador de la cita es obligatorio."
            );

            citaService.confirmarDesdeRecepcion(
                    id.trim()
            );

            redirect.addFlashAttribute(
                    "mensaje",
                    "La cita fue confirmada correctamente."
            );

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            redirect.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );

        } catch (Exception ex) {

            log.error(
                    "Error al confirmar la cita",
                    ex
            );

            redirect.addFlashAttribute(
                    "error",
                    "No fue posible confirmar la cita."
            );
        }

        return REDIRECT_CITAS;
    }

    // ==========================================
    // 5. CANCELAR CITA
    // ==========================================

    @PostMapping("/cancelar")
    public String cancelar(
            @RequestParam String id,
            RedirectAttributes redirect) {

        try {

            validarObligatorio(
                    id,
                    "El identificador de la cita es obligatorio."
            );

            citaService.cancelarDesdeRecepcion(
                    id.trim()
            );

            redirect.addFlashAttribute(
                    "mensaje",
                    "La cita fue cancelada correctamente."
            );

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            redirect.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );

        } catch (Exception ex) {

            log.error(
                    "Error al cancelar la cita",
                    ex
            );

            redirect.addFlashAttribute(
                    "error",
                    "No fue posible cancelar la cita."
            );
        }

        return REDIRECT_CITAS;
    }

    // ==========================================
    // 6. CONSULTAR AGENDA DE MÃ‰DICO
    // ==========================================

    @GetMapping("/agenda")
    public String consultarAgenda(
            @RequestParam String medicoUsername,
            @RequestParam String fecha,
            Model model,
            RedirectAttributes redirect) {

        try {

            validarObligatorio(
                    medicoUsername,
                    "Debes indicar el mÃ©dico."
            );

            validarObligatorio(
                    fecha,
                    "Debes seleccionar una fecha."
            );

            Map<String, String> nombres = nombresPorReferencia();
            List<Map<String, String>> citas =
                    citaService.obtenerAgendaMedico(
                            medicoUsername.trim(),
                            fecha.trim()
                    )
                    .stream()
                    .map(cita -> convertirParaRecepcion(cita, nombres))
                    .toList();

            model.addAttribute("citas", citas);
        cargarSelectores(model);
            model.addAttribute("paciente", new Paciente());

            model.addAttribute(
                    "medicoSeleccionado",
                    medicoUsername.trim()
            );

            model.addAttribute(
                    "fechaSeleccionada",
                    fecha.trim()
            );

            return VISTA_CITAS;

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            redirect.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );

        } catch (Exception ex) {

            log.error(
                    "Error al consultar la agenda mÃ©dica",
                    ex
            );

            redirect.addFlashAttribute(
                    "error",
                    "No fue posible consultar la agenda."
            );
        }

        return REDIRECT_CITAS;
    }

    private void cargarSelectores(Model model) {
        List<Paciente> pacientes = pacienteRepository.findByActivoTrue().stream()
                .sorted(java.util.Comparator.comparing((Paciente p) -> texto(p.getApellidos()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(p -> texto(p.getNombres()), String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<Usuario> medicos = usuarioRepository.findByRol(Rol.MEDICO).stream()
                .filter(Usuario::isActivo)
                .sorted(java.util.Comparator.comparing(u -> texto(u.getNombre()), String.CASE_INSENSITIVE_ORDER))
                .toList();
        model.addAttribute("pacientesActivos", pacientes);
        model.addAttribute("medicosActivos", medicos);
    }

    // ==========================================
    // 7. DATOS ADMINISTRATIVOS PARA THYMELEAF
    // ==========================================

    private Map<String, String> convertirParaRecepcion(
            Cita cita, Map<String, String> nombres) {

        Map<String, String> datos = new HashMap<>();

        datos.put(
                "id",
                texto(cita.getId())
        );

        datos.put(
                "fecha",
                texto(cita.getFecha())
        );

        datos.put(
                "hora",
                texto(cita.getHora())
        );

        datos.put(
                "estado",
                texto(cita.getEstado())
        );

        datos.put(
                "usuarioUsername",
                texto(cita.getUsuarioUsername())
        );

        // Mostrar el nombre real sin alterar la referencia guardada en MongoDB.
        String nombre = nombreVerificado(cita.getPacienteId(), nombres);
        if (nombre == null) {
            nombre = nombreVerificado(cita.getUsuarioUsername(), nombres);
        }
        datos.put("nombrePaciente", nombre != null ? nombre
                : (texto(cita.getUsuarioUsername()).isBlank()
                    ? "Paciente sin vinculaciÃ³n verificada"
                    : texto(cita.getUsuarioUsername())));

        datos.put(
                "medicoUsername",
                texto(cita.getMedicoUsername())
        );

        /*
         * No se envÃ­an a la vista:
         *
         * - Motivo de consulta
         * - DiagnÃ³stico
         * - Tratamiento
         * - Observaciones clÃ­nicas
         * - Historial mÃ©dico
         */

        return datos;
    }

    // Resolver solo referencias exactas (ID o documento).
    // Nunca asociar pacientes por coincidencia de nombre.
    private Map<String, String> nombresPorReferencia() {
        Map<String, String> nombres = new HashMap<>();
        Set<String> ambiguas = new HashSet<>();
        for (Paciente paciente : pacienteRepository.findAll()) {
            String nombre = (texto(paciente.getNombres()) + " "
                    + texto(paciente.getApellidos())).trim();
            agregarReferencia(nombres, ambiguas, paciente.getId(), nombre);
            agregarReferencia(nombres, ambiguas, paciente.getDocumento(), nombre);
        }
        ambiguas.forEach(nombres::remove);
        return nombres;
    }

    private void agregarReferencia(Map<String, String> nombres, Set<String> ambiguas,
                                  String referencia, String nombre) {
        if (referencia == null || referencia.isBlank() || nombre.isBlank()) return;
        String clave = referencia.trim();
        String existente = nombres.putIfAbsent(clave, nombre);
        if (existente != null && !existente.equals(nombre)) ambiguas.add(clave);
    }

    private String nombreVerificado(String referencia, Map<String, String> nombres) {
        return referencia == null ? null : nombres.get(referencia.trim());
    }

    // ==========================================
    // 8. VALIDAR PARÃMETROS OBLIGATORIOS
    // ==========================================

    private void validarObligatorio(
            String valor,
            String mensaje) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    // ==========================================
    // 9. EVITAR VALORES NULOS
    // ==========================================

    private String texto(String valor) {

        return valor == null ? "" : valor;
    }
}

