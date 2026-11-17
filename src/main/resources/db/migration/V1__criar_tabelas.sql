-- Última leitura de cada dispositivo: uma linha por dispositivo, atualizada a cada mensagem.
CREATE TABLE leitura_atual (
    dispositivo     VARCHAR(64)  NOT NULL,
    tensao          DOUBLE       NULL,
    corrente        DOUBLE       NULL,
    potencia        DOUBLE       NULL,
    kwh             DOUBLE       NULL,
    rele            VARCHAR(3)   NULL,
    alerta          VARCHAR(20)  NULL,
    tensao_min      DOUBLE       NULL,
    tensao_max      DOUBLE       NULL,
    corrente_max    DOUBLE       NULL,
    intervalo_ms    INT          NULL,
    online          BIT(1)       NOT NULL,
    momento_leitura DATETIME(6)  NULL,
    recebido_em     DATETIME(6)  NOT NULL,
    PRIMARY KEY (dispositivo)
);

-- Histórico agregado por minuto (gravar toda leitura de 2 s encheria o disco sem ganho para os gráficos).
CREATE TABLE historico_resumido (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    dispositivo     VARCHAR(64)  NOT NULL,
    minuto          DATETIME(6)  NOT NULL,
    amostras        INT          NOT NULL,
    tensao_media    DOUBLE       NOT NULL,
    tensao_min      DOUBLE       NOT NULL,
    tensao_max      DOUBLE       NOT NULL,
    corrente_media  DOUBLE       NOT NULL,
    corrente_max    DOUBLE       NOT NULL,
    potencia_media  DOUBLE       NOT NULL,
    potencia_max    DOUBLE       NOT NULL,
    consumo_kwh     DOUBLE       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_historico_dispositivo_minuto UNIQUE (dispositivo, minuto)
);

-- Alertas, mudanças de online/offline e comandos enviados.
CREATE TABLE eventos_importantes (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    dispositivo     VARCHAR(64)  NOT NULL,
    tipo            VARCHAR(30)  NOT NULL,
    descricao       VARCHAR(255) NOT NULL,
    tensao          DOUBLE       NULL,
    corrente        DOUBLE       NULL,
    rele            VARCHAR(3)   NULL,
    momento         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_eventos_dispositivo_momento ON eventos_importantes (dispositivo, momento);
