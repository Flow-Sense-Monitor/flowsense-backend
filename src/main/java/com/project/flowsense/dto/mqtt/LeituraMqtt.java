package com.project.flowsense.dto.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mensagem publicada pelo ESP32 em flowsense/{id}/leituras. Exemplo real:
 * {"dispositivo":"esp32-01","tensao":221.30,"corrente":5.42,"potencia":1199.45,"kwh":0.00213,
 *  "rele":"ON","alerta":"NORMAL","limites":{"tensaoMin":198,"tensaoMax":242,"correnteMax":20},
 *  "intervalo":2000,"timestamp":1794445800}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeituraMqtt(
        String dispositivo,
        Double tensao,
        Double corrente,
        Double potencia,
        Double kwh,
        String rele,
        String alerta,
        LimitesMqtt limites,
        Integer intervalo,
        Long timestamp) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LimitesMqtt(Double tensaoMin, Double tensaoMax, Double correnteMax) {
    }
}
