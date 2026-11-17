package com.project.flowsense.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.flowsense.dto.mqtt.AlertaMqtt;
import com.project.flowsense.dto.mqtt.LeituraMqtt;
import com.project.flowsense.entity.HistoricoResumido;
import com.project.flowsense.entity.LeituraAtual;
import com.project.flowsense.entity.TipoEvento;
import com.project.flowsense.exception.PayloadInvalidoException;
import com.project.flowsense.repository.HistoricoResumidoRepository;
import com.project.flowsense.repository.LeituraAtualRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Processa as mensagens que o ESP32 publica (leituras, alertas e status) e grava no banco.
 * Toda mensagem é validada antes; se for inválida, lança PayloadInvalidoException e nada é gravado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeituraService {

    private final LeituraAtualRepository leituraAtualRepository;
    private final HistoricoResumidoRepository historicoRepository;
    private final EventoService eventoService;
    private final Clock relogio;

    /** flowsense/{id}/leituras: atualiza a leitura atual e soma a leitura no minuto correspondente do histórico. */
    @Transactional
    public void registrarLeitura(String dispositivo, LeituraMqtt msg) {
        ValidadorPayload.validarLeitura(dispositivo, msg);

        Instant agora = relogio.instant();
        Instant momento = ValidadorPayload.resolverHorario(msg.timestamp(), agora);

        LeituraAtual atual = leituraAtualRepository.findById(dispositivo)
                .orElseGet(() -> new LeituraAtual(dispositivo));

        // Consumo desde a leitura anterior (trata o reinício do ESP32, quando o kWh volta a 0).
        double consumo = CalculadoraConsumo.consumoEntre(atual.getKwh(), msg.kwh());
        boolean estavaOnline = atual.isOnline();

        atual.setTensao(msg.tensao());
        atual.setCorrente(msg.corrente());
        atual.setPotencia(msg.potencia()); // usamos a potência do ESP32: com relé OFF ela vem 0
        atual.setKwh(msg.kwh());
        atual.setRele(msg.rele());
        if (msg.alerta() != null) {
            atual.setAlerta(msg.alerta());
        }
        if (msg.limites() != null) {
            atual.setTensaoMin(msg.limites().tensaoMin());
            atual.setTensaoMax(msg.limites().tensaoMax());
            atual.setCorrenteMax(msg.limites().correnteMax());
        }
        if (msg.intervalo() != null) {
            atual.setIntervaloMs(msg.intervalo());
        }
        atual.setOnline(true);
        atual.setMomentoLeitura(momento);
        atual.setRecebidoEm(agora);
        leituraAtualRepository.save(atual);

        if (!estavaOnline) {
            eventoService.registrar(dispositivo, TipoEvento.ONLINE, "Dispositivo enviando leituras", agora);
        }

        // Histórico por minuto: uma linha por dispositivo e minuto, atualizada a cada leitura.
        Instant minuto = momento.truncatedTo(ChronoUnit.MINUTES);
        HistoricoResumido historico = historicoRepository.findByDispositivoAndMinuto(dispositivo, minuto)
                .orElseGet(() -> new HistoricoResumido(dispositivo, minuto));
        historico.adicionarAmostra(msg.tensao(), msg.corrente(), msg.potencia(), consumo);
        historicoRepository.save(historico);

        log.debug("Leitura de {}: {} V, {} A, {} W, relé {}", dispositivo,
                msg.tensao(), msg.corrente(), msg.potencia(), msg.rele());
    }

    /** flowsense/{id}/alertas: o ESP32 só publica quando o alerta muda, então cada mensagem vira um evento. */
    @Transactional
    public void registrarAlerta(String dispositivo, AlertaMqtt msg) {
        ValidadorPayload.validarAlerta(dispositivo, msg);
        Instant momento = ValidadorPayload.resolverHorario(msg.timestamp(), relogio.instant());

        TipoEvento tipo = switch (msg.tipo()) {
            case "SOBRETENSAO" -> TipoEvento.SOBRETENSAO;
            case "SUBTENSAO" -> TipoEvento.SUBTENSAO;
            case "SOBRECORRENTE" -> TipoEvento.SOBRECORRENTE;
            default -> TipoEvento.ALERTA_ENCERRADO; // "NORMAL"
        };
        String descricao = switch (tipo) {
            case SOBRETENSAO -> "Tensão acima do limite";
            case SUBTENSAO -> "Tensão abaixo do limite";
            case SOBRECORRENTE -> "Corrente acima do limite";
            default -> "Valores voltaram ao normal";
        };
        if ("OFF".equals(msg.rele()) && tipo != TipoEvento.ALERTA_ENCERRADO) {
            descricao += "; relé desligado pela proteção";
        }
        eventoService.registrar(dispositivo, tipo, descricao, momento, msg.tensao(), msg.corrente(), msg.rele());

        // Mantém a leitura atual coerente até a próxima leitura chegar.
        leituraAtualRepository.findById(dispositivo).ifPresent(atual -> {
            atual.setAlerta(msg.tipo());
            leituraAtualRepository.save(atual);
        });
    }

    /**
     * flowsense/{id}/status: "online" (publicado pelo ESP32 ao conectar) ou "offline"
     * (publicado pelo broker quando o ESP32 cai, a "última vontade"). Só registra evento quando o estado muda.
     */
    @Transactional
    public void registrarStatus(String dispositivo, String texto) {
        String status = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        if (!status.equals("online") && !status.equals("offline")) {
            throw new PayloadInvalidoException("status deve ser online ou offline, veio: " + texto);
        }
        boolean online = status.equals("online");
        Instant agora = relogio.instant();

        Optional<LeituraAtual> existente = leituraAtualRepository.findById(dispositivo);
        if (existente.isPresent() && existente.get().isOnline() == online) {
            return; // estado não mudou
        }
        if (existente.isEmpty() && !online) {
            return; // dispositivo nunca visto e offline: nada a registrar
        }

        LeituraAtual atual = existente.orElseGet(() -> {
            LeituraAtual nova = new LeituraAtual(dispositivo);
            nova.setRecebidoEm(agora); // ainda sem leituras; os valores ficam vazios
            return nova;
        });
        atual.setOnline(online);
        leituraAtualRepository.save(atual);
        eventoService.registrar(dispositivo, online ? TipoEvento.ONLINE : TipoEvento.OFFLINE,
                online ? "Dispositivo conectado ao broker" : "Dispositivo desconectado do broker", agora);
    }
}
