package com.mycompany.clinica.dto;

public record ConversionResponse(
    String tipoConversion,
    double valorEntrada,
    double valorSalida,
    String resultado
) {}
