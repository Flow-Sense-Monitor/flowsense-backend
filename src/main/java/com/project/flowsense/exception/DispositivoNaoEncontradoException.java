package com.project.flowsense.exception;

/** O dispositivo não está na lista de permitidos ou ainda não enviou nenhuma mensagem. Vira HTTP 404. */
public class DispositivoNaoEncontradoException extends RuntimeException {

    public DispositivoNaoEncontradoException(String id) {
        super("Dispositivo '" + id + "' não encontrado ou ainda sem leituras.");
    }
}
