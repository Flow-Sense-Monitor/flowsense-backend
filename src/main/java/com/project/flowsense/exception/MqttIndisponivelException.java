package com.project.flowsense.exception;

/** O backend está sem conexão com o broker MQTT e não consegue enviar o comando. Vira HTTP 503. */
public class MqttIndisponivelException extends RuntimeException {

    public MqttIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public MqttIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
