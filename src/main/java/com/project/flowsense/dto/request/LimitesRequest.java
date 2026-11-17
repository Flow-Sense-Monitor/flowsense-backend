package com.project.flowsense.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Corpo de PUT /api/dispositivos/{id}/limites.
 * As mesmas regras do firmware: 0 < tensaoMin < tensaoMax <= 300 e 0 < correnteMax <= 30.
 */
public record LimitesRequest(
        @NotNull(message = "informe a tensão mínima")
        @Positive(message = "a tensão mínima deve ser maior que 0")
        Double tensaoMin,

        @NotNull(message = "informe a tensão máxima")
        @DecimalMax(value = "300", message = "a tensão máxima deve ser no máximo 300 V")
        Double tensaoMax,

        @NotNull(message = "informe a corrente máxima")
        @Positive(message = "a corrente máxima deve ser maior que 0")
        @DecimalMax(value = "30", message = "a corrente máxima deve ser no máximo 30 A")
        Double correnteMax) {

    @JsonIgnore
    @AssertTrue(message = "a tensão mínima deve ser menor que a tensão máxima")
    public boolean isFaixaDeTensaoValida() {
        // Se algum valor faltar, as anotações @NotNull acima já apontam o erro.
        return tensaoMin == null || tensaoMax == null || tensaoMin < tensaoMax;
    }
}
