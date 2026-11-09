
package com.mycompany.clinica.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Document(collection = "pacientes")
public class Paciente {

    // ============================================
    // IDENTIFICACIÓN EN MONGODB
    // ============================================

    @Id
    private String id;

    // ============================================
    // DATOS PERSONALES
    // ============================================

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 30, message = "El documento no puede superar los 30 caracteres")
    private String documento;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
    private String apellidos;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaNacimiento;

    private String sexo;

    private String tipoSangre;

    // ============================================
    // DATOS FÍSICOS DEL PACIENTE
    // ============================================

    @DecimalMin(value = "0.01", message = "El peso debe ser mayor que cero")
    @DecimalMax(value = "500.00", message = "El peso no puede superar los 500 kg")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal peso;

    @DecimalMin(value = "0.01", message = "La estatura debe ser mayor que cero")
    @DecimalMax(value = "300.00", message = "La estatura no puede superar los 300 cm")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal estatura;

    // ============================================
    // INFORMACIÓN DE SALUD Y CONTACTO
    // ============================================

    @Size(max = 120, message = "El nombre de la EPS es demasiado largo")
    private String eps;

    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String telefono;

    @Email(message = "Ingrese un correo electrónico válido")
    @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
    private String correo;

    @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
    private String direccion;

    // ============================================
    // CONTACTO DE EMERGENCIA
    // ============================================

    @Size(max = 120, message = "El contacto de emergencia es demasiado largo")
    private String contactoEmergencia;

    @Size(max = 20, message = "El teléfono de emergencia no puede superar los 20 caracteres")
    private String telefonoEmergencia;

    // ============================================
    // CONTROL ADMINISTRATIVO
    // ============================================

    private boolean activo = true;

    private Instant fechaRegistro;

    // ============================================
    // CONSTRUCTOR
    // ============================================

    public Paciente() {
    }

    // ============================================
    // GETTERS Y SETTERS
    // ============================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getTipoSangre() {
        return tipoSangre;
    }

    public void setTipoSangre(String tipoSangre) {
        this.tipoSangre = tipoSangre;
    }

    // ============================================
    // PESO Y ESTATURA
    // ============================================

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }

    public BigDecimal getEstatura() {
        return estatura;
    }

    public void setEstatura(BigDecimal estatura) {
        this.estatura = estatura;
    }

    // ============================================
    // CÁLCULO AUTOMÁTICO DEL IMC
    // ============================================

    public BigDecimal getImc() {

        if (peso == null || estatura == null ||
                peso.signum() <= 0 || estatura.signum() <= 0) {
            return null;
        }

        BigDecimal estaturaMetros = estatura.divide(
                BigDecimal.valueOf(100),
                4,
                RoundingMode.HALF_UP);

        BigDecimal estaturaCuadrada = estaturaMetros.multiply(estaturaMetros);

        return peso.divide(
                estaturaCuadrada,
                2,
                RoundingMode.HALF_UP);
    }

    // ============================================
    // INFORMACIÓN DE CONTACTO
    // ============================================

    public String getEps() {
        return eps;
    }

    public void setEps(String eps) {
        this.eps = eps;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getContactoEmergencia() {
        return contactoEmergencia;
    }

    public void setContactoEmergencia(String contactoEmergencia) {
        this.contactoEmergencia = contactoEmergencia;
    }

    public String getTelefonoEmergencia() {
        return telefonoEmergencia;
    }

    public void setTelefonoEmergencia(String telefonoEmergencia) {
        this.telefonoEmergencia = telefonoEmergencia;
    }

    // ============================================
    // CONTROL ADMINISTRATIVO
    // ============================================

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Instant getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Instant fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    // ============================================
    // NOMBRE COMPLETO
    // ============================================

    public String getNombreCompleto() {

        String nombre = nombres == null
                ? ""
                : nombres.trim();

        String apellido = apellidos == null
                ? ""
                : apellidos.trim();

        return (nombre + " " + apellido).trim();
    }
}
