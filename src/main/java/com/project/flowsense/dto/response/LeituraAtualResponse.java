package com.project.flowsense.dto.response;

import java.time.Instant;

import com.project.flowsense.entity.LeituraAtual;

/** Resposta de GET /api/dispositivos/{id}/leitura-atual. */
public record LeituraAtualResponse(
        String dispositivo,
        Double tensao,
        Double corrente,
        Double potencia,
        Double kwh,
        String rele,
        String alerta,
        Limites limites,
        Integer intervaloMs,
        boolean online,
        Instant momentoLeitura,
        Instant recebidoEm) {

    public record Limites(Double tensaoMin, Double tensaoMax, Double correnteMax) {
    }

    public static LeituraAtualResponse de(LeituraAtual l) {
        return new LeituraAtualResponse(
                l.getDispositivo(), l.getTensao(), l.getCorrente(), l.getPotencia(), l.getKwh(),
                l.getRele(), l.getAlerta(),
                new Limites(l.getTensaoMin(), l.getTensaoMax(), l.getCorrenteMax()),
                l.getIntervaloMs(), l.isOnline(), l.getMomentoLeitura(), l.getRecebidoEm());
    }
}
