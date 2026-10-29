
package com.mycompany.clinica.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

import com.mycompany.clinica.entity.Paciente;
import java.util.Objects;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;

import com.mycompany.clinica.entity.Cita;
import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.repository.UsuarioRepository;
import com.mycompany.clinica.repository.PacienteRepository;
import com.mycompany.clinica.repository.CitaRepository;
import com.mycompany.clinica.repository.HistorialRepository;
import com.mycompany.clinica.repository.MedicamentoRepository;
import com.mycompany.clinica.service.CitaService;
import com.mycompany.clinica.service.HistorialService;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    // =====================================================
    // SERVICIOS
    // =====================================================

    private final CitaService citaService;
    private final HistorialService historialService;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final CitaRepository citaRepository;
    private final HistorialRepository historialRepository;
    private final MedicamentoRepository medicamentoRepository;

    public DashboardController(
            CitaService citaService,
            HistorialService historialService,
            UsuarioRepository usuarioRepository,
            PacienteRepository pacienteRepository,
            CitaRepository citaRepository,
            HistorialRepository historialRepository,
            MedicamentoRepository medicamentoRepository) {

        this.citaService = citaService;
        this.historialService = historialService;
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.citaRepository = citaRepository;
        this.historialRepository = historialRepository;
        this.medicamentoRepository = medicamentoRepository;
    }

    // =====================================================
    // DASHBOARD SEGÚN EL ROL
    // =====================================================

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Principal principal,
            Model model) {

        // =============================================
        // VALIDAR AUTENTICACIÓN
        // =============================================

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            return "redirect:/login";
        }

        // =============================================
        // USUARIO ACTUAL
        // =============================================

        String username = principal != null
                ? principal.getName()
                : authentication.getName();

        model.addAttribute("username", username);

        // =============================================
        // IDENTIFICAR ROLES
        // =============================================

        boolean esAdmin = tieneRol(authentication, "ADMIN");

        boolean esMedico = tieneRol(authentication, "MEDICO");

        boolean esEnfermero = tieneRol(authentication, "ENFERMERO");

        boolean esRecepcionista =
                tieneRol(authentication, "RECEPCIONISTA");

        boolean esUsuario = tieneRol(authentication, "USUARIO");

        // =============================================
        // ENVIAR ROLES AL MODELO
        // =============================================

        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("esMedico", esMedico);
        model.addAttribute("esEnfermero", esEnfermero);
        model.addAttribute("esRecepcionista", esRecepcionista);
        model.addAttribute("esUsuario", esUsuario);

        // =============================================
        // DASHBOARD ADMINISTRADOR
        // =============================================

        if (esAdmin) {
            cargarEstadisticasAdmin(model);
            return "admin-dashboard";
        }

        // =============================================
        // DASHBOARD MÉDICO
        // =============================================

        if (esMedico) {
            cargarEstadisticasMedico(model, username);
            return "medico-dashboard";
        }

        // =============================================
        // DASHBOARD ENFERMERO
        // =============================================

        if (esEnfermero) {
            cargarEstadisticasEnfermero(model, username);
            return "enfermero-dashboard";
        }

        // =============================================
        // DASHBOARD RECEPCIONISTA
        // =============================================

        if (esRecepcionista) {
            return "redirect:/recepcionista/dashboard";
        }

        // =============================================
        // DASHBOARD USUARIO
        // =============================================

        if (esUsuario) {

            // Consultar únicamente los registros
            // del usuario autenticado.

            var citas = citaService.buscarPorUsuario(
                    authentication.getName()
            );

            var historiales = historialService.buscarPorUsuario(
                    authentication.getName()
            );

            // Fecha y hora de Colombia.

            ZoneId zona = ZoneId.of("America/Bogota");

            LocalDate hoy = LocalDate.now(zona);
            LocalTime horaActual = LocalTime.now(zona);

            // Ordenar próximas citas por fecha y hora.

            var proximasCitas = citas
                    .stream()
                    .filter(cita ->
                            esProximaCita(cita, hoy, horaActual)
                    )
                    .sorted(
                            Comparator
                                    .comparing(Cita::getFecha)
                                    .thenComparing(
                                            Cita::getHora,
                                            Comparator.nullsLast(
                                                    Comparator.naturalOrder()
                                            )
                                    )
                    )
                    .toList();

            // Datos disponibles para el dashboard.

            model.addAttribute(
                    "totalCitas",
                    citas.size()
            );

            model.addAttribute(
                    "totalHistoriales",
                    historiales.size()
            );

            model.addAttribute(
                    "totalProximas",
                    proximasCitas.size()
            );

            model.addAttribute(
                    "proximasCitas",
                    proximasCitas
                            .stream()
                            .limit(3)
                            .toList()
            );

            return "usuario-dashboard";
        }

        // =============================================
        // ROL NO RECONOCIDO
        // =============================================

        return "redirect:/error/403";
    }

    // =====================================================
    // PANEL MÉDICO: DATOS DEL MÉDICO AUTENTICADO
    // =====================================================
    private void cargarEstadisticasMedico(Model model, String medicoUsername) {
        ZoneId zona = ZoneId.of("America/Bogota");
        LocalDate hoy = LocalDate.now(zona);
        LocalTime horaActual = LocalTime.now(zona);

        // Filtrado por médico desde los métodos ya existentes.
        var citasMedico = citaRepository.findByMedicoUsername(medicoUsername);
        var historialesMedico = historialRepository.findByMedicoUsername(medicoUsername);

        long citasHoy = citasMedico.stream()
                .filter(c -> hoy.toString().equals(c.getFecha()))
                .count();
        long citasPendientes = citasMedico.stream()
                .filter(c -> "PENDIENTE".equalsIgnoreCase(c.getEstado())
                        || "PROGRAMADA".equalsIgnoreCase(c.getEstado()))
                .count();
        long citasConfirmadas = citasMedico.stream()
                .filter(c -> "CONFIRMADA".equalsIgnoreCase(c.getEstado()))
                .count();

        var proximasCitas = citasMedico.stream()
                .filter(c -> esProximaCita(c, hoy, horaActual))
                .sorted(Comparator.comparing(Cita::getFecha)
                        .thenComparing(Cita::getHora,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .toList();

        // Pacientes únicos con historia registrada por este médico.
        long pacientesConHistorial = historialesMedico.stream()
                .map(h -> h.getPacienteId())
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .distinct()
                .count();

        model.addAttribute("totalMedicamentos", medicamentoRepository.countByStockGreaterThan(0));
        model.addAttribute("totalCitas", citasMedico.size());
        model.addAttribute("citasHoy", citasHoy);
        model.addAttribute("citasPendientes", citasPendientes);
        model.addAttribute("citasConfirmadas", citasConfirmadas);
        model.addAttribute("totalHistoriales", historialesMedico.size());
        model.addAttribute("pacientesConHistorial", pacientesConHistorial);
        model.addAttribute("proximasCitas", proximasCitas);
        model.addAttribute("totalProximas", proximasCitas.size());
    }

    // =====================================================
    // PANEL ENFERMERO: SOLO CITAS ASIGNADAS
    // =====================================================
    private void cargarEstadisticasEnfermero(Model model, String enfermeroUsername) {
        ZoneId zona = ZoneId.of("America/Bogota");
        LocalDate hoy = LocalDate.now(zona);
        LocalTime ahora = LocalTime.now(zona);

        List<Cita> citas = citaService.buscarPorEnfermero(enfermeroUsername);
        long citasHoy = citas.stream()
                .filter(c -> hoy.toString().equals(c.getFecha()))
                .count();
        long citasPendientes = citas.stream()
                .filter(c -> "PROGRAMADA".equalsIgnoreCase(c.getEstado())
                        || "PENDIENTE".equalsIgnoreCase(c.getEstado()))
                .count();
        long citasAtendidas = citas.stream()
                .filter(c -> "ATENDIDA".equalsIgnoreCase(c.getEstado()))
                .count();

        List<Cita> proximas = citas.stream()
                .filter(c -> esProximaCita(c, hoy, ahora))
                .sorted(Comparator.comparing(Cita::getFecha)
                        .thenComparing(Cita::getHora,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .toList();

        // Resolver pacientes por ID o documento exacto, nunca por nombre parecido.
        Map<String, Paciente> porReferencia = new HashMap<>();
        Set<String> ambiguas = new HashSet<>();
        for (Paciente paciente : pacienteRepository.findAll()) {
            String id = paciente.getId();
            if (id != null && !id.isBlank()) {
                registrarReferencia(porReferencia, ambiguas, id.trim(), paciente);
            }
            String documento = paciente.getDocumento();
            if (documento != null && !documento.isBlank()) {
                registrarReferencia(porReferencia, ambiguas, documento.trim(), paciente);
            }
        }

        Map<String, String> nombres = new HashMap<>();
        for (Cita cita : proximas) {
            if (cita.getId() == null) continue;
            String referencia = cita.getPacienteId();
            if (referencia == null || referencia.isBlank()) {
                referencia = cita.getUsuarioUsername();
            }
            if (referencia == null || referencia.isBlank()) continue;
            referencia = referencia.trim();
            if (ambiguas.contains(referencia)) continue;
            Paciente paciente = porReferencia.get(referencia);
            if (paciente == null) continue;
            String nombre = ((paciente.getNombres() == null ? "" : paciente.getNombres())
                    + " " + (paciente.getApellidos() == null ? "" : paciente.getApellidos())).trim();
            if (!nombre.isBlank()) nombres.put(cita.getId(), nombre);
        }

        model.addAttribute("citasHoy", citasHoy);
        model.addAttribute("citasPendientes", citasPendientes);
        model.addAttribute("citasAtendidas", citasAtendidas);
        model.addAttribute("totalCitas", citas.size());
        model.addAttribute("proximasCitas", proximas);
        model.addAttribute("nombresPacientesPorCita", nombres);
    }

    private void registrarReferencia(Map<String, Paciente> mapa,
                                     Set<String> ambiguas,
                                     String clave, Paciente paciente) {
        Paciente anterior = mapa.putIfAbsent(clave, paciente);
        if (anterior != null && !Objects.equals(anterior.getId(), paciente.getId())) {
            ambiguas.add(clave);
        }
    }

    // =====================================================
    // VERIFICAR ROL
    // =====================================================

    private boolean tieneRol(
            Authentication authentication,
            String rol) {

        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals(
                                "ROLE_" + rol
                        )
                );
    }

    // =====================================================
    // COMPROBAR SI UNA CITA ESTÁ PRÓXIMA
    // =====================================================

    private boolean esProximaCita(
            Cita cita,
            LocalDate hoy,
            LocalTime horaActual) {

        boolean programada =
                "PROGRAMADA".equalsIgnoreCase(
                        cita.getEstado()
                );

        boolean confirmada =
                "CONFIRMADA".equalsIgnoreCase(
                        cita.getEstado()
                );

        if (!programada && !confirmada) {
            return false;
        }

        if (cita.getFecha() == null
                || cita.getFecha().isBlank()) {

            return false;
        }

        try {
            LocalDate fecha = LocalDate.parse(
                    cita.getFecha()
            );

            // Cita en un día posterior.

            if (fecha.isAfter(hoy)) {
                return true;
            }

            // Excluir días anteriores.

            if (!fecha.equals(hoy)) {
                return false;
            }

            // Mostrar citas de hoy sin hora registrada.

            if (cita.getHora() == null
                    || cita.getHora().isBlank()) {

                return true;
            }

            // Mostrar citas de hoy cuya hora no ha pasado.

            LocalTime hora = LocalTime.parse(
                    cita.getHora()
            );

            return !hora.isBefore(horaActual);

        } catch (DateTimeParseException ex) {

            return false;
        }
    }

    // =====================================================
    // PANEL ADMINISTRADOR
    // =====================================================

    @GetMapping("/admin")
    public String admin(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !tieneRol(authentication, "ADMIN")) {
            return "redirect:/error/403";
        }
        cargarEstadisticasAdmin(model);

        return "admin";
    }

    private void cargarEstadisticasAdmin(Model model) {
        long totalUsuarios = usuarioRepository.count();
        long totalPacientes = pacienteRepository.count();
        long totalCitas = citaRepository.count();
        long totalHistoriales = historialRepository.count();
        long totalAdmins = usuarioRepository.countByRol(Rol.ADMIN);
        long totalMedicos = usuarioRepository.countByRol(Rol.MEDICO);
        long totalEnfermeros = usuarioRepository.countByRol(Rol.ENFERMERO);
        long totalRecepcionistas = usuarioRepository.countByRol(Rol.RECEPCIONISTA);
        long totalUsuariosRol = usuarioRepository.countByRol(Rol.USUARIO);

        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("totalPacientes", totalPacientes);
        model.addAttribute("totalCitas", totalCitas);
        model.addAttribute("totalHistoriales", totalHistoriales);
        model.addAttribute("totalAdmins", totalAdmins);
        model.addAttribute("totalMedicos", totalMedicos);
        model.addAttribute("totalEnfermeros", totalEnfermeros);
        model.addAttribute("totalRecepcionistas", totalRecepcionistas);
        model.addAttribute("totalUsuariosRol", totalUsuariosRol);

        log.info("CLINGUARD ADMIN DASHBOARD: usuarios={}, medicos={}, enfermeros={}, pacientes={}, citas={}, historiales={}",
                totalUsuarios, totalMedicos, totalEnfermeros, totalPacientes, totalCitas, totalHistoriales);
    }

    // =====================================================
    // RUTAS ANTIGUAS
    // =====================================================

    @GetMapping({
            "/medico",
            "/enfermero",
            "/usuario",
            "/recepcionista"
    })
    public String dashboardPorRol() {
        return "redirect:/dashboard";
    }
}
