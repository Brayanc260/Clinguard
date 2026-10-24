package com.mycompany.clinica.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Document(collection = "pacientes")
public class Paciente {

    @Id
    private String id;

    private String tipoDocumento;

    @Size(max = 30, message = "El documento no puede superar los 30 caracteres")
    private String documento;

    private String nombre;
    private String identificacion;
    private Double pesoKg;
    private Integer edad;

    @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres")
    private String nombres;

    @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
    private String apellidos;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaNacimiento;

    private String sexo;
    private String tipoSangre;
    private String eps;
    private String telefono;
    private String correo;
    private String direccion;
    private String contactoEmergencia;
    private String telefonoEmergencia;
    private boolean activo = true;
    private Instant fechaRegistro;

    public Paciente() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getDocumento() {
        return (documento != null && !documento.isBlank()) ? documento : identificacion;
    }
    public void setDocumento(String documento) {
        this.documento = documento;
        this.identificacion = documento;
    }

    public String getIdentificacion() {
        return getDocumento();
    }
    public void setIdentificacion(String identificacion) {
        setDocumento(identificacion);
    }

    public String getNombres() {
        if (nombres != null && !nombres.isBlank()) return nombres;
        return (nombre != null) ? nombre : "";
    }
    public void setNombres(String nombres) {
        this.nombres = nombres;
        if (this.nombre == null || this.nombre.isBlank()) this.nombre = nombres;
    }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getNombre() {
        return getNombreCompleto();
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
        if (this.nombres == null || this.nombres.isBlank()) this.nombres = nombre;
    }

    public String getNombreCompleto() {
        String n = getNombres();
        String a = apellidos == null ? "" : apellidos.trim();
        String full = (n + " " + a).trim();
        return full.isEmpty() ? (nombre != null ? nombre : "") : full;
    }

    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }

    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getTipoSangre() { return tipoSangre; }
    public void setTipoSangre(String tipoSangre) { this.tipoSangre = tipoSangre; }

    public String getEps() { return eps; }
    public void setEps(String eps) { this.eps = eps; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getContactoEmergencia() { return contactoEmergencia; }
    public void setContactoEmergencia(String contactoEmergencia) { this.contactoEmergencia = contactoEmergencia; }

    public String getTelefonoEmergencia() { return telefonoEmergencia; }
    public void setTelefonoEmergencia(String telefonoEmergencia) { this.telefonoEmergencia = telefonoEmergencia; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public Instant getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Instant fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
