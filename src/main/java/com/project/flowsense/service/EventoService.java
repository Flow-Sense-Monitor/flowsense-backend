package com.project.flowsense.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.flowsense.dto.response.EventoResponse;
import com.project.flowsense.entity.EventosImportantes;
import com.project.flowsense.entity.TipoEvento;
import com.project.flowsense.repository.EventosImportantesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Grava e lista os eventos importantes (alertas, online/offline, comandos). */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventoService {

    public static final int LIMITE_MAXIMO = 200;

    private final EventosImportantesRepository repositorio;

    @Transactional
    public EventosImportantes registrar(String dispositivo, TipoEvento tipo, String descricao, Instant momento) {
        return registrar(dispositivo, tipo, descricao, momento, null, null, null);
    }

    @Transactional
    public EventosImportantes registrar(String dispositivo, TipoEvento tipo, String descricao, Instant momento,
            Double tensao, Double corrente, String rele) {
        EventosImportantes evento = new EventosImportantes(dispositivo, tipo, descricao, momento);
        evento.setTensao(tensao);
        evento.setCorrente(corrente);
        evento.setRele(rele);
        log.info("Evento {} em {}: {}", tipo, dispositivo, descricao);
        return repositorio.save(evento);
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listar(String dispositivo, int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new IllegalArgumentException("O parâmetro 'limite' deve estar entre 1 e " + LIMITE_MAXIMO + ".");
        }
        return repositorio.findByDispositivoOrderByMomentoDescIdDesc(dispositivo, PageRequest.of(0, limite))
                .stream()
                .map(EventoResponse::de)
                .toList();
    }
}
