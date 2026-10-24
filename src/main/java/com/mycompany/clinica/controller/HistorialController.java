package com.mycompany.clinica.controller;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Instant;
import java.util.Comparator;
import com.mycompany.clinica.entity.Paciente;
import com.mycompany.clinica.repository.PacienteRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mycompany.clinica.entity.Historial;
import com.mycompany.clinica.service.HistorialService;


@Controller
@RequestMapping("/historial")
public class HistorialController {


    private final HistorialService historialService;
    private final PacienteRepository pacienteRepository;


    public HistorialController(
            HistorialService historialService,
            PacienteRepository pacienteRepository) {

        this.historialService = historialService;
        this.pacienteRepository = pacienteRepository;
    }


    // =====================================================
    // LISTA DE HISTORIALES
    // GET /historial
    // =====================================================

    @GetMapping({"", "/"})
    public String mostrarHistorial(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication,
            Model model) {

        if (!estaAutenticado(authentication)) {
            return "redirect:/login";
        }

        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 10;
        }
        size = Math.min(size, 100);

        String username = authentication.getName();

        boolean esAdmin = tieneRol(authentication, "ROLE_ADMIN");
        boolean esMedico = tieneRol(authentication, "ROLE_MEDICO");
        boolean esEnfermero = tieneRol(authentication, "ROLE_ENFERMERO");
        boolean esUsuario = tieneRol(authentication, "ROLE_USUARIO");

        if (!esAdmin && !esMedico && !esEnfermero && !esUsuario) {
            return "redirect:/error/403";
        }
        model.addAttribute("username", username);
        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("esMedico", esMedico);
        model.addAttribute("esEnfermero", esEnfermero);
        model.addAttribute("esUsuario", esUsuario);

        Pageable pageable = PageRequest.of(page, size);

        Page<Historial> paginaHistorial;

        if (esAdmin || esEnfermero) {
            paginaHistorial = historialService.listarTodos(pageable);
        } else if (esMedico) {
            // Solo historiales creados/asignados a este medico.
            // La consulta paginada deberia trasladarse al repositorio si crece el volumen.
            List<Historial> propios = historialService.listarTodos().stream()
                    .filter(h -> username.equals(h.getMedicoUsername()))
                    .sorted(Comparator.comparing(Historial::getFecha,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            int desde = (int) Math.min((long) page * size, propios.size());
            int hasta = Math.min(desde + size, propios.size());
            paginaHistorial = new PageImpl<>(propios.subList(desde, hasta), pageable, propios.size());
        } else {
            paginaHistorial = historialService.buscarPorUsuario(username, pageable);
        }

        model.addAttribute(
                "historiales",
                paginaHistorial.getContent()
        );

        model.addAttribute(
                "totalHistoriales",
                paginaHistorial.getTotalElements()
        );

        model.addAttribute(
                "paginaActual",
                paginaHistorial.getNumber()
        );

        model.addAttribute(
                "totalPaginas",
                paginaHistorial.getTotalPages()
        );

        model.addAttribute(
                "size",
                paginaHistorial.getSize()
        );

        cargarNombresPacientes(model);
        return "historial";
    }


    // Historial de un paciente seleccionado desde la lista de Pacientes.
    // Solo se consideran coincidencias exactas de ID o documento.
    @GetMapping("/paciente/{id}")
    public String historialPorPaciente(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!estaAutenticado(authentication)) return "redirect:/login";
        boolean esAdmin = tieneRol(authentication, "ROLE_ADMIN");
        boolean esMedico = tieneRol(authentication, "ROLE_MEDICO");
        boolean esEnfermero = tieneRol(authentication, "ROLE_ENFERMERO");
        if (!esAdmin && !esMedico && !esEnfermero) return "redirect:/error/403";

        Optional<Paciente> resultado = pacienteRepository.findById(id);
        if (resultado.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El paciente no existe.");
            return "redirect:/pacientes";
        }

        Paciente paciente = resultado.get();
        String documento = paciente.getDocumento();
        Set<String> referencias = new java.util.HashSet<>();
        referencias.add(paciente.getId());
        if (documento != null && !documento.isBlank()) referencias.add(documento.trim());

        List<Historial> encontrados = historialService.listarTodos().stream()
                .filter(h -> (h.getPacienteId() != null && h.getPacienteId().equals(paciente.getId()))
                        || (h.getUsuarioUsername() != null && referencias.contains(h.getUsuarioUsername().trim())))
                .filter(h -> puedeVerHistorial(authentication, h))
                .toList();
        int size = 10;
        int totalPaginas = (encontrados.size() + size - 1) / size;
        int pagina = Math.min(Math.max(page, 0), Math.max(0, totalPaginas - 1));
        int desde = Math.min(pagina * size, encontrados.size());
        int hasta = Math.min(desde + size, encontrados.size());

        model.addAttribute("historiales", encontrados.subList(desde, hasta));
        model.addAttribute("totalHistoriales", encontrados.size());
        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("size", size);
        model.addAttribute("username", authentication.getName());
        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("esMedico", esMedico);
        model.addAttribute("esEnfermero", esEnfermero);
        model.addAttribute("esUsuario", false);
        model.addAttribute("pacienteSeleccionado", paciente);
        cargarNombresPacientes(model);
        return "historial";
    }


