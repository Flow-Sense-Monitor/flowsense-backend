package com.project.flowsense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CalculadoraConsumoTest {

    private static final double DELTA = 1e-9;

    @Test
    void primeiraLeituraNaoGeraConsumo() {
        assertEquals(0, CalculadoraConsumo.consumoEntre(null, 0.5), DELTA);
    }

    @Test
    void consumoEhADiferencaEntreLeituras() {
        assertEquals(0.002, CalculadoraConsumo.consumoEntre(0.010, 0.012), DELTA);
    }

    @Test
    void reinicioDoEsp32NaoGeraConsumoNegativo() {
        // Antes do reinício estava em 0.500 kWh; reiniciou e já mediu 0.001 kWh desde então.
        assertEquals(0.001, CalculadoraConsumo.consumoEntre(0.500, 0.001), DELTA);
    }

    @Test
    void sequenciaComReinicioSomaSoAsPartesPositivas() {
        double[] leituras = { 0.100, 0.150, 0.200, 0.010, 0.030 }; // reinício entre 0.200 e 0.010
        Double anterior = null;
        double total = 0;
        for (double kwh : leituras) {
            total += CalculadoraConsumo.consumoEntre(anterior, kwh);
            anterior = kwh;
        }
        // 0.05 + 0.05 + 0.01 (desde o reinício) + 0.02
        assertEquals(0.13, total, DELTA);
    }
}
