package com.mycompany.clinica.dto;

public class ConversionRequest {
    
    // Campos para la conversión normal
    private String categoria;
    private String tipoConversion;
    private Double valor;

    // Campos para la validación clínica (Paciente + Medicamento)
    private String pacienteId;
    private String medicamentoId;
    private Double dosisRecetada;

    public ConversionRequest() {}

    // Getters y Setters
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getTipoConversion() { return tipoConversion; }
    public void setTipoConversion(String tipoConversion) { this.tipoConversion = tipoConversion; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public String getPacienteId() { return pacienteId; }
    public void setPacienteId(String pacienteId) { this.pacienteId = pacienteId; }

    public String getMedicamentoId() { return medicamentoId; }
    public void setMedicamentoId(String medicamentoId) { this.medicamentoId = medicamentoId; }

    public Double getDosisRecetada() { return dosisRecetada; }
    public void setDosisRecetada(Double dosisRecetada) { this.dosisRecetada = dosisRecetada; }
}