    // =====================================================
    // NUEVO HISTORIAL
    // =====================================================

    @GetMapping("/nuevo")
    public String nuevoHistorial(
            @RequestParam(required = false) String pacienteId,
            Authentication authentication,
            Model model) {

        if (!estaAutenticado(authentication)) {
            return "redirect:/login";
        }

        if (!puedeCrearEditar(authentication)) {
            return "redirect:/error/403";
        }

        Historial historial =
                new Historial();

        historial.setFecha(
                LocalDate.now().toString()
        );

        if (tieneRol(
                authentication,
                "ROLE_MEDICO")) {

            historial.setMedicoUsername(
                    authentication.getName()
            );
        }

        if (pacienteId != null && !pacienteId.isBlank()) {
            Paciente seleccionado = pacienteRepository.findById(pacienteId)
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND, "Paciente no encontrado"));
            historial.setPacienteId(seleccionado.getId());
            model.addAttribute("pacienteSeleccionado", seleccionado);
        }
        model.addAttribute("pacientes", pacienteRepository.findAll());
        model.addAttribute("historial", historial);

        model.addAttribute(
                "modoEdicion",
                false
        );

        return "historial-form";
    }


    // =====================================================
    // GUARDAR
    // =====================================================

    @PostMapping("/guardar")
    public String guardarHistorial(
            @ModelAttribute("historial")
            Historial historial,

            Authentication authentication,

            RedirectAttributes redirectAttributes) {


        if (!estaAutenticado(authentication)) {
            return "redirect:/login";
        }


        if (!puedeCrearEditar(authentication)) {
            return "redirect:/error/403";
        }


        if (historial.getPacienteId() == null
                ||
                historial.getPacienteId().isBlank()
                || !pacienteRepository.existsById(historial.getPacienteId())) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Debes seleccionar un paciente registrado."
            );

            return "redirect:/historial/nuevo";
        }


        if (historial.getMotivoConsulta() == null
                ||
                historial.getMotivoConsulta().isBlank()) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Debes indicar el motivo de consulta."
            );

            return "redirect:/historial/nuevo";
        }


        if (historial.getFecha() == null
                ||
                historial.getFecha().isBlank()) {

            historial.setFecha(
                    LocalDate.now().toString()
            );
        }


        if (tieneRol(
                authentication,
                "ROLE_MEDICO")) {

            historial.setMedicoUsername(
                    authentication.getName()
            );
        }


        if (historial.getMedicoUsername() == null
                ||
                historial.getMedicoUsername().isBlank()) {

            historial.setMedicoUsername(
                    authentication.getName()
            );
        }


        historial.setId(null);
        // No inventar el username de un usuario: pacienteId es un ID de MongoDB.
        // La vinculacion real cuenta-paciente requiere revisar Paciente y Usuario.
        historial.setUsuarioUsername(null);
        historial.setCreadoPor(authentication.getName());
        historial.setFechaCreacion(Instant.now());
        historial.setModificadoPor(null);
        historial.setFechaModificacion(null);
        historial.setAnulado(false);
        historial.setMotivoAnulacion(null);

        historialService.guardar(
                historial
        );


        redirectAttributes.addFlashAttribute(
                "exito",
                "Historial mÃ©dico guardado correctamente."
        );


        return "redirect:/historial";
    }


    // =====================================================
    // VER
    // =====================================================

    @GetMapping("/ver/{id}")
    public String verHistorial(
            @PathVariable String id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {


        if (!estaAutenticado(authentication)) {
            return "redirect:/login";
        }


        Optional<Historial> historialOptional =
                historialService.buscarPorId(id);


        if (historialOptional.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "El historial solicitado no existe."
            );

            return "redirect:/historial";
        }


        Historial historial =
                historialOptional.get();

        if (!tieneRol(authentication, "ROLE_ADMIN")
                && !tieneRol(authentication, "ROLE_MEDICO")
                && !tieneRol(authentication, "ROLE_ENFERMERO")
                && !tieneRol(authentication, "ROLE_USUARIO")) {
            return "redirect:/error/403";
        }

        if (tieneRol(
                authentication,
                "ROLE_USUARIO")) {

            if (historial.getUsuarioUsername() == null
                    ||
                    !historial
                            .getUsuarioUsername()
                            .equals(authentication.getName())) {

                return "redirect:/error/403";
            }
        }


        if (!puedeVerHistorial(authentication, historial)) {
            return "redirect:/error/403";
        }

        model.addAttribute(
                "historial",
                historial
        );


        return "historial-detalle";
    }


    // =====================================================
    // EDITAR
    // =====================================================

    @GetMapping("/editar/{id}")
    public String editarHistorial(
            @PathVariable String id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {


        if (!estaAutenticado(authentication)) {
            return "redirect:/login";
        }


        if (!puedeCrearEditar(authentication)) {
            return "redirect:/error/403";
        }


        Optional<Historial> historialOptional =
                historialService.buscarPorId(id);


        if (historialOptional.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se encontrÃ³ el historial."
            );

            return "redirect:/historial";
        }


        if (!puedeModificarHistorial(authentication, historialOptional.get())) {
            return "redirect:/error/403";
        }

        model.addAttribute(
                "historial",
                historialOptional.get()
        );


        model.addAttribute("pacientes", pacienteRepository.findAll());
        model.addAttribute("modoEdicion", true);


        return "historial-form";
    }


    // =====================================================
    // ACTUALIZAR
    // =====================================================

    @PostMapping("/actualizar/{id}")
    public String actualizarHistorial(
            @PathVariable String id,

            @ModelAttribute("historial")
            Historial actualizado,

            Authentication authentication,

            RedirectAttributes redirectAttributes) {


        if (!estaAutenticado(authentication)) {
            return "redirect:/login";
        }


        if (!puedeCrearEditar(authentication)) {
            return "redirect:/error/403";
        }


        Optional<Historial> historialOptional =
                historialService.buscarPorId(id);


        if (historialOptional.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se encontrÃ³ el historial."
            );

            return "redirect:/historial";
        }


        Historial historial =
                historialOptional.get();

        if (!puedeModificarHistorial(authentication, historial)) {
            return "redirect:/error/403";
        }

        if (tieneRol(authentication, "ROLE_MEDICO")
                && !java.util.Objects.equals(historial.getPacienteId(), actualizado.getPacienteId())) {
            redirectAttributes.addFlashAttribute("error", "No puedes cambiar el paciente de un historial existente.");
            return "redirect:/historial/editar/" + id;
        }

        if (actualizado.getPacienteId() == null ||
                !pacienteRepository.existsById(actualizado.getPacienteId())) {
            redirectAttributes.addFlashAttribute("error", "Debes seleccionar un paciente registrado.");
            return "redirect:/historial/editar/" + id;
        }
        historial.setPacienteId(actualizado.getPacienteId());
        // Conservar la asociacion de cuenta existente; no sustituir por el ID del paciente.

        historial.setFecha(
                actualizado.getFecha()
        );

        historial.setMotivoConsulta(
                actualizado.getMotivoConsulta()
        );

        historial.setDiagnostico(
                actualizado.getDiagnostico()
        );

        historial.setTratamiento(
                actualizado.getTratamiento()
        );

        historial.setObservaciones(
                actualizado.getObservaciones()
        );


        if (tieneRol(
                authentication,
                "ROLE_ADMIN")) {

            if (actualizado.getMedicoUsername() != null
                    &&
                    !actualizado
                            .getMedicoUsername()
                            .isBlank()) {

                historial.setMedicoUsername(
                        actualizado.getMedicoUsername()
                );
            }
        }


        // La identidad del medico asignado no se sobrescribe durante la edicion.

        historial.setModificadoPor(authentication.getName());
        historial.setFechaModificacion(Instant.now());
        historialService.actualizar(historial);


        redirectAttributes.addFlashAttribute(
                "exito",
                "Historial mÃ©dico actualizado correctamente."
        );


        return "redirect:/historial";
    }


    // =====================================================
    // ELIMINAR
    // =====================================================

    @PostMapping("/eliminar/{id}")
    public String eliminarHistorial(
            @PathVariable String id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {


        if (!estaAutenticado(authentication)) {
            return "redirect:/login";
        }


        if (!tieneRol(
                authentication,
                "ROLE_ADMIN")) {

            return "redirect:/error/403";
        }


        if (!historialService.existePorId(id)) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "El historial no existe."
            );

            return "redirect:/historial";
        }


        Historial historial = historialService.buscarPorId(id).orElseThrow();
        historial.setAnulado(true);
        historial.setMotivoAnulacion("Anulado por administraciÃ³n. Pendiente de registrar justificaciÃ³n detallada.");
        historial.setModificadoPor(authentication.getName());
        historial.setFechaModificacion(Instant.now());
        historialService.actualizar(historial);


        redirectAttributes.addFlashAttribute(
                "exito",
                "Historial mÃ©dico marcado como anulado."
        );


        return "redirect:/historial";
    }


    private void cargarNombresPacientes(Model model) {
        Map<String, String> nombres = pacienteRepository.findAll().stream()
            .collect(Collectors.toMap(Paciente::getId,
                p -> String.valueOf(p.getNombres()) + " " + String.valueOf(p.getApellidos()),
                (a, b) -> a));
        model.addAttribute("nombresPacientes", nombres);
    }

    // Acceso por registro. Admin: todos. Enfermeria: lectura segun politica actual.
    // Medico: solo historiales cuyo medicoUsername coincide con la sesion.
    // Usuario: solo historiales explicitamente asociados a su username.
    private boolean puedeVerHistorial(Authentication authentication, Historial historial) {
        if (!estaAutenticado(authentication) || historial == null) return false;
        if (tieneRol(authentication, "ROLE_ADMIN")) return true;
        if (tieneRol(authentication, "ROLE_ENFERMERO")) return true;
        if (tieneRol(authentication, "ROLE_MEDICO")) {
            return authentication.getName().equals(historial.getMedicoUsername());
        }
        return tieneRol(authentication, "ROLE_USUARIO")
                && authentication.getName().equals(historial.getUsuarioUsername());
    }

    private boolean puedeModificarHistorial(Authentication authentication, Historial historial) {
        if (!puedeCrearEditar(authentication) || historial == null) {
            return false;
        }
        return tieneRol(authentication, "ROLE_ADMIN")
                || (tieneRol(authentication, "ROLE_MEDICO")
                    && authentication.getName().equals(historial.getMedicoUsername()));
    }

    // =====================================================
    // VALIDAR AUTENTICACIÃ“N
    // =====================================================

    private boolean estaAutenticado(
            Authentication authentication) {

        return authentication != null
                &&
                authentication.isAuthenticated();
    }


    // =====================================================
    // ADMIN / MÃ‰DICO
    // =====================================================

    private boolean puedeCrearEditar(
            Authentication authentication) {

        return tieneRol(
                authentication,
                "ROLE_ADMIN"
        )
                ||
                tieneRol(
                        authentication,
                        "ROLE_MEDICO"
                );
    }


    // =====================================================
    // VALIDAR ROL
    // =====================================================

    private boolean tieneRol(
            Authentication authentication,
            String rol) {


        if (authentication == null) {
            return false;
        }


        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(
                        authority ->
                                authority
                                        .getAuthority()
                                        .equals(rol)
                );
    }

}
