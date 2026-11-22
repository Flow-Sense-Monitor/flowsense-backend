package com.project.flowsense.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Histórico agregado por minuto: em vez de gravar cada leitura que chega,
 * guardamos médias, máximos e o consumo daquele minuto.
 * Como o ESP32 manda pelo menos uma leitura por minuto, praticamente todo minuto ganha uma linha.
 */
@Entity
@Table(name = "historico_resumido")
@Getter
@Setter
@NoArgsConstructor
public class HistoricoResumido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String dispositivo;

    /** Início do minuto (ex.: 14:32:00). */
    @Column(nullable = false)
    private Instant minuto;

    /** Quantas leituras entraram na média. */
    @Column(nullable = false)
    private int amostras;

    @Column(nullable = false)
    private double tensaoMedia;
    @Column(nullable = false)
    private double tensaoMin;
    @Column(nullable = false)
    private double tensaoMax;

    @Column(nullable = false)
    private double correnteMedia;
    @Column(nullable = false)
    private double correnteMax;

    @Column(nullable = false)
    private double potenciaMedia;
    @Column(nullable = false)
    private double potenciaMax;

    /** Energia consumida dentro deste minuto (kWh). */
    @Column(nullable = false)
    private double consumoKwh;

    public HistoricoResumido(String dispositivo, Instant minuto) {
        this.dispositivo = dispositivo;
        this.minuto = minuto;
    }

    /** Inclui uma leitura nas médias e máximos deste minuto. */
    public void adicionarAmostra(double tensao, double corrente, double potencia, double consumoKwh) {
        if (amostras == 0) {
            tensaoMin = tensao;
            tensaoMax = tensao;
            correnteMax = corrente;
            potenciaMax = potencia;
        }
        // Média incremental: nova média = (média antiga * n + valor) / (n + 1)
        tensaoMedia = (tensaoMedia * amostras + tensao) / (amostras + 1);
        correnteMedia = (correnteMedia * amostras + corrente) / (amostras + 1);
        potenciaMedia = (potenciaMedia * amostras + potencia) / (amostras + 1);

        tensaoMin = Math.min(tensaoMin, tensao);
        tensaoMax = Math.max(tensaoMax, tensao);
        correnteMax = Math.max(correnteMax, corrente);
        potenciaMax = Math.max(potenciaMax, potencia);

        this.consumoKwh += consumoKwh;
        amostras++;
    }
}
