package com.mycompany.clinica.conversion;

import java.text.DecimalFormat;

public abstract class ConversionBase {
    protected final String tipoConversion;
    protected final double valorEntrada;
    protected double valorSalida;

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    protected ConversionBase(String tipoConversion, double valorEntrada) {
        this.tipoConversion = tipoConversion;
        this.valorEntrada = valorEntrada;
    }

    public abstract void convertir();

    public double getValorSalida() { return valorSalida; }
    public String getResultado()   { return "Resultado: " + DF.format(valorSalida); }
}
