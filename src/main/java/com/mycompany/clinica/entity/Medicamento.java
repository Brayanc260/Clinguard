package com.mycompany.clinica.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "medicamentos")
public class Medicamento {

    @Id
    private String id;

    private String nombre;
    private String principioActivo;
    private String presentacion;

    private Double cantidadMedicamento;
    private String unidadMedicamento;

    private Double cantidadVehiculo;
    private String unidadVehiculo;

    private String viaAdministracion;

    private Integer stock;

    private String estado;

    private Double dosisMinimaMgKg;
    private Double dosisMaximaMgKg;
    private String descripcion;

    public Medicamento() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPrincipioActivo() {
        return principioActivo;
    }

    public void setPrincipioActivo(String principioActivo) {
        this.principioActivo = principioActivo;
    }

    public String getPresentacion() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion = presentacion;
    }

    public Double getCantidadMedicamento() {
        return cantidadMedicamento;
    }

    public void setCantidadMedicamento(Double cantidadMedicamento) {
        this.cantidadMedicamento = cantidadMedicamento;
    }

    public String getUnidadMedicamento() {
        return unidadMedicamento;
    }

    public void setUnidadMedicamento(String unidadMedicamento) {
        this.unidadMedicamento = unidadMedicamento;
    }

    public Double getCantidadVehiculo() {
        return cantidadVehiculo;
    }

    public void setCantidadVehiculo(Double cantidadVehiculo) {
        this.cantidadVehiculo = cantidadVehiculo;
    }

    public String getUnidadVehiculo() {
        return unidadVehiculo;
    }

    public void setUnidadVehiculo(String unidadVehiculo) {
        this.unidadVehiculo = unidadVehiculo;
    }

    public String getViaAdministracion() {
        return viaAdministracion;
    }

    public void setViaAdministracion(String viaAdministracion) {
        this.viaAdministracion = viaAdministracion;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Double getDosisMinimaMgKg() {
        return dosisMinimaMgKg;
    }

    public void setDosisMinimaMgKg(Double dosisMinimaMgKg) {
        this.dosisMinimaMgKg = dosisMinimaMgKg;
    }

    public Double getDosisMaximaMgKg() {
        return dosisMaximaMgKg;
    }

    public void setDosisMaximaMgKg(Double dosisMaximaMgKg) {
        this.dosisMaximaMgKg = dosisMaximaMgKg;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
