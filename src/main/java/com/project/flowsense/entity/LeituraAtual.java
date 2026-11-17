package com.project.flowsense.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Última leitura conhecida de cada dispositivo (uma linha por ESP32).
 * É sobrescrita a cada mensagem recebida em flowsense/{id}/leituras.
 */
@Entity
@Table(name = "leitura_atual")
@Getter
@Setter
@NoArgsConstructor
public class LeituraAtual {

    @Id
    @Column(length = 64)
    private String dispositivo;

    private Double tensao;     // V
    private Double corrente;   // A
    private Double potencia;   // W (já calculada pelo ESP32; vem 0 com o relé desligado)
    private Double kwh;        // consumo acumulado desde o último reinício do ESP32

    @Column(length = 3)
    private String rele;       // ON | OFF

    @Column(length = 20)
    private String alerta;     // NORMAL | SUBTENSAO | SOBRETENSAO | SOBRECORRENTE

    private Double tensaoMin;
    private Double tensaoMax;
    private Double correnteMax;
    private Integer intervaloMs;

    @Column(nullable = false)
    private boolean online;

    /** Horário da leitura informado pelo ESP32 (ou do servidor, se o relógio do ESP32 não estava sincronizado). */
    private Instant momentoLeitura;

    /** Horário em que o backend recebeu a última mensagem; usado para detectar dispositivo sem dados. */
    @Column(nullable = false)
    private Instant recebidoEm;

    public LeituraAtual(String dispositivo) {
        this.dispositivo = dispositivo;
    }
}
