package com.project.flowsense.dto.response;

import java.time.Instant;

import com.project.flowsense.entity.EventosImportantes;
import com.project.flowsense.entity.TipoEvento;

/** Item de GET /api/dispositivos/{id}/eventos. */
public record EventoResponse(
        Long id,
        TipoEvento tipo,
        String descricao,
        Double tensao,
        Double corrente,
        String rele,
        Instant momento) {

    public static EventoResponse de(EventosImportantes e) {
        return new EventoResponse(e.getId(), e.getTipo(), e.getDescricao(),
                e.getTensao(), e.getCorrente(), e.getRele(), e.getMomento());
    }
}
