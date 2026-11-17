package com.project.flowsense.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.flowsense.config.FlowsenseProperties;
import com.project.flowsense.dto.response.ConsumoResponse;
import com.project.flowsense.dto.response.DispositivoResponse;
import com.project.flowsense.dto.response.LeituraAtualResponse;
import com.project.flowsense.dto.response.PontoHistoricoResponse;
import com.project.flowsense.entity.HistoricoResumido;
import com.project.flowsense.exception.DispositivoNaoEncontradoException;
import com.project.flowsense.repository.HistoricoResumidoRepository;
import com.project.flowsense.repository.LeituraAtualRepository;

import lombok.RequiredArgsConstructor;

/** Consultas usadas pelo dashboard: dispositivos, leitura atual, histórico e consumo. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaService {

    /** Máximo de pontos devolvidos num gráfico; acima disso agrupamos vários minutos em um ponto. */
    static final int MAXIMO_PONTOS = 500;
    /** Tamanhos de agrupamento possíveis, em minutos (1 h → 1 min, 24 h → 5 min, 7 dias → 30 min). */
    private static final int[] AGRUPAMENTOS = { 1, 2, 5, 10, 15, 30, 60, 120, 240, 720, 1440 };
    /** Período máximo de uma consulta. */
    private static final Duration PERIODO_MAXIMO = Duration.ofDays(92);
    /** Período padrão quando o frontend não manda "de". */
    private static final Duration PERIODO_PADRAO = Duration.ofHours(1);

    private final LeituraAtualRepository leituraAtualRepository;
    private final HistoricoResumidoRepository historicoRepository;
    private final FlowsenseProperties propriedades;
    private final Clock relogio;

    /** Todos os dispositivos configurados, mesmo os que ainda não mandaram dados (aparecem offline). */
    public List<DispositivoResponse> listarDispositivos() {
        return propriedades.dispositivos().stream()
                .map(id -> leituraAtualRepository.findById(id)
                        .map(DispositivoResponse::de)
                        .orElse(new DispositivoResponse(id, false, null, null)))
                .toList();
    }

    public LeituraAtualResponse leituraAtual(String id) {
        verificarDispositivo(id);
        return leituraAtualRepository.findById(id)
                .map(LeituraAtualResponse::de)
                .orElseThrow(() -> new DispositivoNaoEncontradoException(id));
    }

    /** Pontos para os gráficos no período [de, ate). */
    public List<PontoHistoricoResponse> historico(String id, Instant de, Instant ate) {
        verificarDispositivo(id);
        Periodo periodo = resolverPeriodo(de, ate);
        List<HistoricoResumido> minutos = historicoRepository
                .findByDispositivoAndMinutoGreaterThanEqualAndMinutoLessThanOrderByMinutoAsc(
                        id, periodo.de(), periodo.ate());
        int tamanho = tamanhoDoAgrupamento(Duration.between(periodo.de(), periodo.ate()));
        return agrupar(minutos, tamanho);
    }

    public ConsumoResponse consumo(String id, Instant de, Instant ate) {
        verificarDispositivo(id);
        Periodo periodo = resolverPeriodo(de, ate);
        double kwh = historicoRepository.somarConsumo(id, periodo.de(), periodo.ate());
        double tarifa = propriedades.tarifaKwh();
        return new ConsumoResponse(id, periodo.de(), periodo.ate(), kwh, tarifa, kwh * tarifa);
    }

    public void verificarDispositivo(String id) {
        if (!propriedades.dispositivoPermitido(id)) {
            throw new DispositivoNaoEncontradoException(id);
        }
    }

    /** Preenche os padrões (última hora até agora) e valida o período. */
    Periodo resolverPeriodo(Instant de, Instant ate) {
        Instant fim = ate != null ? ate : relogio.instant();
        Instant inicio = de != null ? de : fim.minus(PERIODO_PADRAO);
        if (!inicio.isBefore(fim)) {
            throw new IllegalArgumentException("O parâmetro 'de' deve ser anterior a 'ate'.");
        }
        if (Duration.between(inicio, fim).compareTo(PERIODO_MAXIMO) > 0) {
            throw new IllegalArgumentException("O período máximo de consulta é de " + PERIODO_MAXIMO.toDays() + " dias.");
        }
        return new Periodo(inicio, fim);
    }

    /** Menor agrupamento que mantém o gráfico com no máximo MAXIMO_PONTOS pontos. */
    static int tamanhoDoAgrupamento(Duration periodo) {
        long totalMinutos = Math.max(1, periodo.toMinutes());
        for (int tamanho : AGRUPAMENTOS) {
            if (totalMinutos / tamanho <= MAXIMO_PONTOS) {
                return tamanho;
            }
        }
        return AGRUPAMENTOS[AGRUPAMENTOS.length - 1];
    }

    /**
     * Junta os minutos em grupos de "tamanho" minutos. Médias ponderadas pelo número de amostras,
     * mínimos e máximos dos grupos, e consumo somado.
     */
    static List<PontoHistoricoResponse> agrupar(List<HistoricoResumido> minutos, int tamanho) {
        Map<Long, List<HistoricoResumido>> grupos = new LinkedHashMap<>();
        for (HistoricoResumido m : minutos) {
            long minutoEpoch = m.getMinuto().getEpochSecond() / 60;
            long inicioGrupo = minutoEpoch / tamanho * tamanho;
            grupos.computeIfAbsent(inicioGrupo, k -> new ArrayList<>()).add(m);
        }

        List<PontoHistoricoResponse> pontos = new ArrayList<>();
        grupos.forEach((inicioGrupo, lista) -> {
            int amostras = 0;
            double somaTensao = 0, somaCorrente = 0, somaPotencia = 0, consumo = 0;
            double tensaoMin = Double.MAX_VALUE, tensaoMax = 0, correnteMax = 0, potenciaMax = 0;
            for (HistoricoResumido m : lista) {
                amostras += m.getAmostras();
                somaTensao += m.getTensaoMedia() * m.getAmostras();
                somaCorrente += m.getCorrenteMedia() * m.getAmostras();
                somaPotencia += m.getPotenciaMedia() * m.getAmostras();
                consumo += m.getConsumoKwh();
                tensaoMin = Math.min(tensaoMin, m.getTensaoMin());
                tensaoMax = Math.max(tensaoMax, m.getTensaoMax());
                correnteMax = Math.max(correnteMax, m.getCorrenteMax());
                potenciaMax = Math.max(potenciaMax, m.getPotenciaMax());
            }
            int n = Math.max(1, amostras);
            pontos.add(new PontoHistoricoResponse(Instant.ofEpochSecond(inicioGrupo * 60), tamanho,
                    somaTensao / n, tensaoMin, tensaoMax,
                    somaCorrente / n, correnteMax,
                    somaPotencia / n, potenciaMax, consumo));
        });
        return pontos;
    }

    record Periodo(Instant de, Instant ate) {
    }
}
