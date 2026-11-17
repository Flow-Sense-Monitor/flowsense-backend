package com.project.flowsense.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações do FlowSense lidas do application.properties (que por sua vez lê variáveis de ambiente).
 *
 * @param dispositivos IDs de ESP32 aceitos; mensagens de outros IDs são ignoradas (o broker é público)
 * @param apiKey       chave exigida no cabeçalho X-API-Key para enviar comandos
 * @param tarifaKwh    tarifa de energia em R$/kWh, usada no custo estimado
 * @param corsOrigens  origens do navegador autorizadas a chamar a API
 * @param retencaoDias quantos dias de histórico e eventos são mantidos
 * @param mqtt         conexão com o broker MQTT
 */
@ConfigurationProperties(prefix = "flowsense")
public record FlowsenseProperties(
        List<String> dispositivos,
        String apiKey,
        double tarifaKwh,
        List<String> corsOrigens,
        int retencaoDias,
        Mqtt mqtt) {

    public record Mqtt(String host, int porta, String usuario, String senha, boolean tls) {

        /** Ex.: tcp://broker.hivemq.com:1883 ou ssl://meu-broker:8883 */
        public String uri() {
            return (tls ? "ssl://" : "tcp://") + host + ":" + porta;
        }

        public boolean temAutenticacao() {
            return usuario != null && !usuario.isBlank();
        }
    }

    public boolean dispositivoPermitido(String id) {
        return dispositivos != null && dispositivos.contains(id);
    }

    public boolean apiKeyConfigurada() {
        return apiKey != null && !apiKey.isBlank();
    }
}
