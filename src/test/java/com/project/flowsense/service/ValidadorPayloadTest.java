package com.project.flowsense.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.project.flowsense.dto.mqtt.AlertaMqtt;
import com.project.flowsense.dto.mqtt.LeituraMqtt;
import com.project.flowsense.exception.PayloadInvalidoException;

class ValidadorPayloadTest {

    private static final Instant AGORA = Instant.parse("2026-11-14T15:00:00Z");

    static LeituraMqtt leitura(Double tensao, Double corrente, String rele) {
        return new LeituraMqtt("esp32-01", tensao, corrente, 1199.45, 0.00213, rele, "NORMAL",
                new LeituraMqtt.LimitesMqtt(198.0, 242.0, 20.0), 2000, 1794445800L);
    }

    // ---------------- timestamp ----------------

    @Test
    void timestampZeroUsaHorarioDoServidor() {
        assertEquals(AGORA, ValidadorPayload.resolverHorario(0L, AGORA));
    }

    @Test
    void timestampAusenteUsaHorarioDoServidor() {
        assertEquals(AGORA, ValidadorPayload.resolverHorario(null, AGORA));
    }

    @Test
    void timestampValidoEhUsado() {
        long umMinutoAntes = AGORA.minusSeconds(60).getEpochSecond();
        assertEquals(AGORA.minusSeconds(60), ValidadorPayload.resolverHorario(umMinutoAntes, AGORA));
    }

    @Test
    void timestampAbsurdoUsaHorarioDoServidor() {
        assertEquals(AGORA, ValidadorPayload.resolverHorario(5000L, AGORA));                 // 1970: relógio sem sincronizar
        assertEquals(AGORA, ValidadorPayload.resolverHorario(AGORA.getEpochSecond() + 86_400, AGORA)); // 1 dia no futuro
    }

    // ---------------- leituras ----------------

    @Test
    void leituraDoExemploDoFirmwareEhAceita() {
        assertDoesNotThrow(() -> ValidadorPayload.validarLeitura("esp32-01", leitura(221.3, 5.42, "ON")));
    }

    @Test
    void leituraComReleDesligadoEPotenciaZeroEhAceita() {
        LeituraMqtt l = new LeituraMqtt("esp32-01", 221.3, 5.42, 0.0, 0.1, "OFF", "SOBRECORRENTE", null, null, 0L);
        assertDoesNotThrow(() -> ValidadorPayload.validarLeitura("esp32-01", l));
    }

    @Test
    void leituraSemTensaoEhRecusada() {
        assertThrows(PayloadInvalidoException.class,
                () -> ValidadorPayload.validarLeitura("esp32-01", leitura(null, 5.0, "ON")));
    }

    @Test
    void leituraComValorForaDaFaixaEhRecusada() {
        assertThrows(PayloadInvalidoException.class,
                () -> ValidadorPayload.validarLeitura("esp32-01", leitura(221.0, -1.0, "ON")));
        assertThrows(PayloadInvalidoException.class,
                () -> ValidadorPayload.validarLeitura("esp32-01", leitura(9999.0, 5.0, "ON")));
    }

    @Test
    void leituraComReleInvalidoEhRecusada() {
        assertThrows(PayloadInvalidoException.class,
                () -> ValidadorPayload.validarLeitura("esp32-01", leitura(221.0, 5.0, "TALVEZ")));
    }

    @Test
    void leituraDeOutroDispositivoNoTopicoEhRecusada() {
        assertThrows(PayloadInvalidoException.class,
                () -> ValidadorPayload.validarLeitura("esp32-02", leitura(221.0, 5.0, "ON")));
    }

    @Test
    void leituraVaziaEhRecusada() {
        assertThrows(PayloadInvalidoException.class, () -> ValidadorPayload.validarLeitura("esp32-01", null));
    }

    // ---------------- alertas ----------------

    @Test
    void alertaDoExemploDoFirmwareEhAceito() {
        AlertaMqtt a = new AlertaMqtt("esp32-01", "SUBTENSAO", 192.09, 19.53, "OFF", 1794529491L);
        assertDoesNotThrow(() -> ValidadorPayload.validarAlerta("esp32-01", a));
    }

    @Test
    void alertaDeTipoDesconhecidoEhRecusado() {
        AlertaMqtt a = new AlertaMqtt("esp32-01", "INCENDIO", 220.0, 1.0, "ON", 0L);
        assertThrows(PayloadInvalidoException.class, () -> ValidadorPayload.validarAlerta("esp32-01", a));
    }
}
