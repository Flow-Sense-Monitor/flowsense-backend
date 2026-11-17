package com.project.flowsense.dto.response;

import java.time.Instant;

/** Resposta de GET /api/dispositivos/{id}/consumo. */
public record ConsumoResponse(
        String dispositivo,
        Instant de,
        Instant ate,
        double consumoKwh,
        double tarifaKwh,
        double custoEstimado) {
}
