package com.project.flowsense.dto.response;

import java.time.Instant;

/**
 * Um ponto dos gráficos. Representa um intervalo de "minutos" minutos começando em "inicio"
 * (1 minuto para períodos curtos; mais minutos agrupados para períodos longos).
 */
public record PontoHistoricoResponse(
        Instant inicio,
        int minutos,
        double tensaoMedia,
        double tensaoMin,
        double tensaoMax,
        double correnteMedia,
        double correnteMax,
        double potenciaMedia,
        double potenciaMax,
        double consumoKwh) {
}
