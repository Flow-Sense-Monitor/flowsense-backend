package com.project.flowsense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.flowsense.dto.response.PontoHistoricoResponse;
import com.project.flowsense.entity.HistoricoResumido;

class ConsultaServiceTest {

    @Test
    void tamanhoDoAgrupamentoPorPeriodo() {
        assertEquals(1, ConsultaService.tamanhoDoAgrupamento(Duration.ofHours(1)));   // 60 pontos
        assertEquals(5, ConsultaService.tamanhoDoAgrupamento(Duration.ofHours(24)));  // 288 pontos
        assertEquals(30, ConsultaService.tamanhoDoAgrupamento(Duration.ofDays(7)));   // 336 pontos
    }

    @Test
    void agruparFazMediaPonderadaESomaConsumo() {
        HistoricoResumido m1 = new HistoricoResumido("esp32-01", Instant.parse("2026-11-14T15:00:00Z"));
        m1.adicionarAmostra(220, 5, 1100, 0.01);
        HistoricoResumido m2 = new HistoricoResumido("esp32-01", Instant.parse("2026-11-14T15:01:00Z"));
        m2.adicionarAmostra(230, 10, 2300, 0.02);
        m2.adicionarAmostra(230, 10, 2300, 0.02);

        List<PontoHistoricoResponse> pontos = ConsultaService.agrupar(List.of(m1, m2), 5);

        assertEquals(1, pontos.size());
        PontoHistoricoResponse p = pontos.get(0);
        assertEquals(Instant.parse("2026-11-14T15:00:00Z"), p.inicio());
        assertEquals((220 + 230 + 230) / 3.0, p.tensaoMedia(), 1e-9); // ponderada pelas 3 amostras
        assertEquals(220, p.tensaoMin(), 1e-9);
        assertEquals(10, p.correnteMax(), 1e-9);
        assertEquals(0.05, p.consumoKwh(), 1e-9);
    }
}
