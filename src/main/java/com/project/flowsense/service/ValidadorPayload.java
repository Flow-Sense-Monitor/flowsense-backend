package com.project.flowsense.service;

import java.time.Instant;
import java.util.Set;

import com.project.flowsense.dto.mqtt.AlertaMqtt;
import com.project.flowsense.dto.mqtt.LeituraMqtt;
import com.project.flowsense.exception.PayloadInvalidoException;

/**
 * Regras de validação das mensagens MQTT. O broker é público: qualquer um pode publicar
 * nos nossos tópicos, então nada é gravado sem passar por aqui.
 */
public final class ValidadorPayload {

    public static final Set<String> TIPOS_ALERTA = Set.of("NORMAL", "SUBTENSAO", "SOBRETENSAO", "SOBRECORRENTE");
    private static final Set<String> ESTADOS_RELE = Set.of("ON", "OFF");

    /** Horários antes disso são considerados relógio não sincronizado. */
    private static final Instant HORARIO_MINIMO = Instant.parse("2024-01-01T00:00:00Z");
    /** Tolerância para relógio do ESP32 um pouco adiantado em relação ao servidor. */
    private static final long TOLERANCIA_FUTURO_SEGUNDOS = 300;

    private ValidadorPayload() {
    }

    public static void validarLeitura(String idTopico, LeituraMqtt l) {
        if (l == null) {
            throw new PayloadInvalidoException("leitura vazia");
        }
        validarMesmoDispositivo(idTopico, l.dispositivo());
        exigirFaixa("tensao", l.tensao(), 0, 400);
        exigirFaixa("corrente", l.corrente(), 0, 100);
        exigirFaixa("potencia", l.potencia(), 0, 40_000);
        exigirFaixa("kwh", l.kwh(), 0, 10_000_000);
        if (!ESTADOS_RELE.contains(l.rele())) {
            throw new PayloadInvalidoException("rele deve ser ON ou OFF, veio: " + l.rele());
        }
        if (l.alerta() != null && !TIPOS_ALERTA.contains(l.alerta())) {
            throw new PayloadInvalidoException("alerta desconhecido: " + l.alerta());
        }
        if (l.intervalo() != null && (l.intervalo() < 500 || l.intervalo() > 60_000)) {
            throw new PayloadInvalidoException("intervalo fora da faixa: " + l.intervalo());
        }
        // Limites são opcionais; se vierem, precisam fazer sentido.
        LeituraMqtt.LimitesMqtt lim = l.limites();
        if (lim != null) {
            exigirFaixa("limites.tensaoMin", lim.tensaoMin(), 0, 300);
            exigirFaixa("limites.tensaoMax", lim.tensaoMax(), 0, 300);
            exigirFaixa("limites.correnteMax", lim.correnteMax(), 0, 30);
        }
    }

    public static void validarAlerta(String idTopico, AlertaMqtt a) {
        if (a == null) {
            throw new PayloadInvalidoException("alerta vazio");
        }
        validarMesmoDispositivo(idTopico, a.dispositivo());
        if (!TIPOS_ALERTA.contains(a.tipo())) {
            throw new PayloadInvalidoException("tipo de alerta desconhecido: " + a.tipo());
        }
        // Valores do alerta são informativos; se vierem, precisam ser números válidos.
        if (a.tensao() != null) {
            exigirFaixa("tensao", a.tensao(), 0, 400);
        }
        if (a.corrente() != null) {
            exigirFaixa("corrente", a.corrente(), 0, 100);
        }
        if (a.rele() != null && !ESTADOS_RELE.contains(a.rele())) {
            throw new PayloadInvalidoException("rele deve ser ON ou OFF, veio: " + a.rele());
        }
    }

    /**
     * Converte o timestamp do ESP32 (Unix, segundos, UTC). O ESP32 manda 0 enquanto o relógio
     * não sincronizou pela internet; nesse caso, ou se o valor for absurdo, usamos o horário do servidor.
     */
    public static Instant resolverHorario(Long timestamp, Instant agora) {
        if (timestamp == null || timestamp <= 0) {
            return agora;
        }
        Instant informado = Instant.ofEpochSecond(timestamp);
        if (informado.isBefore(HORARIO_MINIMO) || informado.isAfter(agora.plusSeconds(TOLERANCIA_FUTURO_SEGUNDOS))) {
            return agora;
        }
        return informado;
    }

    private static void validarMesmoDispositivo(String idTopico, String idPayload) {
        // O campo "dispositivo" é opcional, mas se vier precisa bater com o tópico.
        if (idPayload != null && !idPayload.equals(idTopico)) {
            throw new PayloadInvalidoException("dispositivo do payload (" + idPayload
                    + ") diferente do tópico (" + idTopico + ")");
        }
    }

    private static void exigirFaixa(String campo, Double valor, double min, double max) {
        if (valor == null || valor.isNaN() || valor.isInfinite()) {
            throw new PayloadInvalidoException(campo + " ausente ou inválido");
        }
        if (valor < min || valor > max) {
            throw new PayloadInvalidoException(campo + " fora da faixa [" + min + ", " + max + "]: " + valor);
        }
    }
}
