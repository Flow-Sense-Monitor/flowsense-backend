package com.project.flowsense.mqtt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.flowsense.config.FlowsenseProperties;
import com.project.flowsense.exception.PayloadInvalidoException;
import com.project.flowsense.service.LeituraService;

import tools.jackson.databind.json.JsonMapper;

class MqttRoteadorTest {

    private static final String LEITURA_REAL = """
            {"dispositivo":"esp32-01","tensao":221.30,"corrente":5.42,"potencia":1199.45,
             "kwh":0.00213,"rele":"ON","alerta":"NORMAL",
             "limites":{"tensaoMin":198,"tensaoMax":242,"correnteMax":20},
             "intervalo":2000,"timestamp":1794445800}""";

    private LeituraService leituraService;
    private MqttRoteador roteador;

    @BeforeEach
    void preparar() {
        leituraService = mock(LeituraService.class);
        FlowsenseProperties props = new FlowsenseProperties(List.of("esp32-01"), "chave", 0.95,
                List.of("http://localhost:5173"), 90,
                new FlowsenseProperties.Mqtt("broker.hivemq.com", 1883, null, null, false));
        roteador = new MqttRoteador(props, leituraService, JsonMapper.builder().build());
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void leituraRealDoFirmwareEhEncaminhada() {
        assertTrue(roteador.processar("flowsense/esp32-01/leituras", bytes(LEITURA_REAL)));
        verify(leituraService).registrarLeitura(eq("esp32-01"),
                argThat(l -> l.tensao() == 221.30 && "ON".equals(l.rele()) && l.limites().correnteMax() == 20));
    }

    @Test
    void jsonQuebradoEhDescartadoSemErro() {
        assertFalse(roteador.processar("flowsense/esp32-01/leituras", bytes("{\"tensao\": 22")));
        assertFalse(roteador.processar("flowsense/esp32-01/leituras", bytes("{\"tensao\":\"muito alta\"}")));
        verifyNoInteractions(leituraService);
    }

    @Test
    void dispositivoNaoConfiguradoEhIgnorado() {
        assertFalse(roteador.processar("flowsense/intruso/leituras", bytes(LEITURA_REAL)));
        verifyNoInteractions(leituraService);
    }

    @Test
    void topicoForaDoPadraoEhIgnorado() {
        assertFalse(roteador.processar("flowsense/esp32-01/leituras/extra", bytes(LEITURA_REAL)));
        assertFalse(roteador.processar("outro/esp32-01/leituras", bytes(LEITURA_REAL)));
        verifyNoInteractions(leituraService);
    }

    @Test
    void mensagemGrandeDemaisEhDescartada() {
        byte[] enorme = new byte[MqttRoteador.TAMANHO_MAXIMO_BYTES + 1];
        assertFalse(roteador.processar("flowsense/esp32-01/leituras", enorme));
        verifyNoInteractions(leituraService);
    }

    @Test
    void payloadInvalidoEhDescartadoSemPropagarExcecao() {
        doThrow(new PayloadInvalidoException("rele inválido"))
                .when(leituraService).registrarLeitura(anyString(), any());
        assertFalse(roteador.processar("flowsense/esp32-01/leituras", bytes(LEITURA_REAL)));
    }

    @Test
    void statusEhEncaminhadoComoTexto() {
        assertTrue(roteador.processar("flowsense/esp32-01/status", bytes("offline")));
        verify(leituraService).registrarStatus("esp32-01", "offline");
    }
}
