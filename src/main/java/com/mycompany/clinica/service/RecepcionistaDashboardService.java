package com.mycompany.clinica.service;

import com.mycompany.clinica.entity.Cita;
import com.mycompany.clinica.repository.CitaRepository;
import com.mycompany.clinica.repository.PacienteRepository;

import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RecepcionistaDashboardService {

    // =====================================================
    // CONFIGURACIÃ“N
    // =====================================================

    private static final ZoneId ZONA_COLOMBIA =
            ZoneId.of("America/Bogota");

    private static final int LIMITE_PROXIMAS_CITAS = 5;

    private static final Set<String> ESTADOS_PROXIMOS =
            Set.of("PROGRAMADA", "PENDIENTE", "CONFIRMADA");

    // =====================================================
    // REPOSITORIOS
    // =====================================================

    private final PacienteRepository pacienteRepository;
    private final CitaRepository citaRepository;

    public RecepcionistaDashboardService(
            PacienteRepository pacienteRepository,
            CitaRepository citaRepository) {

        this.pacienteRepository = pacienteRepository;
        this.citaRepository = citaRepository;
    }

    // =====================================================
    // 1. CARGAR INDICADORES DEL DASHBOARD
    // =====================================================

    public void cargarIndicadores(Model model) {

        LocalDateTime ahora = LocalDateTime.now(ZONA_COLOMBIA);
        String fechaHoy = ahora.toLocalDate().toString();

        // Total de pacientes registrados, activos e inactivos.
        long totalPacientes = pacienteRepository.count();

        // Citas programadas para la fecha actual, sin excluir estados.
        long citasHoy = citaRepository.countByFecha(fechaHoy);

        // Consultar solo citas desde hoy para indicadores y prÃ³ximas citas.
        List<Cita> citasDesdeHoy = citaRepository
                .findByFechaGreaterThanEqualOrderByFechaAscHoraAsc(fechaHoy);

        long citasPendientes = 0;
        long citasConfirmadas = 0;

        for (Cita cita : citasDesdeHoy) {
            if (convertirFecha(cita.getFecha()) == null) {
                continue;
            }

            String estado = normalizarEstado(cita.getEstado());

            // Conserva el criterio original: fecha actual o futura.
            if ("PROGRAMADA".equals(estado) || "PENDIENTE".equals(estado)) {
                citasPendientes++;
            } else if ("CONFIRMADA".equals(estado)) {
                citasConfirmadas++;
            }
        }

        List<Map<String, String>> proximasCitas = citasDesdeHoy.stream()
                .filter(cita -> ESTADOS_PROXIMOS.contains(
                        normalizarEstado(cita.getEstado())))
                .filter(cita -> {
                    LocalDateTime fechaHora = obtenerFechaHora(cita);
                    return fechaHora != null && !fechaHora.isBefore(ahora);
                })
                .sorted(Comparator.comparing(this::obtenerFechaHora))
                .limit(LIMITE_PROXIMAS_CITAS)
                .map(this::convertirParaVista)
                .toList();

        model.addAttribute("totalPacientes", totalPacientes);
        model.addAttribute("citasHoy", citasHoy);
        model.addAttribute("citasPendientes", citasPendientes);
        model.addAttribute("citasConfirmadas", citasConfirmadas);
        model.addAttribute("proximasCitas", proximasCitas);
    }

    // =====================================================
    // 2. DATOS ADMINISTRATIVOS PARA THYMELEAF
    // =====================================================

    private Map<String, String> convertirParaVista(Cita cita) {
        Map<String, String> datos = new HashMap<>();

        String usuario = texto(cita.getUsuarioUsername());
        datos.put("pacienteNombre", usuario.isBlank() ? "Sin asignar" : usuario);
        datos.put("usuarioUsername", usuario);
        datos.put("medicoUsername", texto(cita.getMedicoUsername()));
        datos.put("fecha", texto(cita.getFecha()));
        datos.put("hora", texto(cita.getHora()));
        datos.put("estado", normalizarEstado(cita.getEstado()));

        // No exponer motivos, observaciones ni datos clÃ­nicos.
        return datos;
    }

    // =====================================================
    // 3. FECHAS Y HORAS SEGURAS
    // =====================================================

    private LocalDateTime obtenerFechaHora(Cita cita) {
        if (cita == null) {
            return null;
        }

        LocalDate fecha = convertirFecha(cita.getFecha());
        LocalTime hora = convertirHora(cita.getHora());

        if (fecha == null || hora == null) {
            return null;
        }

        return LocalDateTime.of(fecha, hora);
    }

    private LocalDate convertirFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalTime convertirHora(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            return LocalTime.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String normalizarEstado(String estado) {
        return estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }
}

