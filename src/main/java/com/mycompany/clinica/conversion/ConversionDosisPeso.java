package com.mycompany.clinica.conversion;

public final class ConversionDosisPeso extends ConversionBase {
    public ConversionDosisPeso(String tipo, double valor) { super(tipo, valor); }

    @Override
    public void convertir() {
        valorSalida = switch (tipoConversion) {
            case "Gramos a Miligramos"             -> valorEntrada * 1000;
            case "Miligramos a Gramos"             -> valorEntrada / 1000;
            case "Miligramos a Microgramos"        -> valorEntrada * 1000;
            case "Kilogramos a Gramos"             -> valorEntrada * 1000;
            case "Calcular Dosis por Peso (mg/kg)" -> valorEntrada * 2;
            default -> throw new IllegalArgumentException("Conversión no soportada: " + tipoConversion);
        };
    }
}
