package com.mycompany.clinica.dto;

public class LoginRequest {
    private String nombreUsuario;
    private String password;

    // Constructor vacío necesario para Spring
    public LoginRequest() {}

    // Getters
    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getPassword() {
        return password;
    }

    // Setters
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}