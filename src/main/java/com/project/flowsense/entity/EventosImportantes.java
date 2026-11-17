package com.project.flowsense.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de tudo que importa para o usuário: alertas do ESP32,
 * dispositivo ficando online/offline e comandos enviados pelo dashboard.
 */
@Entity
@Table(name = "eventos_importantes")
@Getter
@Setter
@NoArgsConstructor
public class EventosImportantes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String dispositivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEvento tipo;

    @Column(nullable = false)
    private String descricao;

    private Double tensao;
    private Double corrente;

    @Column(length = 3)
    private String rele;

    @Column(nullable = false)
    private Instant momento;

    public EventosImportantes(String dispositivo, TipoEvento tipo, String descricao, Instant momento) {
        this.dispositivo = dispositivo;
        this.tipo = tipo;
        this.descricao = descricao;
        this.momento = momento;
    }
}
