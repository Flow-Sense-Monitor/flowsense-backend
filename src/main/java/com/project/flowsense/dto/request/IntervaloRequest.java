package com.project.flowsense.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Corpo de PUT /api/dispositivos/{id}/intervalo. Ex.: {"intervalo":5000} (milissegundos) */
public record IntervaloRequest(
        @NotNull(message = "informe o intervalo em milissegundos")
        @Min(value = 500, message = "o intervalo deve ser de pelo menos 500 ms")
        @Max(value = 60000, message = "o intervalo deve ser de no máximo 60000 ms")
        Integer intervalo) {
}
