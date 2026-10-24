package com.mycompany.clinica.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "citas")
public class Cita {
    @Id
    private String id;

    @NotBlank(message = "La fecha es obligatoria.")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Formato de fecha: yyyy-MM-dd.")
    private String fecha;

    @NotBlank(message = "La hora es obligatoria.")
    @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "Formato de hora: HH:mm.")
    private String hora;

    @Size(max = 500)
    private String motivo;
    // Cuenta de acceso opcional para citas de pacientes con usuario.
    @Size(max = 100)
    private String usuarioUsername;
    // Referencia al directorio pacientes; no requiere una cuenta de acceso.
    private String pacienteId;
    @NotBlank(message = "El mÃƒÂ©dico es obligatorio.")
    @Size(max = 100)
    private String medicoUsername;
    @Size(max = 100)
    private String enfermeroUsername;
    @NotBlank(message = "El estado es obligatorio.")
    @Pattern(regexp = "PROGRAMADA|PENDIENTE|CONFIRMADA|CANCELADA|CANCELADO|RECHAZADA|RECHAZADO|COMPLETADA|COMPLETADO|ATENDIDA|ATENDIDO|EN_CURSO|REPROGRAMADA", message = "Estado no permitido.")
    private String estado = "PROGRAMADA";
    @Size(max = 2000)
    private String observaciones;

    public Cita() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getUsuarioUsername() {
        return usuarioUsername;
    }

    public void setUsuarioUsername(String usuarioUsername) {
        this.usuarioUsername = usuarioUsername;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(String pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getMedicoUsername() {
        return medicoUsername;
    }

    public void setMedicoUsername(String medicoUsername) {
        this.medicoUsername = medicoUsername;
    }

    public String getEnfermeroUsername() {
        return enfermeroUsername;
    }

    public void setEnfermeroUsername(String enfermeroUsername) {
        this.enfermeroUsername = enfermeroUsername;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

