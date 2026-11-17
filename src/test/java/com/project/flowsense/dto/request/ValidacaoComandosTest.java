package com.project.flowsense.dto.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** As regras de validação dos comandos precisam ser as mesmas do firmware. */
class ValidacaoComandosTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void criarValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        fabrica.close();
    }

    private boolean valido(Object dto) {
        return validador.validate(dto).isEmpty();
    }

    // ---------------- limites: 0 < tensaoMin < tensaoMax <= 300 e 0 < correnteMax <= 30 ----------------

    @Test
    void limitesValidos() {
        assertTrue(valido(new LimitesRequest(200.0, 240.0, 15.0)));
        assertTrue(valido(new LimitesRequest(0.1, 300.0, 30.0))); // extremos permitidos
    }

    @Test
    void tensaoMinimaPrecisaSerMenorQueAMaxima() {
        assertFalse(valido(new LimitesRequest(240.0, 240.0, 15.0)));
        assertFalse(valido(new LimitesRequest(250.0, 240.0, 15.0)));
    }

    @Test
    void tensaoForaDaFaixa() {
        assertFalse(valido(new LimitesRequest(0.0, 240.0, 15.0)));
        assertFalse(valido(new LimitesRequest(200.0, 300.1, 15.0)));
    }

    @Test
    void correnteForaDaFaixa() {
        assertFalse(valido(new LimitesRequest(200.0, 240.0, 0.0)));
        assertFalse(valido(new LimitesRequest(200.0, 240.0, 30.5)));
    }

    @Test
    void limitesIncompletos() {
        assertFalse(valido(new LimitesRequest(null, 240.0, 15.0)));
        assertFalse(valido(new LimitesRequest(200.0, 240.0, null)));
    }

    // ---------------- intervalo: 500 a 60000 ms ----------------

    @Test
    void intervalo() {
        assertTrue(valido(new IntervaloRequest(500)));
        assertTrue(valido(new IntervaloRequest(60000)));
        assertFalse(valido(new IntervaloRequest(499)));
        assertFalse(valido(new IntervaloRequest(60001)));
        assertFalse(valido(new IntervaloRequest(null)));
    }

    // ---------------- relé ----------------

    @Test
    void rele() {
        assertTrue(valido(new ReleRequest("ON")));
        assertTrue(valido(new ReleRequest("OFF")));
        assertFalse(valido(new ReleRequest("on")));
        assertFalse(valido(new ReleRequest(null)));
    }
}
