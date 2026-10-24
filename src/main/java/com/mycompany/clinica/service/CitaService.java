package com.mycompany.clinica.service;

import com.mycompany.clinica.entity.Cita;
import com.mycompany.clinica.entity.Paciente;
import com.mycompany.clinica.repository.PacienteRepository;
import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;
import com.mycompany.clinica.repository.UsuarioRepository;
import com.mycompany.clinica.repository.CitaRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;

@Service
public class CitaService {

    // ==========================================
    // CONFIGURACIÃ“N
    // ==========================================

    private static final ZoneId ZONA_COLOMBIA =
            ZoneId.of("America/Bogota");

    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final Set<String> ESTADOS_NO_DISPONIBLES =
            Set.of(
                    "CANCELADA",
                    "CANCELADO",
                    "RECHAZADA",
                    "RECHAZADO"
            );

    private static final Set<String> ESTADOS_FINALES =
            Set.of(
                    "CANCELADA",
                    "CANCELADO",
                    "RECHAZADA",
                    "RECHAZADO",
                    "COMPLETADA",
                    "COMPLETADO",
                    "ATENDIDA",
                    "ATENDIDO"
            );

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PROGRAMADA", "PENDIENTE", "CONFIRMADA", "CANCELADA",
            "CANCELADO", "RECHAZADA", "RECHAZADO", "COMPLETADA",
            "COMPLETADO", "ATENDIDA", "ATENDIDO", "EN_CURSO", "REPROGRAMADA"
    );

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;

    public CitaService(CitaRepository citaRepository,
                       UsuarioRepository usuarioRepository,
                       PacienteRepository pacienteRepository) {
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
    }

    // ==========================================
    // 1. MÃ‰TODOS GENERALES
    // ==========================================

    public List<Cita> listarTodas() {
        return citaRepository.findAll();
    }

    public Optional<Cita> buscarPorId(String id) {

        validarTexto(id, "El identificador");

        return citaRepository.findById(id.trim());
    }

    /**
     * Punto de guardado compartido. Al crear o cambiar horario/asignaciones
     * aplica las mismas reglas que RecepciÃ³n. Los cambios clÃ­nicos o de estado
     * de citas existentes no exigen que su fecha siga siendo futura.
     */
    public synchronized Cita guardar(Cita cita) {
        if (cita == null) {
            throw new IllegalArgumentException("No se puede guardar una cita vacÃ­a.");
        }
        String estado = normalizarEstado(cita.getEstado());
        if (estado.isBlank()) estado = "PROGRAMADA";
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new IllegalArgumentException("Estado de cita no permitido: " + estado);
        }
        cita.setEstado(estado);

        Cita anterior = null;
        if (cita.getId() != null && !cita.getId().isBlank()) {
            anterior = citaRepository.findById(cita.getId())
                    .orElseThrow(() -> new IllegalArgumentException("La cita no existe."));
        }
        boolean cambioAgenda = anterior == null
                || !Objects.equals(anterior.getFecha(), cita.getFecha())
                || !Objects.equals(anterior.getHora(), cita.getHora())
                || !Objects.equals(anterior.getMedicoUsername(), cita.getMedicoUsername())
                || !Objects.equals(anterior.getUsuarioUsername(), cita.getUsuarioUsername())
                || !Objects.equals(anterior.getPacienteId(), cita.getPacienteId());

        if (cambioAgenda) {
            // Las citas finalizadas o canceladas no deben cambiar de agenda.
            if (anterior != null && ESTADOS_FINALES.contains(normalizarEstado(anterior.getEstado()))) {
                throw new IllegalStateException("No se puede reprogramar una cita cancelada o finalizada.");
            }
            String fecha = normalizarFecha(cita.getFecha());
            String hora = normalizarHora(cita.getHora());
            String medico = exigirUsername(cita.getMedicoUsername(), "El mÃ©dico");
            String paciente = cita.getUsuarioUsername();
            String pacienteId = cita.getPacienteId();
            if (pacienteId != null) pacienteId = pacienteId.trim();
            validarFechaHoraFutura(fecha, hora);
            validarMedicoActivo(medico);
            if (pacienteId != null && !pacienteId.isBlank()) {
                validarPacienteActivo(pacienteId);
                if (!ESTADOS_NO_DISPONIBLES.contains(estado)) {
                    validarSinCrucesDirectorio(pacienteId, medico, fecha, hora, cita.getId());
                }
            } else {
                paciente = exigirUsername(paciente, "El usuario del paciente");
                validarUsuarioPacienteActivo(paciente);
                if (!ESTADOS_NO_DISPONIBLES.contains(estado)) {
                    validarSinCruces(paciente, medico, fecha, hora, cita.getId());
                }
            }
            cita.setFecha(fecha);
            cita.setHora(hora);
            cita.setMedicoUsername(medico);
            cita.setPacienteId(pacienteId == null || pacienteId.isBlank() ? null : pacienteId);
            cita.setUsuarioUsername(paciente);
        }
        return citaRepository.save(cita);
    }

    public void eliminar(String id) {

        validarTexto(id, "El identificador");

        citaRepository.deleteById(id.trim());
    }

    public boolean existePorId(String id) {

        if (id == null || id.isBlank()) {
            return false;
        }

        return citaRepository.existsById(id.trim());
    }

    // ==========================================
    // 2. CONSULTAS POR USUARIO Y PERSONAL
    // ==========================================

    public List<Cita> buscarPorUsuario(String username) {

        validarTexto(username, "El usuario");

        return citaRepository.findByUsuarioUsername(
                username.trim()
        );
    }

    public List<Cita> buscarPorMedico(String username) {

        validarTexto(username, "El mÃ©dico");

        return citaRepository.findByMedicoUsername(
                username.trim()
        );
    }

    public List<Cita> buscarPorEnfermero(String username) {

        validarTexto(username, "El enfermero");

        return citaRepository.findByEnfermeroUsername(
                username.trim()
        );
    }

    // ==========================================
    // 3. CONSULTAS GENERALES
    // ==========================================

    public List<Cita> buscarPorFecha(String fecha) {

        String fechaNormalizada =
                normalizarFecha(fecha);

        return citaRepository.findByFecha(
                fechaNormalizada
        );
    }

    public List<Cita> buscarPorEstado(String estado) {

        validarTexto(estado, "El estado");

        return citaRepository.findByEstado(
                estado.trim()
        );
    }

    public List<Cita> buscarPorFechaYEstado(
            String fecha,
            String estado) {

        String fechaNormalizada =
                normalizarFecha(fecha);

        validarTexto(estado, "El estado");

        return citaRepository.findByFechaAndEstado(
                fechaNormalizada,
                estado.trim()
        );
    }

    // ==========================================
    // 4. AGENDA MÃ‰DICA
    // ==========================================

    public List<Cita> obtenerAgendaMedico(
            String medicoUsername,
            String fecha) {

        validarTexto(
                medicoUsername,
                "El mÃ©dico"
        );

        String fechaNormalizada =
                normalizarFecha(fecha);

        return citaRepository
                .findByMedicoUsernameAndFechaOrderByHoraAsc(
                        medicoUsername.trim(),
                        fechaNormalizada
                );
    }

    public List<Cita> listarOrdenadas() {

        return citaRepository
                .findAllByOrderByFechaAscHoraAsc();
    }

    // ==========================================
    // 5. PRÃ“XIMAS CITAS
    // ==========================================

    public List<Cita> listarProximas() {

        LocalDateTime ahora =
                LocalDateTime.now(ZONA_COLOMBIA);

        String hoy =
                ahora.toLocalDate().toString();

        return citaRepository
                .findByFechaGreaterThanEqualOrderByFechaAscHoraAsc(
                        hoy
                )
                .stream()
                .filter(cita -> {

                    LocalDateTime fechaHora =
                            obtenerFechaHora(cita);

                    return fechaHora != null
                            && !fechaHora.isBefore(ahora);
                })
                .filter(cita ->
                        !ESTADOS_FINALES.contains(
                                normalizarEstado(
                                        cita.getEstado()
                                )
                        )
                )
                .toList();
    }

    // ==========================================
    // 6. DISPONIBILIDAD DEL MÃ‰DICO
    // ==========================================

    public boolean estaDisponible(
            String medicoUsername,
            String fecha,
            String hora) {

        return estaDisponible(
                medicoUsername,
                fecha,
                hora,
                null
        );
    }

    public boolean estaDisponible(
            String medicoUsername,
            String fecha,
            String hora,
            String citaExcluirId) {

        validarTexto(
                medicoUsername,
                "El mÃ©dico"
        );

        String fechaNormalizada =
                normalizarFecha(fecha);

        LocalTime horaSolicitada =
                convertirHora(hora);

        /*
         * Consultamos las citas del mÃ©dico
         * en la fecha indicada.
         *
         * Se comparan horas como LocalTime
         * para admitir registros existentes
         * con formatos como 09:00 y 09:00:00.
         */

        List<Cita> citasDelMedico =
                citaRepository
                        .findByMedicoUsernameAndFechaOrderByHoraAsc(
                                medicoUsername.trim(),
                                fechaNormalizada
                        );

        return citasDelMedico.stream()
                .filter(cita ->
                        citaExcluirId == null
                                || !citaExcluirId.equals(
                                        cita.getId()
                                )
                )
                .filter(cita ->
                        !ESTADOS_NO_DISPONIBLES.contains(
                                normalizarEstado(
                                        cita.getEstado()
                                )
                        )
                )
                .noneMatch(cita -> {

                    LocalTime horaExistente =
                            obtenerHora(cita);

                    return horaExistente == null
                            || horaExistente.equals(
                                    horaSolicitada
                            );
                });
    }

    // ==========================================
    // 7. REGISTRAR CITA DESDE RECEPCIÃ“N
    // ==========================================

    public synchronized Cita registrarDesdeRecepcion(
            String usuarioUsername,
            String medicoUsername,
            String fecha,
            String hora) {

        validarTexto(
                usuarioUsername,
                "El usuario"
        );

        validarTexto(
                medicoUsername,
                "El mÃ©dico"
        );

        String usuario =
                usuarioUsername.trim();

        String medico =
                medicoUsername.trim();

        // Verificar las cuentas antes de reservar el horario.
        validarUsuarioPacienteActivo(usuario);
        validarMedicoActivo(medico);

        String fechaNormalizada =
                normalizarFecha(fecha);

        String horaNormalizada =
                normalizarHora(hora);

        validarFechaHoraFutura(
                fechaNormalizada,
                horaNormalizada
        );

        validarSinCruces(usuario, medico, fechaNormalizada, horaNormalizada, null);

        Cita cita = new Cita();

        cita.setUsuarioUsername(usuario);
        cita.setMedicoUsername(medico);

        cita.setFecha(fechaNormalizada);
        cita.setHora(horaNormalizada);

        cita.setEstado("PROGRAMADA");

        /*
         * RecepciÃ³n no modifica informaciÃ³n
         * clÃ­nica del paciente.
         */

        return citaRepository.save(cita);
    }


    /** Reserva para un paciente existente en la colecciÃ³n pacientes (sin login). */
    public synchronized Cita registrarPacienteDesdeRecepcion(
            String pacienteId, String medicoUsername, String fecha, String hora) {
        validarTexto(pacienteId, "El paciente");
        String medico = exigirUsername(medicoUsername, "El mÃ©dico");
        String f = normalizarFecha(fecha);
        String h = normalizarHora(hora);
        validarPacienteActivo(pacienteId);
        validarMedicoActivo(medico);
        validarFechaHoraFutura(f, h);
        validarSinCrucesDirectorio(pacienteId.trim(), medico, f, h, null);
        Cita cita = new Cita();
        cita.setPacienteId(pacienteId.trim());
        cita.setMedicoUsername(medico);
        cita.setFecha(f);
        cita.setHora(h);
        cita.setEstado("PROGRAMADA");
        return citaRepository.save(cita);
    }

    private void validarPacienteActivo(String id) {
        Paciente p = pacienteRepository.findById(id.trim())
                .orElseThrow(() -> new IllegalArgumentException("El paciente seleccionado no estÃ¡ registrado."));
        if (!p.isActivo()) throw new IllegalStateException("El paciente estÃ¡ inactivo.");
    }

    private void validarSinCrucesDirectorio(String pacienteId, String medico,
            String fecha, String hora, String excluirId) {
        if (!estaDisponible(medico, fecha, hora, excluirId))
            throw new IllegalStateException("El mÃ©dico ya tiene una cita en ese horario.");
        java.time.LocalTime solicitada = convertirHora(hora);
        boolean ocupado = citaRepository.findByFecha(fecha).stream()
                .filter(c -> excluirId == null || !excluirId.equals(c.getId()))
                .filter(c -> pacienteId.equals(c.getPacienteId()))
                .filter(c -> !ESTADOS_NO_DISPONIBLES.contains(normalizarEstado(c.getEstado())))
                .anyMatch(c -> solicitada.equals(obtenerHora(c)));
        if (ocupado) throw new IllegalStateException("El paciente ya tiene una cita en ese horario.");
    }

    // ==========================================
    // 8. REPROGRAMAR CITA
    // ==========================================

    public synchronized Cita reprogramarDesdeRecepcion(
            String id,
            String nuevaFecha,
            String nuevaHora) {

        Cita cita = obtenerCitaObligatoria(id);

        String estado =
                normalizarEstado(cita.getEstado());

        if (ESTADOS_FINALES.contains(estado)) {

            throw new IllegalStateException(
                    "No se puede reprogramar una cita "
                    + "cancelada, rechazada o finalizada."
            );
        }

        String fechaNormalizada =
                normalizarFecha(nuevaFecha);

        String horaNormalizada =
                normalizarHora(nuevaHora);

        validarFechaHoraFutura(
                fechaNormalizada,
                horaNormalizada
        );

        String medico =
                cita.getMedicoUsername();

        validarTexto(
                medico,
                "El mÃ©dico asignado"
        );
        validarMedicoActivo(medico);

        if (cita.getPacienteId() != null && !cita.getPacienteId().isBlank()) {
            validarSinCrucesDirectorio(cita.getPacienteId(), medico, fechaNormalizada, horaNormalizada, cita.getId());
        } else {
            validarSinCruces(cita.getUsuarioUsername(), medico, fechaNormalizada, horaNormalizada, cita.getId());
        }

        cita.setFecha(fechaNormalizada);
        cita.setHora(horaNormalizada);

        // La nueva programaciÃ³n requiere confirmaciÃ³n.
        cita.setEstado("PROGRAMADA");

        return citaRepository.save(cita);
    }

    // ==========================================
    // 9. CONFIRMAR CITA
    // ==========================================

    public Cita confirmarDesdeRecepcion(String id) {

        Cita cita = obtenerCitaObligatoria(id);

        String estado =
                normalizarEstado(cita.getEstado());

        if ("CONFIRMADA".equals(estado)) {

            throw new IllegalStateException(
                    "Esta cita ya se encuentra confirmada."
            );
        }

        if (ESTADOS_FINALES.contains(estado)) {

            throw new IllegalStateException(
                    "No se puede confirmar una cita "
                    + "cancelada, rechazada o finalizada."
            );
        }

        LocalDateTime fechaHora =
                obtenerFechaHora(cita);

        if (fechaHora == null
                || !fechaHora.isAfter(
                        LocalDateTime.now(
                                ZONA_COLOMBIA
                        )
                )) {

            throw new IllegalStateException(
                    "No se puede confirmar una cita "
                    + "cuya fecha y hora ya pasaron."
            );
        }

        if (!Set.of("PROGRAMADA", "PENDIENTE", "REPROGRAMADA").contains(estado)) {
            throw new IllegalStateException("El estado actual no permite confirmar esta cita.");
        }
        validarMedicoActivo(cita.getMedicoUsername());
        if (cita.getPacienteId() != null && !cita.getPacienteId().isBlank()) {
            validarSinCrucesDirectorio(cita.getPacienteId(), cita.getMedicoUsername(),
                    normalizarFecha(cita.getFecha()), normalizarHora(cita.getHora()), cita.getId());
        } else {
            validarSinCruces(cita.getUsuarioUsername(), cita.getMedicoUsername(),
                    normalizarFecha(cita.getFecha()), normalizarHora(cita.getHora()), cita.getId());
        }
        cita.setEstado("CONFIRMADA");

        return citaRepository.save(cita);
    }

    // ==========================================
    // 10. CANCELAR CITA
    // ==========================================

    public Cita cancelarDesdeRecepcion(String id) {

        Cita cita = obtenerCitaObligatoria(id);

        String estado =
                normalizarEstado(cita.getEstado());

        if (ESTADOS_FINALES.contains(estado)) {

            throw new IllegalStateException(
                    "No se puede cancelar una cita "
                    + "que ya fue cancelada o finalizada."
            );
        }

        cita.setEstado("CANCELADA");

        return citaRepository.save(cita);
    }

    // ==========================================
    // 11. CONTADORES PARA DASHBOARD
    // ==========================================

    public long contarCitasHoy() {

        String hoy =
                LocalDate.now(ZONA_COLOMBIA)
                        .toString();

        return citaRepository.countByFecha(hoy);
    }

    public long contarPorEstado(String estado) {

        validarTexto(estado, "El estado");

        return citaRepository.countByEstado(
                estado.trim()
        );
    }

    public long contarPorFechaYEstado(
            String fecha,
            String estado) {

        String fechaNormalizada =
                normalizarFecha(fecha);

        validarTexto(estado, "El estado");

        return citaRepository.countByFechaAndEstado(
                fechaNormalizada,
                estado.trim()
        );
    }

    // ==========================================
    // 12. OBTENER CITA OBLIGATORIA
    // ==========================================

    private Cita obtenerCitaObligatoria(String id) {

        validarTexto(
                id,
                "El identificador de la cita"
        );

        return citaRepository
                .findById(id.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La cita solicitada no existe."
                        )
                );
    }

    // ==========================================
    // 13. VALIDACIÃ“N DE CUENTAS
    // ==========================================

    private void validarMedicoActivo(String username) {
        validarTexto(username, "El mÃ©dico");
        Usuario medico = usuarioRepository.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El mÃ©dico seleccionado no estÃ¡ registrado."));
        if (medico.getRol() != Rol.MEDICO) {
            throw new IllegalArgumentException(
                    "El usuario seleccionado no tiene el rol de mÃ©dico.");
        }
        if (!medico.isActivo()) {
            throw new IllegalStateException(
                    "No se pueden asignar citas a un mÃ©dico inactivo.");
        }
    }

    private void validarUsuarioPacienteActivo(String username) {
        validarTexto(username, "El usuario del paciente");
        Usuario usuario = usuarioRepository.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario del paciente no estÃ¡ registrado."));
        if (usuario.getRol() != Rol.USUARIO) {
            throw new IllegalArgumentException(
                    "La cuenta seleccionada no tiene el rol de usuario paciente.");
        }
        if (!usuario.isActivo()) {
            throw new IllegalStateException(
                    "No se pueden registrar citas para una cuenta inactiva.");
        }
    }

    /** El paciente tampoco debe estar agendado a esa misma hora con otro mÃ©dico. */
    private void validarSinCruces(String usuario, String medico,
                                  String fecha, String hora, String excluirId) {
        if (!estaDisponible(medico, fecha, hora, excluirId)) {
            throw new IllegalStateException("El mÃ©dico ya tiene una cita en ese horario.");
        }
        LocalTime solicitada = convertirHora(hora);
        boolean pacienteOcupado = citaRepository.findByUsuarioUsername(usuario).stream()
                .filter(c -> excluirId == null || !excluirId.equals(c.getId()))
                .filter(c -> fecha.equals(c.getFecha()))
                .filter(c -> !ESTADOS_NO_DISPONIBLES.contains(normalizarEstado(c.getEstado())))
                .anyMatch(c -> {
                    LocalTime existente = obtenerHora(c);
                    return existente == null || existente.equals(solicitada);
                });
        if (pacienteOcupado) {
            throw new IllegalStateException("El paciente ya tiene una cita en ese horario.");
        }
    }

    private String exigirUsername(String valor, String nombre) {
        validarTexto(valor, nombre);
        return valor.trim();
    }

    // ==========================================
    // 13. VALIDACIONES GENERALES
    // ==========================================

    private void validarTexto(
            String valor,
            String nombreCampo) {

        if (valor == null || valor.isBlank()) {

            throw new IllegalArgumentException(
                    nombreCampo + " es obligatorio."
            );
        }
    }

    private String normalizarFecha(String fecha) {

        validarTexto(fecha, "La fecha");

        try {

            return LocalDate.parse(
                    fecha.trim()
            ).toString();

        } catch (DateTimeParseException ex) {

            throw new IllegalArgumentException(
                    "La fecha debe tener el formato yyyy-MM-dd."
            );
        }
    }

    private LocalTime convertirHora(String hora) {

        validarTexto(hora, "La hora");

        try {

            return LocalTime.parse(
                    hora.trim()
            );

        } catch (DateTimeParseException ex) {

            throw new IllegalArgumentException(
                    "La hora debe tener un formato vÃ¡lido, "
                    + "por ejemplo 09:30."
            );
        }
    }

    private String normalizarHora(String hora) {

        LocalTime horaConvertida =
                convertirHora(hora);

        if (horaConvertida.getSecond() != 0
                || horaConvertida.getNano() != 0) {

            throw new IllegalArgumentException(
                    "Las citas deben programarse "
                    + "con precisiÃ³n de minutos."
            );
        }

        return horaConvertida.format(
                FORMATO_HORA
        );
    }

    private void validarFechaHoraFutura(
            String fecha,
            String hora) {

        LocalDateTime fechaHora =
                LocalDateTime.of(
                        LocalDate.parse(
                                normalizarFecha(fecha)
                        ),
                        convertirHora(hora)
                );

        if (!fechaHora.isAfter(
                LocalDateTime.now(
                        ZONA_COLOMBIA
                )
        )) {

            throw new IllegalArgumentException(
                    "La fecha y hora de la cita "
                    + "deben ser futuras."
            );
        }
    }

    // ==========================================
    // 14. UTILIDADES INTERNAS
    // ==========================================

    private LocalDateTime obtenerFechaHora(Cita cita) {

        if (cita == null) {
            return null;
        }

        try {

            return LocalDateTime.of(
                    LocalDate.parse(
                            cita.getFecha()
                    ),
                    LocalTime.parse(
                            cita.getHora()
                    )
            );

        } catch (RuntimeException ex) {

            return null;
        }
    }

    private LocalTime obtenerHora(Cita cita) {

        if (cita == null
                || cita.getHora() == null) {

            return null;
        }

        try {

            return LocalTime.parse(
                    cita.getHora()
            );

        } catch (RuntimeException ex) {

            return null;
        }
    }

    private String normalizarEstado(String estado) {

        if (estado == null) {
            return "";
        }

        return estado
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}

