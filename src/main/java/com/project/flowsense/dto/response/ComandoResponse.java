package com.project.flowsense.dto.response;

import java.time.Instant;

/**
 * Resposta dos comandos (relé, limites, intervalo). O comando foi publicado no broker;
 * a confirmação de que o ESP32 obedeceu chega na próxima leitura.
 */
public record ComandoResponse(String dispositivo, String topico, String payload, Instant enviadoEm, String mensagem) {
}
