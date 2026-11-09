package com.mycompany.clinica.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "historiales")
public class Historial {
    @Id
    private String id;
    private String pacienteId;
    private String usuarioUsername;
    private String medicoUsername;
    private String fecha;
    private String motivoConsulta;
    private String diagnostico;
    private String tratamiento;
    private String observaciones;
    private String creadoPor;
    private Instant fechaCreacion;
    private String modificadoPor;
    private Instant fechaModificacion;
    private boolean anulado;
    private String motivoAnulacion;

    public Historial() {
    }

    public String getId() {
        return id;
    }

    public void setId(String x) {
        id = x;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(String x) {
        pacienteId = x;
    }

    public String getUsuarioUsername() {
        return usuarioUsername;
    }

    public void setUsuarioUsername(String x) {
        usuarioUsername = x;
    }

    public String getMedicoUsername() {
        return medicoUsername;
    }

    public void setMedicoUsername(String x) {
        medicoUsername = x;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String x) {
        fecha = x;
    }

    public String getMotivoConsulta() {
        return motivoConsulta;
    }

    public void setMotivoConsulta(String x) {
        motivoConsulta = x;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String x) {
        diagnostico = x;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String x) {
        tratamiento = x;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String x) {
        observaciones = x;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String x) {
        creadoPor = x;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant x) {
        fechaCreacion = x;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(String x) {
        modificadoPor = x;
    }

    public Instant getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(Instant x) {
        fechaModificacion = x;
    }

    public boolean isAnulado() {
        return anulado;
    }

    public void setAnulado(boolean x) {
        anulado = x;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String x) {
        motivoAnulacion = x;
    }
}
