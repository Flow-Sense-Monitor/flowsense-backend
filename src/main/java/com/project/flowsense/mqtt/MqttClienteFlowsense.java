package com.project.flowsense.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.project.flowsense.config.FlowsenseProperties;
import com.project.flowsense.exception.MqttIndisponivelException;

import lombok.extern.slf4j.Slf4j;

/**
 * Conexão do backend com o broker MQTT (Eclipse Paho).
 *
 * - Assina flowsense/+/leituras, flowsense/+/alertas e flowsense/+/status ("+" = qualquer dispositivo).
 * - Se a primeira conexão falhar (broker fora, sem internet), tenta de novo a cada 5 s, sem travar a aplicação.
 * - Depois de conectado, o próprio Paho reconecta sozinho se a conexão cair; ao reconectar, assinamos de novo.
 * - O Paho não permite chamar métodos que esperam o broker (subscribe, publish) dentro dos seus callbacks,
 *   por isso conectar e assinar rodam numa thread própria ("tarefas").
 */
@Slf4j
@Component
public class MqttClienteFlowsense implements MqttCallbackExtended, SmartLifecycle {

    private static final String[] TOPICOS = {
            "flowsense/+/leituras",
            "flowsense/+/alertas",
            "flowsense/+/status" };
    private static final int[] QOS = { 0, 1, 1 };
    private static final int ESPERA_ENTRE_TENTATIVAS_SEGUNDOS = 5;

    private final FlowsenseProperties.Mqtt config;
    private final MqttRoteador roteador;
    private final ScheduledExecutorService tarefas = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mqtt-tarefas");
        t.setDaemon(true);
        return t;
    });

    private MqttClient cliente;
    private volatile boolean rodando;

    public MqttClienteFlowsense(FlowsenseProperties propriedades, MqttRoteador roteador) {
        this.config = propriedades.mqtt();
        this.roteador = roteador;
    }

    // ---------------- Ciclo de vida (o Spring chama start ao subir e stop ao desligar) ----------------

    @Override
    public void start() {
        try {
            // ID aleatório: dois backends com o mesmo ID no broker público derrubariam um ao outro.
            String clientId = "flowsense-backend-" + UUID.randomUUID().toString().substring(0, 8);
            cliente = new MqttClient(config.uri(), clientId, new MemoryPersistence());
            cliente.setCallback(this);
        } catch (MqttException e) {
            throw new IllegalStateException("Configuração MQTT inválida (" + config.uri() + ")", e);
        }
        rodando = true;
        tarefas.execute(this::conectar);
    }

    @Override
    public void stop() {
        rodando = false;
        tarefas.shutdownNow();
        try {
            if (cliente != null && cliente.isConnected()) {
                cliente.disconnect(2000);
            }
            if (cliente != null) {
                cliente.close();
            }
        } catch (MqttException e) {
            log.debug("Erro ao desconectar do broker: {}", e.getMessage());
        }
    }

    @Override
    public boolean isRunning() {
        return rodando;
    }

    // ---------------- Conexão ----------------

    private void conectar() {
        if (!rodando) {
            return;
        }
        try {
            log.info("Conectando ao broker MQTT {} ...", config.uri());
            cliente.connect(opcoesDeConexao());
            // As assinaturas são feitas em connectComplete(), que o Paho chama logo após conectar.
        } catch (MqttException e) {
            log.warn("Falha ao conectar no broker MQTT ({}). Nova tentativa em {} s.",
                    e.getMessage(), ESPERA_ENTRE_TENTATIVAS_SEGUNDOS);
            tarefas.schedule(this::conectar, ESPERA_ENTRE_TENTATIVAS_SEGUNDOS, TimeUnit.SECONDS);
        }
    }

    private MqttConnectOptions opcoesDeConexao() {
        MqttConnectOptions opcoes = new MqttConnectOptions();
        opcoes.setAutomaticReconnect(true);   // reconecta sozinho após a primeira conexão
        opcoes.setMaxReconnectDelay(30_000);  // espera no máximo 30 s entre tentativas
        opcoes.setCleanSession(true);
        opcoes.setConnectionTimeout(10);
        opcoes.setKeepAliveInterval(30);
        if (config.temAutenticacao()) {
            opcoes.setUserName(config.usuario());
            if (config.senha() != null) {
                opcoes.setPassword(config.senha().toCharArray());
            }
        }
        // TLS: com MQTT_TLS=true a URI vira ssl://... e o Paho usa a conexão segura padrão do Java.
        return opcoes;
    }

    private void assinarTopicos() {
        try {
            cliente.subscribe(TOPICOS, QOS);
            log.info("Assinando {}", String.join(", ", TOPICOS));
        } catch (MqttException e) {
            log.error("Falha ao assinar os tópicos: {}", e.getMessage());
        }
    }

    // ---------------- Publicação (usada pelo ComandoService) ----------------

    /** Publica um comando com QoS 1 (o broker confirma o recebimento). Não é retido. */
    public void publicar(String topico, String payload) {
        if (cliente == null || !cliente.isConnected()) {
            throw new MqttIndisponivelException("Sem conexão com o broker MQTT; tente novamente em instantes.");
        }
        try {
            MqttMessage mensagem = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            mensagem.setQos(1);
            mensagem.setRetained(false);
            cliente.publish(topico, mensagem);
            log.info("Comando publicado em {}: {}", topico, payload);
        } catch (MqttException e) {
            throw new MqttIndisponivelException("Falha ao publicar o comando no broker MQTT.", e);
        }
    }

    public boolean conectado() {
        return cliente != null && cliente.isConnected();
    }

    // ---------------- Callbacks do Paho ----------------

    @Override
    public void connectComplete(boolean reconexao, String uri) {
        log.info("{} ao broker MQTT {}", reconexao ? "Reconectado" : "Conectado", uri);
        tarefas.execute(this::assinarTopicos);
    }

    @Override
    public void connectionLost(Throwable causa) {
        log.warn("Conexão com o broker MQTT perdida ({}). Reconectando automaticamente...",
                causa != null ? causa.getMessage() : "sem detalhe");
    }

    @Override
    public void messageArrived(String topico, MqttMessage mensagem) {
        // Nunca deixar exceção escapar daqui: o Paho derrubaria a conexão.
        try {
            roteador.processar(topico, mensagem.getPayload());
        } catch (Exception e) {
            log.error("Erro ao processar mensagem de {}: {}", topico, e.getMessage(), e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // nada a fazer: publicar() já registra o envio
    }
}
