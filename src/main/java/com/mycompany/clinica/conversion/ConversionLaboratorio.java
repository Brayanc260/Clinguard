package com.mycompany.clinica.conversion;

public final class ConversionLaboratorio extends ConversionBase {
    public ConversionLaboratorio(String tipo, double valor) { super(tipo, valor); }

    @Override
    public void convertir() {
        valorSalida = switch (tipoConversion) {
            case "Glucosa mmol/L a mg/dL"    -> valorEntrada * 18.0182;
            case "Glucosa mg/dL a mmol/L"    -> valorEntrada / 18.0182;
            case "UI a Miligramos"            -> valorEntrada / 40.0;
            case "Colesterol mmol/L a mg/dL" -> valorEntrada * 38.67;
            case "Creatinina mg/dL a µmol/L" -> valorEntrada * 88.4;
            default -> throw new IllegalArgumentException("Conversión no soportada: " + tipoConversion);
        };
    }
}
