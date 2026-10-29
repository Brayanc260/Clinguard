
package com.mycompany.clinica.controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mycompany.clinica.entity.Cita;
import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;
import com.mycompany.clinica.entity.Paciente;
import com.mycompany.clinica.repository.PacienteRepository;
import com.mycompany.clinica.repository.UsuarioRepository;
import com.mycompany.clinica.service.CitaService;
import com.mycompany.clinica.service.NotificacionService;

@Controller
public class CitaController {

    // =====================================================
    // SERVICIOS
    // =====================================================

    private final CitaService citaService;
    private final NotificacionService notificacionService;
    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;

    public CitaController(
            CitaService citaService,
            NotificacionService notificacionService,
            PacienteRepository pacienteRepository,
            UsuarioRepository usuarioRepository) {

        this.citaService = citaService;
        this.notificacionService = notificacionService;
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // =====================================================
    // MOSTRAR CITAS SEGÚN EL ROL
    // =====================================================

    @GetMapping("/citas")
    public String mostrarCitas(
            Authentication authentication,
            Model model) {

        if (!estaAutenticado(authentication)) {
            return "redirect:/login";
        }

        String username = authentication.getName();

        boolean esAdmin = tieneRol(
            authentication, "ROLE_ADMIN");

        boolean esMedico = tieneRol(
            authentication, "ROLE_MEDICO");

        boolean esEnfermero = tieneRol(
            authentication, "ROLE_ENFERMERO");

        boolean esUsuario = tieneRol(
            authentication, "ROLE_USUARIO");

        model.addAttribute("username", username);
        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("esMedico", esMedico);
        model.addAttribute("esEnfermero", esEnfermero);
        model.addAttribute("esUsuario", esUsuario);

        List<Cita> citas;

        if (esUsuario) {

            citas = citaService.buscarPorUsuario(username);

        } else if (esMedico) {

            citas = citaService.buscarPorMedico(username);

        } else if (esEnfermero) {

            citas = citaService.buscarPorEnfermero(username);

        } else if (esAdmin) {

            citas = citaService.listarTodas();

        } else {

            return "redirect:/error/403";
        }

        model.addAttribute("citas", citas);
        model.addAttribute("totalCitas", citas.size());

        // Resolver solo referencias verificables: ID de MongoDB o documento.
        // No asociar nombres libres de citas antiguas por similitud.
        if (esAdmin || esMedico || esEnfermero) {
            Map<String, String> nombresPacientesPorCita = new HashMap<>();
            Map<String, Paciente> pacientesPorReferencia = new HashMap<>();
            java.util.Set<String> referenciasAmbiguas = new java.util.HashSet<>();
            for (Paciente paciente : pacienteRepository.findAll()) {
                if (paciente.getId() != null && !paciente.getId().isBlank()) {
                    String clave = paciente.getId().trim();
                    if (pacientesPorReferencia.putIfAbsent(clave, paciente) != null) referenciasAmbiguas.add(clave);
                }
                if (paciente.getDocumento() != null && !paciente.getDocumento().isBlank()) {
                    String clave = paciente.getDocumento().trim();
                    Paciente previo = pacientesPorReferencia.putIfAbsent(clave, paciente);
                    if (previo != null && !previo.getId().equals(paciente.getId())) referenciasAmbiguas.add(clave);
                }
            }
            for (Cita cita : citas) {
                if (cita.getId() == null) continue;
                // Las citas del directorio utilizan pacienteId; las antiguas pueden
                // contener un ID o documento verificado en usuarioUsername.
                String referencia = cita.getPacienteId();
                if (referencia == null || referencia.isBlank()) {
                    referencia = cita.getUsuarioUsername();
                }
                if (referencia == null || referencia.isBlank()) continue;
                String clave = referencia.trim();
                Paciente paciente = referenciasAmbiguas.contains(clave) ? null : pacientesPorReferencia.get(clave);
                if (paciente != null) {
                    String nombres = paciente.getNombres() == null ? "" : paciente.getNombres();
                    String apellidos = paciente.getApellidos() == null ? "" : paciente.getApellidos();
                    String nombreCompleto = (nombres + " " + apellidos).trim();
                    nombresPacientesPorCita.put(cita.getId(),
                            nombreCompleto.isEmpty() ? "Paciente registrado" : nombreCompleto);
                }
            }
            model.addAttribute("nombresPacientesPorCita", nombresPacientesPorCita);
        }

        return "citas";
    }

    // =====================================================
    // FORMULARIO DE NUEVA CITA
    // =====================================================

    @GetMapping("/citas/nueva")
    public String nuevaCita(
            Authentication authentication,
            Model model) {

        if (!puedeGestionar(authentication)) {
            return "redirect:/error/403";
        }

        Cita cita = new Cita();

        cita.setFecha(LocalDate.now().toString());
        cita.setEstado("PROGRAMADA");

        if (tieneRol(authentication, "ROLE_MEDICO")) {

            cita.setMedicoUsername(
                authentication.getName()
            );
        }

        if (tieneRol(authentication, "ROLE_ENFERMERO")) {

            cita.setEnfermeroUsername(
                authentication.getName()
            );
        }

        model.addAttribute("cita", cita);
        model.addAttribute("estados", estados());
        // El formulario puede ofrecer un selector de pacientes registrados.
        // La autorización efectiva de cada operación se comprueba en el servidor.
        model.addAttribute("pacientesRegistrados", pacienteRepository.findAll());
        cargarProfesionales(model);

        return "cita-form";
    }

    // =====================================================
    // GUARDAR NUEVA CITA
    // =====================================================

    @PostMapping("/citas/guardar")
    public String guardarCita(
            @ModelAttribute Cita cita,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (!puedeGestionar(authentication)) {
            return "redirect:/error/403";
        }

        // La identidad del profesional se establece en el servidor ANTES de validar.
        // No se confía en el campo medicoUsername enviado por el navegador.
        if (tieneRol(authentication, "ROLE_MEDICO")) {
            cita.setMedicoUsername(authentication.getName());
        }
        if (tieneRol(authentication, "ROLE_ENFERMERO")) {
            cita.setEnfermeroUsername(authentication.getName());
        }

        if (!datosValidos(cita, redirectAttributes)) {
            return "redirect:/citas/nueva";
        }

        if (cita.getEstado() == null
                || cita.getEstado().isBlank()) {

            cita.setEstado("PROGRAMADA");
        }

        if (!estadoValido(cita.getEstado())) {

            redirectAttributes.addFlashAttribute(
                "error",
                "El estado seleccionado no es válido."
            );

            return "redirect:/citas/nueva";
        }

        // Una nueva cita siempre debe tener ID nuevo
        cita.setId(null);

        // GUARDAR EN MONGODB CON MANEJO DE VALIDACIONES
        try {
            cita = citaService.guardar(cita);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/citas/nueva";
        }

        // GENERAR NOTIFICACIONES
        notificacionService.citaCreada(
            cita.getId(),
            cita.getUsuarioUsername(),
            cita.getMedicoUsername(),
            cita.getEnfermeroUsername()
        );

        redirectAttributes.addFlashAttribute(
            "exito",
            "Cita registrada correctamente."
        );

        return "redirect:/citas";
    }

    // =====================================================
    // FORMULARIO DE EDICIÓN
    // =====================================================

    @GetMapping("/citas/editar/{id}")
    public String editarCita(
            @PathVariable String id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!puedeGestionar(authentication)) {
            return "redirect:/error/403";
        }

        Optional<Cita> citaOptional =
            citaService.buscarPorId(id);

        if (citaOptional.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                "error",
                "La cita no existe."
            );

            return "redirect:/citas";
        }

