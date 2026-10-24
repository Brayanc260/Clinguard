
package com.mycompany.clinica.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.mycompany.clinica.entity.Notificacion;
import com.mycompany.clinica.entity.Rol;
import com.mycompany.clinica.entity.Usuario;
import com.mycompany.clinica.repository.NotificacionRepository;
import com.mycompany.clinica.repository.UsuarioRepository;

@Service
public class NotificacionService {

    private final NotificacionRepository repository;
    private final UsuarioRepository usuarioRepository;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public NotificacionService(
            NotificacionRepository repository,
            UsuarioRepository usuarioRepository) {

        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    // =====================================================
    // CONSULTAR NOTIFICACIONES
    // =====================================================

    public List<Notificacion> obtenerPorUsuario(
            String username) {

        return repository
            .findByDestinatarioUsernameOrderByFechaCreacionDesc(
                username
            );
    }

    public List<Notificacion> obtenerPorEstado(
            String username,
            boolean leida) {

        return repository
            .findByDestinatarioUsernameAndLeidaOrderByFechaCreacionDesc(
                username,
                leida
            );
    }

    public long contarTodas(String username) {

        return repository.countByDestinatarioUsername(
            username
        );
    }

    public long contarNoLeidas(String username) {

        return repository
            .countByDestinatarioUsernameAndLeida(
                username,
                false
            );
    }

    public long contarLeidas(String username) {

        return repository
            .countByDestinatarioUsernameAndLeida(
                username,
                true
            );
    }

    // =====================================================
    // CREAR NOTIFICACIÃ“N
    // =====================================================

    public void crear(
            String destinatario,
            String tipo,
            String titulo,
            String mensaje,
            String referenciaId,
            String referenciaTipo) {

        if (destinatario == null
                || destinatario.isBlank()) {
            return;
        }

        // Solo crear avisos para cuentas reales y activas. Un pacienteId de
        // MongoDB no es un username del sistema.
        if (usuarioRepository.findByUsername(destinatario.trim())
                .filter(Usuario::isActivo).isEmpty()) {
            return;
        }

        Notificacion notificacion = new Notificacion(
            destinatario.trim(),
            tipo,
            titulo,
            mensaje,
            referenciaId,
            referenciaTipo
        );

        repository.save(notificacion);
    }

    // =====================================================
    // CREAR NOTIFICACIONES PARA VARIOS
    // =====================================================

    public void crearParaVarios(
            Set<String> destinatarios,
            String tipo,
            String titulo,
            String mensaje,
            String referenciaId,
            String referenciaTipo) {

        if (destinatarios == null
                || destinatarios.isEmpty()) {
            return;
        }

        for (String username : destinatarios) {

            crear(
                username,
                tipo,
                titulo,
                mensaje,
                referenciaId,
                referenciaTipo
            );
        }
    }

    // =====================================================
    // DESTINATARIOS ADMINISTRATIVOS ACTIVOS
    // =====================================================

    private Set<String> usuariosActivosPorRol(Rol rol) {
        Set<String> usuarios = new LinkedHashSet<>();

        for (Usuario usuario : usuarioRepository.findByRol(rol)) {
            if (usuario.isActivo()) {
                agregar(usuarios, usuario.getUsername());
            }
        }

        return usuarios;
    }

    private Set<String> obtenerAdministradores() {
        return usuariosActivosPorRol(Rol.ADMIN);
    }

    private Set<String> obtenerPersonalAdministrativo() {
        Set<String> destinatarios = obtenerAdministradores();
        destinatarios.addAll(usuariosActivosPorRol(Rol.RECEPCIONISTA));
        return destinatarios;
    }

    // =====================================================
    // PACIENTE REGISTRADO
    // DESTINATARIOS: ADMIN Y RECEPCIONISTA
    // =====================================================

    public void pacienteRegistrado(
            String pacienteId) {

        crearParaVarios(
            obtenerPersonalAdministrativo(),
            "PACIENTE",
            "Nuevo paciente registrado",
            "Se ha registrado un nuevo paciente en ClinGuard.",
            pacienteId,
            "PACIENTE"
        );
    }

    // =====================================================
    // NUEVA CITA
    // ADMIN - RECEPCIONISTA - MEDICO - ENFERMERO - USUARIO
    // =====================================================

    public void citaCreada(
            String citaId,
            String usuarioUsername,
            String medicoUsername,
            String enfermeroUsername) {

        Set<String> destinatarios = destinatariosCita(
                usuarioUsername, medicoUsername, enfermeroUsername);

        crearParaVarios(
                destinatarios,
                "CITA",
                "Nueva cita registrada",
                "Se ha registrado una cita mÃ©dica. Consulta tu agenda si estÃ¡ asignada a ti.",
                citaId,
                "CITA"
        );
    }

    // =====================================================
    // CITA REPROGRAMADA
    // =====================================================

    public void citaReprogramada(
            String citaId,
            String usuarioUsername,
            String medicoUsername,
            String enfermeroUsername) {

        Set<String> destinatarios =
            destinatariosCita(
                usuarioUsername,
                medicoUsername,
                enfermeroUsername
            );

        crearParaVarios(
            destinatarios,
            "CITA",
            "Cita reprogramada",
            "Se ha modificado la fecha u hora de una cita relacionada contigo.",
            citaId,
            "CITA"
        );
    }

    // =====================================================
    // CITA CANCELADA
    // =====================================================

    public void citaCancelada(
            String citaId,
            String usuarioUsername,
            String medicoUsername,
            String enfermeroUsername) {

        Set<String> destinatarios =
            destinatariosCita(
                usuarioUsername,
                medicoUsername,
                enfermeroUsername
            );

        crearParaVarios(
            destinatarios,
            "CITA",
            "Cita cancelada",
            "Una cita mÃ©dica relacionada contigo ha sido cancelada.",
            citaId,
            "CITA"
        );
    }

    // =====================================================
    // CITA CONFIRMADA
    // =====================================================

    public void citaConfirmada(
            String citaId,
            String usuarioUsername,
            String medicoUsername,
            String enfermeroUsername) {

        Set<String> destinatarios =
            destinatariosCita(
                usuarioUsername,
                medicoUsername,
                enfermeroUsername
            );

        crearParaVarios(
            destinatarios,
            "CITA",
            "Cita confirmada",
            "Una cita mÃ©dica relacionada contigo ha sido confirmada.",
            citaId,
            "CITA"
        );
    }

    // =====================================================
    // CITA ATENDIDA
    // =====================================================

    public void citaAtendida(
            String citaId,
            String usuarioUsername,
            String medicoUsername,
            String enfermeroUsername) {

        Set<String> destinatarios =
            destinatariosCita(
                usuarioUsername,
                medicoUsername,
                enfermeroUsername
            );

        crearParaVarios(
            destinatarios,
            "CITA",
            "AtenciÃ³n mÃ©dica finalizada",
            "Una cita mÃ©dica relacionada contigo ha sido marcada como atendida.",
            citaId,
            "CITA"
        );
    }

    // =====================================================
    // CITA REASIGNADA
    // =====================================================

    public void citaReasignada(
            String citaId,
            String usuarioUsername,
            String medicoAnterior,
            String enfermeroAnterior,
            String medicoNuevo,
            String enfermeroNuevo) {

        Set<String> destinatarios =
            destinatariosCita(
                usuarioUsername,
                medicoNuevo,
                enfermeroNuevo
            );

        agregarSiRol(destinatarios, medicoAnterior, Rol.MEDICO);
        agregarSiRol(destinatarios, enfermeroAnterior, Rol.ENFERMERO);

        crearParaVarios(
            destinatarios,
            "CITA",
            "ActualizaciÃ³n de personal asignado",
            "Se ha modificado la asignaciÃ³n de profesionales de una cita mÃ©dica.",
            citaId,
            "CITA"
        );
    }

    // =====================================================
    // RECORDATORIO DE CITA PRÃ“XIMA
    // =====================================================

    public void recordatorioCita(
            String citaId,
            String usuarioUsername,
            String medicoUsername,
            String enfermeroUsername) {

        Set<String> destinatarios = destinatariosCita(
                usuarioUsername, medicoUsername, enfermeroUsername);

        crearParaVarios(
            destinatarios,
            "CITA",
            "Recordatorio de cita mÃ©dica",
            "Tienes una cita mÃ©dica prÃ³xima. Consulta los detalles en tu agenda.",
            citaId,
            "CITA"
        );
    }

    // =====================================================
    // HISTORIAL CLÃNICO ACTUALIZADO
    // =====================================================

    public void historialActualizado(
            String historialId,
            String usuarioUsername,
            String medicoUsername) {

        Set<String> destinatarios =
            new LinkedHashSet<>();

        agregar(destinatarios, usuarioUsername);
        agregar(destinatarios, medicoUsername);

        crearParaVarios(
            destinatarios,
            "HISTORIAL",
            "Historial clÃ­nico actualizado",
            "Se ha registrado nueva informaciÃ³n en un historial clÃ­nico relacionado contigo.",
            historialId,
            "HISTORIAL"
        );
    }

    // =====================================================
    // NOTIFICACIÃ“N DE SEGURIDAD
    // =====================================================

    public void notificacionSeguridad(
            String username,
            String mensaje) {

        crear(
            username,
            "SEGURIDAD",
            "Aviso de seguridad",
            mensaje,
            null,
            "SEGURIDAD"
        );
    }

    // =====================================================
    // AVISO GENERAL A ADMINISTRADORES
    // =====================================================

    public void avisoAdministradores(
            String titulo,
            String mensaje) {

        crearParaVarios(
            obtenerAdministradores(),
            "SISTEMA",
            titulo,
            mensaje,
            null,
            "SISTEMA"
        );
    }

    // =====================================================
    // CONVERSIÃ“N MÃ‰DICA REALIZADA
    // ADMIN, MÃ‰DICO Y ENFERMERO ACTIVOS
    // =====================================================

    public void conversionRealizada(
            String conversionId,
            String usuarioResponsable,
            String operacion) {

        Set<String> destinatarios = new LinkedHashSet<>();
        destinatarios.addAll(usuariosActivosPorRol(Rol.ADMIN));
        destinatarios.addAll(usuariosActivosPorRol(Rol.MEDICO));
        destinatarios.addAll(usuariosActivosPorRol(Rol.ENFERMERO));

        // Un usuario con mÃ¡s de un criterio de pertenencia
        // solo recibe una notificaciÃ³n de este cÃ¡lculo.
        crearParaVarios(
            destinatarios,
            "CONVERSION",
            "ConversiÃ³n mÃ©dica registrada",
            "El usuario " + usuarioResponsable
                + " realizÃ³ una conversiÃ³n mÃ©dica (" + operacion + ").",
            conversionId,
            "CONVERSION"
        );
    }

    // =====================================================
    // STOCK BAJO DE MEDICAMENTOS
    // ADMIN Y ENFERMERO ACTIVOS
    // Umbral: 10 unidades. Solo al crear un medicamento con
    // stock bajo o al pasar de >10 a <=10.
    // =====================================================

    public void notificarStockBajo(
            String medicamentoId,
            String nombreMedicamento,
            Integer stockAnterior,
            Integer stockActual) {

        if (vacio(medicamentoId) || stockActual == null || stockActual < 0) {
            return;
        }

        final int umbral = 10;
        boolean actualmenteBajo = stockActual <= umbral;
        boolean antesNoEstabaBajo = stockAnterior == null || stockAnterior > umbral;

        if (!actualmenteBajo || !antesNoEstabaBajo) {
            return;
        }

        Set<String> destinatarios = new LinkedHashSet<>();
        destinatarios.addAll(usuariosActivosPorRol(Rol.ADMIN));
        destinatarios.addAll(usuariosActivosPorRol(Rol.ENFERMERO));

        String nombre = vacio(nombreMedicamento)
                ? "Medicamento sin nombre"
                : nombreMedicamento.trim();

        crearParaVarios(
                destinatarios,
                "MEDICAMENTO",
                "Alerta de stock bajo",
                "El medicamento " + nombre + " tiene " + stockActual
                        + " unidades disponibles (umbral: " + umbral + ").",
                medicamentoId,
                "MEDICAMENTO"
        );
    }

    // =====================================================
    // MARCAR UNA NOTIFICACIÃ“N COMO LEÃDA
    // =====================================================

    public void marcarComoLeida(
            String id,
            String username) {

        if (vacio(id) || vacio(username)) return;

        repository
            .findByIdAndDestinatarioUsername(
                id,
                username
            )
            .ifPresent(notificacion -> {

                if (!notificacion.isLeida()) {

                    notificacion.setLeida(true);
                    repository.save(notificacion);
                }
            });
    }

    // =====================================================
    // MARCAR TODAS COMO LEÃDAS
    // =====================================================

    public void marcarTodasComoLeidas(
            String username) {

        if (vacio(username)) return;

        List<Notificacion> pendientes =
            obtenerPorEstado(username, false);

        if (pendientes.isEmpty()) {
            return;
        }

        for (Notificacion notificacion : pendientes) {
            notificacion.setLeida(true);
        }

        repository.saveAll(pendientes);
    }

    // =====================================================
    // ELIMINAR NOTIFICACIÃ“N PROPIA
    // =====================================================

    public void eliminar(
            String id,
            String username) {

        if (vacio(id) || vacio(username)) return;

        repository
            .findByIdAndDestinatarioUsername(
                id,
                username
            )
            .ifPresent(repository::delete);
    }

    // =====================================================
    // OBTENER DESTINATARIOS DE UNA CITA
    // =====================================================

    private Set<String> destinatariosCita(
            String usuarioUsername,
            String medicoUsername,
            String enfermeroUsername) {

        Set<String> destinatarios = obtenerPersonalAdministrativo();
        agregarSiRol(destinatarios, medicoUsername, Rol.MEDICO);
        agregarSiRol(destinatarios, enfermeroUsername, Rol.ENFERMERO);
        agregarSiRol(destinatarios, usuarioUsername, Rol.USUARIO);
        return destinatarios;
    }

    private void agregarSiRol(Set<String> destinatarios,
                             String username, Rol rolEsperado) {
        if (vacio(username)) return;
        usuarioRepository.findByUsername(username.trim())
                .filter(Usuario::isActivo)
                .filter(u -> u.getRol() == rolEsperado)
                .ifPresent(u -> agregar(destinatarios, u.getUsername()));
    }

    // =====================================================
    // EVITAR DESTINATARIOS VACÃOS
    // =====================================================

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private void agregar(
            Set<String> destinatarios,
            String username) {

        if (username != null
                && !username.isBlank()) {

            destinatarios.add(
                username.trim()
            );
        }
    }
}

