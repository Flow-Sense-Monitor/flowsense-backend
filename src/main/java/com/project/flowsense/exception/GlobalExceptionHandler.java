package com.project.flowsense.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.project.flowsense.dto.response.ErroResponse;

import lombok.extern.slf4j.Slf4j;

/** Converte as exceções em respostas JSON com o status HTTP correto. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Bean Validation falhou nos DTOs (@Valid). Devolve a mensagem de cada campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        e.getBindingResult().getGlobalErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getObjectName(), erro.getDefaultMessage()));
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos: " + String.join("; ", campos.values()), campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> jsonInvalido(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou com JSON inválido.", null);
    }

    @ExceptionHandler({ MethodArgumentTypeMismatchException.class, IllegalArgumentException.class })
    public ResponseEntity<ErroResponse> parametroInvalido(Exception e) {
        String mensagem = e instanceof MethodArgumentTypeMismatchException m
                ? "Parâmetro '" + m.getName() + "' com formato inválido."
                : e.getMessage();
        return resposta(HttpStatus.BAD_REQUEST, mensagem, null);
    }

    @ExceptionHandler(DispositivoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(DispositivoNaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, "Rota não encontrada.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException e) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método " + e.getMethod() + " não é aceito nesta rota.", null);
    }

    @ExceptionHandler(MqttIndisponivelException.class)
    public ResponseEntity<ErroResponse> mqttIndisponivel(MqttIndisponivelException e) {
        log.warn("Comando não enviado: {}", e.getMessage());
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), null);
    }

    /** Qualquer erro inesperado: registra o detalhe no log e devolve mensagem genérica (não expõe o stack trace). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> erroInesperado(Exception e) {
        log.error("Erro inesperado na API", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor.", null);
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem, Map<String, String> campos) {
        return ResponseEntity.status(status).body(ErroResponse.de(status, mensagem, campos));
    }
}
