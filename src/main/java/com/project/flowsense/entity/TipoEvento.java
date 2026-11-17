package com.project.flowsense.entity;

/** Tipos de evento guardados em EventosImportantes. */
public enum TipoEvento {
    // Alertas publicados pelo ESP32 em flowsense/{id}/alertas
    SOBRETENSAO,
    SUBTENSAO,
    SOBRECORRENTE,
    ALERTA_ENCERRADO,   // o ESP32 publicou "NORMAL"

    // Mudanças de conexão (flowsense/{id}/status)
    ONLINE,
    OFFLINE,

    // Comandos enviados pelo dashboard (flowsense/{id}/comandos)
    COMANDO_RELE,
    COMANDO_LIMITES,
    COMANDO_INTERVALO
}
