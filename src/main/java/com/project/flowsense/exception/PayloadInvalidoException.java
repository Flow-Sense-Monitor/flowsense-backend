package com.project.flowsense.exception;

/** Mensagem MQTT malformada ou com valores fora da faixa. É registrada em log e descartada. */
public class PayloadInvalidoException extends RuntimeException {

    public PayloadInvalidoException(String mensagem) {
        super(mensagem);
    }
}
