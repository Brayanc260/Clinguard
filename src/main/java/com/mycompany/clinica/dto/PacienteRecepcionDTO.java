
package com.mycompany.clinica.dto;

public class PacienteRecepcionDTO {

    // =====================================================
    // DATOS ADMINISTRATIVOS DEL PACIENTE
    // =====================================================

    private String id;
    private String documento;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String correo;
    private boolean activo;

    // =====================================================
    // CONSTRUCTOR VACÃO
    // =====================================================

    public PacienteRecepcionDTO() {
    }

    // =====================================================
    // CONSTRUCTOR COMPLETO
    // =====================================================

    public PacienteRecepcionDTO(
            String id,
            String documento,
            String nombres,
            String apellidos,
            String telefono,
            String correo,
            boolean activo) {

        this.id = id;
        this.documento = documento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.correo = correo;
        this.activo = activo;
    }

    // =====================================================
    // ID
    // =====================================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // =====================================================
    // DOCUMENTO
    // =====================================================

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    // =====================================================
    // NOMBRES
    // =====================================================

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    // =====================================================
    // APELLIDOS
    // =====================================================

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    // =====================================================
    // TELÃ‰FONO
    // =====================================================

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    // =====================================================
    // CORREO
    // =====================================================

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    // =====================================================
    // ESTADO DEL PACIENTE
    // =====================================================

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    // =====================================================
    // NOMBRE COMPLETO
    // =====================================================

    public String getNombreCompleto() {

        String nombre = nombres != null ? nombres : "";
        String apellido = apellidos != null ? apellidos : "";

        return (nombre + " " + apellido).trim();
    }
}

