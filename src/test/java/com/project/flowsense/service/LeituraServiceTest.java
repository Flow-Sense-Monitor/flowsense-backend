package com.project.flowsense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.flowsense.dto.mqtt.AlertaMqtt;
import com.project.flowsense.dto.mqtt.LeituraMqtt;
import com.project.flowsense.entity.HistoricoResumido;
import com.project.flowsense.entity.LeituraAtual;
import com.project.flowsense.entity.TipoEvento;
import com.project.flowsense.exception.PayloadInvalidoException;
import com.project.flowsense.repository.HistoricoResumidoRepository;
import com.project.flowsense.repository.LeituraAtualRepository;

/** Testa o LeituraService com repositórios "de mentira" (Mockito) guardando os dados em mapas. */
class LeituraServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-14T15:00:30Z");
    private static final String ID = "esp32-01";

    private final Map<String, LeituraAtual> leituras = new HashMap<>();
    private final Map<Instant, HistoricoResumido> historico = new HashMap<>();

    private LeituraAtualRepository leituraRepo;
    private HistoricoResumidoRepository historicoRepo;
    private EventoService eventoService;
    private LeituraService service;

    @BeforeEach
    void preparar() {
        leituraRepo = mock(LeituraAtualRepository.class);
        historicoRepo = mock(HistoricoResumidoRepository.class);
        eventoService = mock(EventoService.class);

        when(leituraRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(leituras.get(i.getArgument(0))));
        when(leituraRepo.save(any())).thenAnswer(i -> {
            LeituraAtual l = i.getArgument(0);
            leituras.put(l.getDispositivo(), l);
            return l;
        });
        when(historicoRepo.findByDispositivoAndMinuto(anyString(), any()))
                .thenAnswer(i -> Optional.ofNullable(historico.get(i.getArgument(1))));
        when(historicoRepo.save(any())).thenAnswer(i -> {
            HistoricoResumido h = i.getArgument(0);
            historico.put(h.getMinuto(), h);
            return h;
        });

        service = new LeituraService(leituraRepo, historicoRepo, eventoService, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private static LeituraMqtt leitura(double kwh, long timestamp) {
        return new LeituraMqtt(ID, 221.3, 5.42, 1199.45, kwh, "ON", "NORMAL",
                new LeituraMqtt.LimitesMqtt(198.0, 242.0, 20.0), 2000, timestamp);
    }

    @Test
    void timestampZeroGravaComHorarioDoServidor() {
        service.registrarLeitura(ID, leitura(0.001, 0));

        LeituraAtual atual = leituras.get(ID);
        assertEquals(AGORA, atual.getMomentoLeitura());
        assertTrue(atual.isOnline());
        // e o histórico cai no minuto do servidor (15:00)
        assertTrue(historico.containsKey(Instant.parse("2026-11-14T15:00:00Z")));
    }

    @Test
    void reinicioDoKwhNaoGeraConsumoNegativo() {
        service.registrarLeitura(ID, leitura(0.500, 0)); // primeira: sem base, consumo 0
        service.registrarLeitura(ID, leitura(0.510, 0)); // +0.010
        service.registrarLeitura(ID, leitura(0.002, 0)); // reiniciou: +0.002

        HistoricoResumido minuto = historico.get(Instant.parse("2026-11-14T15:00:00Z"));
        assertEquals(3, minuto.getAmostras());
        assertEquals(0.012, minuto.getConsumoKwh(), 1e-9);
        assertEquals(0.002, leituras.get(ID).getKwh(), 1e-9);
    }

    @Test
    void primeiraLeituraRegistraEventoOnline() {
        service.registrarLeitura(ID, leitura(0.001, 0));
        service.registrarLeitura(ID, leitura(0.002, 0));
        // só uma vez: na segunda o dispositivo já estava online
        verify(eventoService).registrar(eq(ID), eq(TipoEvento.ONLINE), anyString(), eq(AGORA));
    }

    @Test
    void leituraInvalidaNaoGravaNada() {
        LeituraMqtt invalida = new LeituraMqtt(ID, null, 5.0, 100.0, 0.1, "ON", "NORMAL", null, 2000, 0L);

        assertThrows(PayloadInvalidoException.class, () -> service.registrarLeitura(ID, invalida));
        verify(leituraRepo, never()).save(any());
        verify(historicoRepo, never()).save(any());
    }

    @Test
    void alertaViraEvento() {
        service.registrarAlerta(ID, new AlertaMqtt(ID, "SOBRECORRENTE", 220.0, 25.0, "OFF", 0L));
        verify(eventoService).registrar(eq(ID), eq(TipoEvento.SOBRECORRENTE), anyString(), eq(AGORA),
                eq(220.0), eq(25.0), eq("OFF"));
    }

    @Test
    void statusSoRegistraEventoQuandoMuda() {
        service.registrarStatus(ID, "online");
        service.registrarStatus(ID, "online");   // repetido (mensagem retida): ignora
        service.registrarStatus(ID, "offline");

        verify(eventoService).registrar(eq(ID), eq(TipoEvento.ONLINE), anyString(), eq(AGORA));
        verify(eventoService).registrar(eq(ID), eq(TipoEvento.OFFLINE), anyString(), eq(AGORA));
        assertEquals(false, leituras.get(ID).isOnline());
    }

    @Test
    void statusDesconhecidoEhRecusado() {
        assertThrows(PayloadInvalidoException.class, () -> service.registrarStatus(ID, "talvez"));
    }
}
