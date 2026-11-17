package com.project.flowsense.dto.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mensagem publicada pelo ESP32 em flowsense/{id}/alertas quando o estado de alerta muda. Exemplo real:
 * {"dispositivo":"esp32-01","tipo":"SUBTENSAO","tensao":192.09,"corrente":19.53,"rele":"OFF","timestamp":1794529491}
 * O tipo "NORMAL" indica que o alerta terminou.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertaMqtt(
        String dispositivo,
        String tipo,
        Double tensao,
        Double corrente,
        String rele,
        Long timestamp) {
}
