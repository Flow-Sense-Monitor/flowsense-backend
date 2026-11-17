package com.project.flowsense.service;

/**
 * Calcula quanta energia foi consumida entre duas leituras.
 *
 * O ESP32 manda o kWh ACUMULADO desde que ligou, e esse valor volta a 0 quando ele reinicia.
 * Por isso o consumo entre duas leituras é a diferença entre elas, com dois cuidados:
 *  - sem leitura anterior (primeira mensagem do dispositivo), não há base: consumo 0;
 *  - se o valor diminuiu, o ESP32 reiniciou e começou do zero: o consumo desde o reinício é o próprio valor atual.
 * Assim nunca aparece consumo negativo.
 */
public final class CalculadoraConsumo {

    private CalculadoraConsumo() {
    }

    public static double consumoEntre(Double kwhAnterior, double kwhAtual) {
        if (kwhAnterior == null) {
            return 0;
        }
        if (kwhAtual < kwhAnterior) {
            return kwhAtual; // reinício do ESP32
        }
        return kwhAtual - kwhAnterior;
    }
}
