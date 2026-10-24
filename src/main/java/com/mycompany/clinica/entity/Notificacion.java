
package com.mycompany.clinica.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notificaciones")
public class Notificacion {

    @Id
    private String id;

    @Indexed
    private String destinatarioUsername;

    private String tipo;
    private String titulo;
    private String mensaje;

    private String referenciaId;
    private String referenciaTipo;

    private boolean leida = false;

    private Instant fechaCreacion = Instant.now();

    public Notificacion() {
    }

    public Notificacion(
            String destinatarioUsername,
            String tipo,
            String titulo,
            String mensaje,
            String referenciaId,
            String referenciaTipo) {

        this.destinatarioUsername = destinatarioUsername;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.referenciaId = referenciaId;
        this.referenciaTipo = referenciaTipo;
        this.leida = false;
        this.fechaCreacion = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDestinatarioUsername() {
        return destinatarioUsername;
    }

    public void setDestinatarioUsername(String destinatarioUsername) {
        this.destinatarioUsername = destinatarioUsername;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getReferenciaId() {
        return referenciaId;
    }

    public void setReferenciaId(String referenciaId) {
        this.referenciaId = referenciaId;
    }

    public String getReferenciaTipo() {
        return referenciaTipo;
    }

    public void setReferenciaTipo(String referenciaTipo) {
        this.referenciaTipo = referenciaTipo;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}

