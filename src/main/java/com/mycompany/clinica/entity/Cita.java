package com.mycompany.clinica.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "citas")
public class Cita {

    @Id
    private String id;
    private String pacienteId;
    private String usuarioUsername;
    private String medicoUsername;
    private String enfermeroUsername;
    private String fecha; 
    private String hora;
    private String estado;
    
    // Campos restaurados
    private String motivo;
    private String observaciones;

    public Cita() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }

    public String getUsuarioUsername() { return usuarioUsername; }
    public void setUsuarioUsername(String usuarioUsername) { this.usuarioUsername = usuarioUsername; }

    public String getMedicoUsername() { return medicoUsername; }
    public void setMedicoUsername(String medicoUsername) { this.medicoUsername = medicoUsername; }

    public String getEnfermeroUsername() { return enfermeroUsername; }
    public void setEnfermeroUsername(String enfermeroUsername) { this.enfermeroUsername = enfermeroUsername; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}