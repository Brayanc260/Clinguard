package com.mycompany.clinica.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ValidacionDosisRequest(
        @NotBlank String pacienteId,
        @NotBlank String medicamentoId,
        @Positive double dosisRecetada // la cantidad de mg que el médico quiere dar
) {
}