        Cita cita = citaOptional.get();

        if (!puedeModificarCita(authentication, cita)) {
            return "redirect:/error/403";
        }

        model.addAttribute("cita", cita);
        model.addAttribute("estados", estados());
        // El formulario puede ofrecer un selector de pacientes registrados.
        // La autorización efectiva de cada operación se comprueba en el servidor.
        model.addAttribute("pacientesRegistrados", pacienteRepository.findAll());
        cargarProfesionales(model);
        model.addAttribute("pacienteNombreDisplay", obtenerPacienteNombreDisplay(cita));

        return "cita-form";
    }

    // =====================================================
    // ACTUALIZAR CITA Y GENERAR NOTIFICACIONES
    // =====================================================

    @PostMapping("/citas/actualizar/{id}")
    public String actualizarCita(
            @PathVariable String id,
            @ModelAttribute Cita citaActualizada,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (!puedeGestionar(authentication)) {
            return "redirect:/error/403";
        }

        Optional<Cita> citaOptional =
            citaService.buscarPorId(id);

        if (citaOptional.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                "error",
                "La cita no existe."
            );

            return "redirect:/citas";
        }

        Cita original = citaOptional.get();

        if (!puedeModificarCita(authentication, original)) {
            return "redirect:/error/403";
        }

        Cita cita = copiarCita(original);

        if (citaActualizada.getFecha() == null
                || citaActualizada.getFecha().isBlank()
                || citaActualizada.getHora() == null
                || citaActualizada.getHora().isBlank()) {
            redirectAttributes.addFlashAttribute(
                "error", "La fecha y la hora son obligatorias.");
            return "redirect:/citas/editar/" + id;
        }

        if (!estadoValido(citaActualizada.getEstado())) {

            redirectAttributes.addFlashAttribute(
                "error",
                "Selecciona un estado válido."
            );

            return "redirect:/citas/editar/" + id;
        }

        // =============================================
        // CONSERVAR INFORMACIÓN ANTERIOR
        // =============================================

        String fechaAnterior = cita.getFecha();
        String horaAnterior = cita.getHora();
        String estadoAnterior = cita.getEstado();

        String usuarioAnterior =
            cita.getUsuarioUsername();

        String medicoAnterior =
            cita.getMedicoUsername();

        String enfermeroAnterior =
            cita.getEnfermeroUsername();

        // =============================================
        // ACTUALIZAR LOS DATOS
        // =============================================

        cita.setFecha(citaActualizada.getFecha());
        cita.setHora(citaActualizada.getHora());
        cita.setMotivo(citaActualizada.getMotivo());

        // El administrador puede reasignar pacientes
        // y profesionales.
        // Médicos y enfermeros conservan las
        // asignaciones existentes.

        if (tieneRol(authentication, "ROLE_ADMIN")) {

            // Al editar únicamente la fecha/hora, el formulario puede omitir
            // la referencia: conservamos el paciente original.
            if (citaActualizada.getPacienteId() != null
                    && !citaActualizada.getPacienteId().isBlank()) {
                cita.setPacienteId(citaActualizada.getPacienteId().trim());
                cita.setUsuarioUsername(null);
            } else if (citaActualizada.getUsuarioUsername() != null
                    && !citaActualizada.getUsuarioUsername().isBlank()) {
                cita.setUsuarioUsername(citaActualizada.getUsuarioUsername().trim());
                cita.setPacienteId(null);
            }

            if (citaActualizada.getMedicoUsername() != null
                    && !citaActualizada.getMedicoUsername().isBlank()) {
                cita.setMedicoUsername(citaActualizada.getMedicoUsername().trim());
            }

            if (citaActualizada.getEnfermeroUsername() != null) {
                cita.setEnfermeroUsername(citaActualizada.getEnfermeroUsername().trim());
            }
        }

        cita.setEstado(citaActualizada.getEstado());

        cita.setObservaciones(
            citaActualizada.getObservaciones()
        );

        // =============================================
        // IDENTIFICAR CAMBIOS
        // =============================================

        boolean cambioHorario =
            !Objects.equals(
                fechaAnterior, cita.getFecha()
            )
            ||
            !Objects.equals(
                horaAnterior, cita.getHora()
            );

        boolean cambioEstado =
            !Objects.equals(
                estadoAnterior, cita.getEstado()
            );

        boolean cambioProfesional =
            !Objects.equals(
                medicoAnterior, cita.getMedicoUsername()
            )
            ||
            !Objects.equals(
                enfermeroAnterior, cita.getEnfermeroUsername()
            );

        boolean cambioUsuario =
            !Objects.equals(
                usuarioAnterior, cita.getUsuarioUsername()
            );

        boolean fueCancelada =
            cambioEstado
            &&
            "CANCELADA".equalsIgnoreCase(
                cita.getEstado()
            );

        boolean fueConfirmada =
            cambioEstado
            &&
            "CONFIRMADA".equalsIgnoreCase(
                cita.getEstado()
            );

        boolean fueAtendida =
            cambioEstado
            &&
            "ATENDIDA".equalsIgnoreCase(
                cita.getEstado()
            );

        // =============================================
        // GUARDAR CAMBIOS EN MONGODB
        // =============================================

        try {
            cita = citaService.guardar(cita);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/citas/editar/" + id;
        }

        // =============================================
        // GENERAR NOTIFICACIONES AUTOMÁTICAS
        // =============================================

        // PRIORIDAD 1: CANCELACIÓN
        if (fueCancelada) {

            notificacionService.citaCancelada(
                cita.getId(),
                cita.getUsuarioUsername(),
                cita.getMedicoUsername(),
                cita.getEnfermeroUsername()
            );
        }

        // PRIORIDAD 2: CONFIRMACIÓN
        else if (fueConfirmada) {

            notificacionService.citaConfirmada(
                cita.getId(),
                cita.getUsuarioUsername(),
                cita.getMedicoUsername(),
                cita.getEnfermeroUsername()
            );
        }

        // PRIORIDAD 3: ATENCIÓN FINALIZADA
        else if (fueAtendida) {

            notificacionService.citaAtendida(
                cita.getId(),
                cita.getUsuarioUsername(),
                cita.getMedicoUsername(),
                cita.getEnfermeroUsername()
            );
        }

        // OTROS CAMBIOS
        else if (!"CANCELADA".equalsIgnoreCase(
                cita.getEstado())) {

            // Reprogramación
            if (cambioHorario) {

                notificacionService.citaReprogramada(
                    cita.getId(),
                    cita.getUsuarioUsername(),
                    cita.getMedicoUsername(),
                    cita.getEnfermeroUsername()
                );
            }

            // Cambio de profesional
            if (cambioProfesional) {

                notificacionService.citaReasignada(
                    cita.getId(),
                    cita.getUsuarioUsername(),
                    medicoAnterior,
                    enfermeroAnterior,
                    cita.getMedicoUsername(),
                    cita.getEnfermeroUsername()
                );
            }

            // Cambio de paciente asociado
            if (cambioUsuario
                    && !cambioHorario
                    && !cambioProfesional) {

                notificacionService.citaReprogramada(
                    cita.getId(),
                    cita.getUsuarioUsername(),
                    cita.getMedicoUsername(),
                    cita.getEnfermeroUsername()
                );
            }
        }

        redirectAttributes.addFlashAttribute(
            "exito",
            "Cita actualizada correctamente."
        );

        return "redirect:/citas";
    }

    // =====================================================
    // ELIMINAR CITA — SOLO ADMIN
    // =====================================================

    @PostMapping("/citas/eliminar/{id}")
    public String eliminarCita(
            @PathVariable String id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (!tieneRol(authentication, "ROLE_ADMIN")) {
            return "redirect:/error/403";
        }

        Optional<Cita> citaOptional =
            citaService.buscarPorId(id);

        if (citaOptional.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                "error",
                "La cita no existe."
            );

            return "redirect:/citas";
        }

        Cita cita = citaOptional.get();

        String usuarioUsername =
            cita.getUsuarioUsername();

        String medicoUsername =
            cita.getMedicoUsername();

        String enfermeroUsername =
            cita.getEnfermeroUsername();

        // Eliminar de MongoDB
        citaService.eliminar(id);

        // Notificar a las personas relacionadas
        notificacionService.citaCancelada(
            id,
            usuarioUsername,
            medicoUsername,
            enfermeroUsername
        );

        redirectAttributes.addFlashAttribute(
            "exito",
            "Cita eliminada correctamente."
        );

        return "redirect:/citas";
    }

    // Copia defensiva: no modificar el objeto obtenido de MongoDB
    // hasta que el servicio haya validado el cambio.
    private Cita copiarCita(Cita original) {
        Cita copia = new Cita();
        copia.setId(original.getId());
        copia.setFecha(original.getFecha());
        copia.setHora(original.getHora());
        copia.setMotivo(original.getMotivo());
        copia.setUsuarioUsername(original.getUsuarioUsername());
        copia.setPacienteId(original.getPacienteId());
        copia.setMedicoUsername(original.getMedicoUsername());
        copia.setEnfermeroUsername(original.getEnfermeroUsername());
        copia.setEstado(original.getEstado());
        copia.setObservaciones(original.getObservaciones());
        return copia;
    }

    // =====================================================
    // PROFESIONALES REGISTRADOS PARA EL FORMULARIO
    // =====================================================
    private void cargarProfesionales(Model model) {
        List<Usuario> medicos = usuarioRepository.findByRol(Rol.MEDICO);
        List<Usuario> enfermeros = usuarioRepository.findByRol(Rol.ENFERMERO);
        model.addAttribute("medicosRegistrados", medicos);
        model.addAttribute("enfermerosRegistrados", enfermeros);
    }

    // =====================================================
    // VALIDACIÓN DE DATOS
    // =====================================================

    private boolean datosValidos(
            Cita cita,
            RedirectAttributes redirectAttributes) {

        if ((cita.getUsuarioUsername() == null || cita.getUsuarioUsername().isBlank())
                && (cita.getPacienteId() == null || cita.getPacienteId().isBlank())) {

            redirectAttributes.addFlashAttribute(
                "error",
                "Debes indicar el usuario del paciente."
            );

            return false;
        }

        if (cita.getMedicoUsername() == null
                || cita.getMedicoUsername().isBlank()) {
            redirectAttributes.addFlashAttribute(
                "error", "Debes indicar un médico.");
            return false;
        }

        if (cita.getFecha() == null
                || cita.getFecha().isBlank()) {

            redirectAttributes.addFlashAttribute(
                "error",
                "Debes seleccionar una fecha."
            );

            return false;
        }

        if (cita.getHora() == null
                || cita.getHora().isBlank()) {

            redirectAttributes.addFlashAttribute(
                "error",
                "Debes seleccionar una hora."
            );

            return false;
        }

        return true;
    }

    // =====================================================
    // VALIDAR ESTADO
    // =====================================================

    private boolean estadoValido(String estado) {

        return estado != null
            && estados().contains(estado);
    }

    // Identificar al paciente por ID o documento, sin inferir por nombres similares.
    private String obtenerPacienteNombreDisplay(Cita cita) {
        String referencia = cita.getPacienteId();
        if (referencia == null || referencia.isBlank()) {
            referencia = cita.getUsuarioUsername();
        }
        if (referencia == null || referencia.isBlank()) {
            return "Paciente sin referencia verificable";
        }

        String clave = referencia.trim();
        Paciente encontrado = null;
        for (Paciente paciente : pacienteRepository.findAll()) {
            boolean coincideId = paciente.getId() != null && clave.equals(paciente.getId().trim());
            boolean coincideDocumento = paciente.getDocumento() != null
                    && clave.equals(paciente.getDocumento().trim());
            if (coincideId || coincideDocumento) {
                if (encontrado != null && !Objects.equals(encontrado.getId(), paciente.getId())) {
                    return "Referencia de paciente ambigua (verificar en Pacientes)";
                }
                encontrado = paciente;
            }
        }
        if (encontrado == null) {
            return "Paciente no identificado (verificar referencia)";
        }
        String nombres = encontrado.getNombres() == null ? "" : encontrado.getNombres().trim();
        String apellidos = encontrado.getApellidos() == null ? "" : encontrado.getApellidos().trim();
        String nombre = (nombres + " " + apellidos).trim();
        if (nombre.isBlank()) nombre = "Paciente registrado";
        String documento = encontrado.getDocumento();
        if (documento != null && !documento.isBlank()) {
            nombre += " — " + documento.trim();
        }
        return nombre;
    }

    // =====================================================
    // AUTENTICACIÓN
    // =====================================================

    private boolean estaAutenticado(
            Authentication authentication) {

        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    }

    // =====================================================
    // PERMISOS PARA CREAR O GESTIONAR
    // =====================================================

    private boolean puedeGestionar(
            Authentication authentication) {

        return estaAutenticado(authentication)
            &&
            (
                tieneRol(authentication, "ROLE_ADMIN")
                ||
                tieneRol(authentication, "ROLE_MEDICO")
                ||
                tieneRol(authentication, "ROLE_ENFERMERO")
            );
    }

    // =====================================================
    // COMPROBAR ASIGNACIÓN DE LA CITA
    // =====================================================

    private boolean puedeModificarCita(
            Authentication authentication,
            Cita cita) {

        if (!puedeGestionar(authentication)) {
            return false;
        }

        if (tieneRol(authentication, "ROLE_ADMIN")) {
            return true;
        }

        String username = authentication.getName();

        if (tieneRol(authentication, "ROLE_MEDICO")) {

            return username.equals(
                cita.getMedicoUsername()
            );
        }

        if (tieneRol(authentication, "ROLE_ENFERMERO")) {

            return username.equals(
                cita.getEnfermeroUsername()
            );
        }

        return false;
    }

    // =====================================================
    // COMPROBAR ROL
    // =====================================================

    private boolean tieneRol(
            Authentication authentication,
            String rol) {

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
            .stream()
            .anyMatch(
                authority ->
                    authority.getAuthority().equals(rol)
            );
    }

    // =====================================================
    // ESTADOS DISPONIBLES
    // =====================================================

    private List<String> estados() {

        return Arrays.asList(
            "PROGRAMADA",
            "CONFIRMADA",
            "ATENDIDA",
            "CANCELADA"
        );
    }
}
