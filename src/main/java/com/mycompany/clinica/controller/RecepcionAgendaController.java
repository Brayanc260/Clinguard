
package com.mycompany.clinica.controller;

import com.mycompany.clinica.entity.Cita;
import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;

import com.mycompany.clinica.repository.CitaRepository;
import com.mycompany.clinica.repository.UsuarioRepository;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Controller
@RequestMapping("/recepcion/agenda")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
public class RecepcionAgendaController {

    // ==========================================
    // CONFIGURACIÃ“N
    // ==========================================

    private static final ZoneId ZONA_COLOMBIA =
            ZoneId.of("America/Bogota");

    // ==========================================
    // REPOSITORIOS
    // ==========================================

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;

    public RecepcionAgendaController(
            CitaRepository citaRepository,
            UsuarioRepository usuarioRepository) {

        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ==========================================
    // 1. MOSTRAR AGENDA MÃ‰DICA
    // GET /recepcion/agenda
    // ==========================================

    @GetMapping
    public String mostrarAgenda(
            @RequestParam(
                    name = "fecha",
                    required = false
            ) String fecha,

            @RequestParam(
                    name = "medicoUsername",
                    required = false
            ) String medicoUsername,

            Model model) {

        // ======================================
        // FECHA
        // ======================================

        LocalDate fechaConsulta =
                LocalDate.now(ZONA_COLOMBIA);

        if (fecha != null && !fecha.isBlank()) {

            try {

                fechaConsulta =
                        LocalDate.parse(fecha.trim());

            } catch (DateTimeParseException ex) {

                model.addAttribute(
                        "error",
                        "La fecha seleccionada no es vÃ¡lida. "
                        + "Se muestra la agenda de hoy."
                );
            }
        }

        String fechaSeleccionada =
                fechaConsulta.toString();

        // ======================================
        // MÃ‰DICO SELECCIONADO
        // ======================================

        String medicoSeleccionado =
                medicoUsername == null
                        ? ""
                        : medicoUsername.trim();

        // ======================================
        // CONSULTAR CITAS
        // ======================================

        List<Cita> registros;

        if (medicoSeleccionado.isBlank()) {

            registros = citaRepository.findByFecha(
                    fechaSeleccionada
            );

        } else {

            registros = citaRepository
                    .findByMedicoUsernameAndFechaOrderByHoraAsc(
                            medicoSeleccionado,
                            fechaSeleccionada
                    );
        }

        // ======================================
        // ORDENAR POR HORA
        // ======================================

        List<Cita> citasOrdenadas =
                new ArrayList<>(registros);

        citasOrdenadas.sort(
                Comparator.comparing(
                        Cita::getHora,
                        Comparator.nullsLast(
                                (hora1, hora2) ->
                                        compararHoras(
                                                hora1,
                                                hora2
                                        )
                        )
                )
        );

        // ======================================
        // CONVERTIR DATOS PARA THYMELEAF
        // ======================================

        List<Map<String, String>> citas =
                citasOrdenadas
                        .stream()
                        .map(this::convertirParaAgenda)
                        .toList();

        // ======================================
        // CONTADORES
        // ======================================

        long totalCitas = citas.size();

        long citasPendientes = 0;
        long citasConfirmadas = 0;
        long citasCanceladas = 0;

        for (Cita cita : citasOrdenadas) {

            String estado = normalizarEstado(
                    cita.getEstado()
            );

            if ("PROGRAMADA".equals(estado)
                    || "PENDIENTE".equals(estado)) {

                citasPendientes++;
            }

            if ("CONFIRMADA".equals(estado)) {

                citasConfirmadas++;
            }

            if ("CANCELADA".equals(estado)
                    || "CANCELADO".equals(estado)) {

                citasCanceladas++;
            }
        }

        // ======================================
        // MÃ‰DICOS PARA FILTRO
        // ======================================

        /*
         * Incluimos:
         *
         * 1. MÃ©dicos activos registrados.
         * 2. MÃ©dicos que aparecen en citas
         *    existentes, para consultar histÃ³ricos.
         *
         * Esto no significa que los mÃ©dicos
         * histÃ³ricos estÃ©n habilitados para
         * recibir nuevas citas.
         */

        Set<String> nombresMedicos =
                new TreeSet<>(
                        String.CASE_INSENSITIVE_ORDER
                );

        List<Usuario> usuariosMedicos =
                usuarioRepository.findByRol(
                        Rol.MEDICO
                );

        for (Usuario medico : usuariosMedicos) {

            if (medico.isActivo()
                    && medico.getUsername() != null
                    && !medico.getUsername().isBlank()) {

                nombresMedicos.add(
                        medico.getUsername().trim()
                );
            }
        }

        List<Cita> todasLasCitas =
                citaRepository.findAll();

        for (Cita cita : todasLasCitas) {

            String medico =
                    cita.getMedicoUsername();

            if (medico != null
                    && !medico.isBlank()) {

                nombresMedicos.add(
                        medico.trim()
                );
            }
        }

        List<String> medicos =
                new ArrayList<>(nombresMedicos);

        // ======================================
        // DATOS PARA THYMELEAF
        // ======================================

        model.addAttribute(
                "fechaSeleccionada",
                fechaSeleccionada
        );

        model.addAttribute(
                "medicoSeleccionado",
                medicoSeleccionado
        );

        model.addAttribute(
                "medicos",
                medicos
        );

        model.addAttribute(
                "citas",
                citas
        );

        model.addAttribute(
                "totalCitas",
                totalCitas
        );

        model.addAttribute(
                "citasPendientes",
                citasPendientes
        );

        model.addAttribute(
                "citasConfirmadas",
                citasConfirmadas
        );

        model.addAttribute(
                "citasCanceladas",
                citasCanceladas
        );

        return "recepcion-agenda";
    }

    // ==========================================
    // 2. CONVERTIR CITA PARA RECEPCIÃ“N
    // ==========================================

    private Map<String, String> convertirParaAgenda(
            Cita cita) {

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

        datos.put(
                "medicoUsername",
                texto(cita.getMedicoUsername())
        );

        /*
         * No enviamos informaciÃ³n clÃ­nica:
         *
         * - Motivo
         * - Observaciones
         * - DiagnÃ³stico
         * - Tratamiento
         * - Historiales
         */

        return datos;
    }

    // ==========================================
    // 3. COMPARAR HORAS
    // ==========================================

    private int compararHoras(
            String hora1,
            String hora2) {

        try {

            LocalTime primera =
                    LocalTime.parse(hora1.trim());

            LocalTime segunda =
                    LocalTime.parse(hora2.trim());

            return primera.compareTo(segunda);

        } catch (RuntimeException ex) {

            return hora1.compareTo(hora2);
        }
    }

    // ==========================================
    // 4. NORMALIZAR ESTADO
    // ==========================================

    private String normalizarEstado(
            String estado) {

        if (estado == null) {
            return "";
        }

        return estado
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    // ==========================================
    // 5. EVITAR VALORES NULOS
    // ==========================================

    private String texto(String valor) {

        return valor == null ? "" : valor;
    }
}

