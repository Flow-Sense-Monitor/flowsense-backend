package com.project.flowsense.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.project.flowsense.dto.request.IntervaloRequest;
import com.project.flowsense.dto.request.LimitesRequest;
import com.project.flowsense.dto.request.ReleRequest;
import com.project.flowsense.dto.response.ComandoResponse;
import com.project.flowsense.dto.response.ConsumoResponse;
import com.project.flowsense.dto.response.DispositivoResponse;
import com.project.flowsense.dto.response.EventoResponse;
import com.project.flowsense.dto.response.LeituraAtualResponse;
import com.project.flowsense.dto.response.PontoHistoricoResponse;
import com.project.flowsense.service.ComandoService;
import com.project.flowsense.service.ConsultaService;
import com.project.flowsense.service.EventoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/dispositivos")
@RequiredArgsConstructor
public class DispositivoController {

    private final ConsultaService consultaService;
    private final EventoService eventoService;
    private final ComandoService comandoService;

    @GetMapping
    public List<DispositivoResponse> listar() {
        return consultaService.listarDispositivos();
    }

    @GetMapping("/{id}/leitura-atual")
    public LeituraAtualResponse leituraAtual(@PathVariable String id) {
        return consultaService.leituraAtual(id);
    }

    @GetMapping("/{id}/historico")
    public List<PontoHistoricoResponse> historico(@PathVariable String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate) {
        return consultaService.historico(id, de, ate);
    }

    @GetMapping("/{id}/consumo")
    public ConsumoResponse consumo(@PathVariable String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate) {
        return consultaService.consumo(id, de, ate);
    }

    @GetMapping("/{id}/eventos")
    public List<EventoResponse> eventos(@PathVariable String id,
            @RequestParam(defaultValue = "50") int limite) {
        consultaService.verificarDispositivo(id);
        return eventoService.listar(id, limite);
    }

    @PostMapping("/{id}/rele")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ComandoResponse rele(@PathVariable String id, @Valid @RequestBody ReleRequest req) {
        return comandoService.alterarRele(id, req);
    }

    @PutMapping("/{id}/limites")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ComandoResponse limites(@PathVariable String id, @Valid @RequestBody LimitesRequest req) {
        return comandoService.alterarLimites(id, req);
    }

    @PutMapping("/{id}/intervalo")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ComandoResponse intervalo(@PathVariable String id, @Valid @RequestBody IntervaloRequest req) {
        return comandoService.alterarIntervalo(id, req);
    }
}
