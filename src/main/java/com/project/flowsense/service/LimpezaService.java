package com.project.flowsense.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.project.flowsense.config.FlowsenseProperties;
import com.project.flowsense.repository.EventosImportantesRepository;
import com.project.flowsense.repository.HistoricoResumidoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Apaga histórico e eventos mais antigos que FLOWSENSE_RETENCAO_DIAS, para não encher o disco da VM. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LimpezaService {

    private final HistoricoResumidoRepository historicoRepository;
    private final EventosImportantesRepository eventosRepository;
    private final FlowsenseProperties propriedades;
    private final Clock relogio;

    /** Roda todo dia às 03:30 (UTC). */
    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    public void apagarDadosAntigos() {
        Instant limite = relogio.instant().minus(Duration.ofDays(propriedades.retencaoDias()));
        long historico = historicoRepository.deleteByMinutoBefore(limite);
        long eventos = eventosRepository.deleteByMomentoBefore(limite);
        log.info("Limpeza: {} minutos de histórico e {} eventos anteriores a {} apagados", historico, eventos, limite);
    }
}
