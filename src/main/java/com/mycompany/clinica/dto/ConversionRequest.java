package com.mycompany.clinica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ConversionRequest(
    @NotBlank String categoria,
    @NotBlank String tipoConversion,
    @Positive double valor
) {}
