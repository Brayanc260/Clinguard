package com.mycompany.clinica.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "registro_conversiones")
public class RegistroConversion {

    @Id
    private String id;

    private String usuarioNombre;
    private String tipoConversion;
    private Double valorEntrada;
    private Double valorSalida;
    private LocalDateTime fechaHora;

    public RegistroConversion() {}

    public String getId()                { return id; }
    public String getUsuarioNombre()     { return usuarioNombre; }
    public String getTipoConversion()    { return tipoConversion; }
    public Double getValorEntrada()      { return valorEntrada; }
    public Double getValorSalida()       { return valorSalida; }
    public LocalDateTime getFechaHora()  { return fechaHora; }

    public void setUsuarioNombre(String v)  { this.usuarioNombre = v; }
    public void setTipoConversion(String v) { this.tipoConversion = v; }
    public void setValorEntrada(Double v)   { this.valorEntrada = v; }
    public void setValorSalida(Double v)    { this.valorSalida = v; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
}