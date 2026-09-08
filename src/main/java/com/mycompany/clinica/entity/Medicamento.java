package com.mycompany.clinica.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Document(collection = "medicamentos")
public class Medicamento {

    @Id
    private String id;

    @NotBlank(message = "El nombre es obligatorio")
    @Indexed(unique = true)
    private String nombre;

    @NotNull(message = "La dosis mínima es obligatoria")
    private Double dosisMinimaMgKg;

    @NotNull(message = "La dosis máxima es obligatoria")
    private Double dosisMaximaMgKg;

    private String descripcion;

    public Medicamento() {}

    public Medicamento(String nombre, Double dosisMinimaMgKg, Double dosisMaximaMgKg, String descripcion) {
        this.nombre = nombre;
        this.dosisMinimaMgKg = dosisMinimaMgKg;
        this.dosisMaximaMgKg = dosisMaximaMgKg;
        this.descripcion = descripcion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Double getDosisMinimaMgKg() { return dosisMinimaMgKg; }
    public void setDosisMinimaMgKg(Double dosisMinimaMgKg) { this.dosisMinimaMgKg = dosisMinimaMgKg; }

    public Double getDosisMaximaMgKg() { return dosisMaximaMgKg; }
    public void setDosisMaximaMgKg(Double dosisMaximaMgKg) { this.dosisMaximaMgKg = dosisMaximaMgKg; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}