package com.project.flowsense.dto.response;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Formato padrão de erro da API. Ex.:
 * {"status":400,"erro":"Bad Request","mensagem":"Dados inválidos","campos":{"tensaoMax":"..."},"momento":"..."}
 */
public record ErroResponse(int status, String erro, String mensagem, Map<String, String> campos, Instant momento) {

    public static ErroResponse de(HttpStatus status, String mensagem) {
        return new ErroResponse(status.value(), status.getReasonPhrase(), mensagem, null, Instant.now());
    }

    public static ErroResponse de(HttpStatus status, String mensagem, Map<String, String> campos) {
        return new ErroResponse(status.value(), status.getReasonPhrase(), mensagem, campos, Instant.now());
    }
}
