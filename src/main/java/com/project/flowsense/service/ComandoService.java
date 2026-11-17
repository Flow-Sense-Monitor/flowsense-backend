package com.project.flowsense.service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.flowsense.dto.request.IntervaloRequest;
import com.project.flowsense.dto.request.LimitesRequest;
import com.project.flowsense.dto.request.ReleRequest;
import com.project.flowsense.dto.response.ComandoResponse;
import com.project.flowsense.entity.TipoEvento;
import com.project.flowsense.mqtt.MqttClienteFlowsense;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * Envia comandos ao ESP32 publicando em flowsense/{id}/comandos. Os formatos são os que o firmware aceita:
 * {"rele":"ON"}, {"limites":{"tensaoMin":..,"tensaoMax":..,"correnteMax":..}} e {"intervalo":5000}.
 */
@Service
@RequiredArgsConstructor
public class ComandoService {

    private final MqttClienteFlowsense mqtt;
    private final ConsultaService consultaService;
    private final EventoService eventoService;
    private final ObjectMapper objectMapper;
    private final Clock relogio;

    public ComandoResponse alterarRele(String id, ReleRequest req) {
        String estado = req.estado();
        String descricao = "Comando enviado: " + ("ON".equals(estado) ? "ligar a carga" : "desligar a carga");
        return enviar(id, Map.of("rele", estado), TipoEvento.COMANDO_RELE, descricao);
    }

    public ComandoResponse alterarLimites(String id, LimitesRequest req) {
        Map<String, Object> limites = new LinkedHashMap<>();
        limites.put("tensaoMin", req.tensaoMin());
        limites.put("tensaoMax", req.tensaoMax());
        limites.put("correnteMax", req.correnteMax());
        String descricao = String.format("Comando enviado: limites %.1f–%.1f V, até %.1f A",
                req.tensaoMin(), req.tensaoMax(), req.correnteMax());
        return enviar(id, Map.of("limites", limites), TipoEvento.COMANDO_LIMITES, descricao);
    }

    public ComandoResponse alterarIntervalo(String id, IntervaloRequest req) {
        String descricao = "Comando enviado: intervalo de envio " + req.intervalo() + " ms";
        return enviar(id, Map.of("intervalo", req.intervalo()), TipoEvento.COMANDO_INTERVALO, descricao);
    }

    private ComandoResponse enviar(String id, Map<String, Object> comando, TipoEvento tipo, String descricao) {
        consultaService.verificarDispositivo(id);
        String topico = "flowsense/" + id + "/comandos";
        String payload = objectMapper.writeValueAsString(comando);

        mqtt.publicar(topico, payload); // lança MqttIndisponivelException (HTTP 503) se o broker estiver fora

        Instant agora = relogio.instant();
        eventoService.registrar(id, tipo, descricao, agora);
        return new ComandoResponse(id, topico, payload, agora,
                "Comando publicado. O ESP32 confirma na próxima leitura.");
    }
}
