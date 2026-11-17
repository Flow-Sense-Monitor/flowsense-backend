package com.project.flowsense.mqtt;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.project.flowsense.config.FlowsenseProperties;
import com.project.flowsense.dto.mqtt.AlertaMqtt;
import com.project.flowsense.dto.mqtt.LeituraMqtt;
import com.project.flowsense.exception.PayloadInvalidoException;
import com.project.flowsense.service.LeituraService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Decide o que fazer com cada mensagem recebida, a partir do tópico flowsense/{id}/{tipo}.
 * Mensagens inválidas são registradas em log e descartadas; nunca derrubam o backend.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttRoteador {

    /** As mensagens do ESP32 têm ~300 bytes; algo muito maior não é nosso. */
    static final int TAMANHO_MAXIMO_BYTES = 2048;

    private final FlowsenseProperties propriedades;
    private final LeituraService leituraService;
    private final ObjectMapper objectMapper;

    /** @return true se a mensagem foi aceita e gravada; false se foi descartada */
    public boolean processar(String topico, byte[] payload) {
        String[] partes = topico.split("/");
        if (partes.length != 3 || !partes[0].equals("flowsense")) {
            log.debug("Tópico ignorado: {}", topico);
            return false;
        }
        String dispositivo = partes[1];
        String tipo = partes[2];

        // O broker é público: só aceitamos os dispositivos configurados em FLOWSENSE_DISPOSITIVOS.
        if (!propriedades.dispositivoPermitido(dispositivo)) {
            log.debug("Mensagem de dispositivo desconhecido ignorada: {}", dispositivo);
            return false;
        }
        if (payload == null || payload.length == 0 || payload.length > TAMANHO_MAXIMO_BYTES) {
            log.warn("Mensagem descartada de {}: tamanho inválido ({} bytes)", topico,
                    payload == null ? 0 : payload.length);
            return false;
        }

        try {
            switch (tipo) {
                case "leituras" -> leituraService.registrarLeitura(dispositivo,
                        objectMapper.readValue(payload, LeituraMqtt.class));
                case "alertas" -> leituraService.registrarAlerta(dispositivo,
                        objectMapper.readValue(payload, AlertaMqtt.class));
                case "status" -> leituraService.registrarStatus(dispositivo,
                        new String(payload, StandardCharsets.UTF_8));
                default -> {
                    log.debug("Tópico ignorado: {}", topico);
                    return false;
                }
            }
            return true;
        } catch (JacksonException e) {
            log.warn("Mensagem descartada de {}: JSON inválido ({})", topico, e.getOriginalMessage());
        } catch (PayloadInvalidoException e) {
            log.warn("Mensagem descartada de {}: {}", topico, e.getMessage());
        }
        return false;
    }
}
