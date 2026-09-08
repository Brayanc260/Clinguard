package com.mycompany.clinica.conversion;

public final class ConversionVolumen extends ConversionBase {
    public ConversionVolumen(String tipo, double valor) { super(tipo, valor); }

    @Override
    public void convertir() {
        valorSalida = switch (tipoConversion) {
            case "Mililitros a Litros" -> valorEntrada / 1000;
            case "Litros a Mililitros" -> valorEntrada * 1000;
            case "Mililitros a Gotas"  -> valorEntrada * 20;
            case "Gotas a Mililitros"  -> valorEntrada / 20;
            case "CC a Mililitros"     -> valorEntrada;
            default -> throw new IllegalArgumentException("Conversión no soportada: " + tipoConversion);
        };
    }
}
