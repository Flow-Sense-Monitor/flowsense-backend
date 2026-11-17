package com.project.flowsense.dto.response;

import java.time.Instant;

import com.project.flowsense.entity.LeituraAtual;

/** Item de GET /api/dispositivos. */
public record DispositivoResponse(String id, boolean online, String alerta, Instant recebidoEm) {

    public static DispositivoResponse de(LeituraAtual l) {
        return new DispositivoResponse(l.getDispositivo(), l.isOnline(), l.getAlerta(), l.getRecebidoEm());
    }
}
