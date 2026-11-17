# FlowSense — Backend

Spring Boot 4.1.1 (Java 21). Assina os tópicos MQTT do ESP32, grava em MySQL (JPA + Flyway) e expõe a API REST usada pelo dashboard.

Documentação completa (arquitetura, tópicos, API, variáveis de ambiente e implantação) no [README da raiz do projeto](../README.md).

## Pacotes

| Pacote | Papel |
|---|---|
| `mqtt` | `MqttClienteFlowsense` (conexão Paho, reconexão, publicação de comandos) e `MqttRoteador` (decide o que fazer com cada tópico) |
| `service` | regras: `LeituraService` (grava leituras/alertas/status), `ConsultaService` (dispositivos, histórico, consumo), `ComandoService`, `EventoService`, `LimpezaService`, `ValidadorPayload`, `CalculadoraConsumo` |
| `controller` | `DispositivoController` (rotas `/api/dispositivos/...`) |
| `entity` / `repository` | `LeituraAtual`, `HistoricoResumido`, `EventosImportantes` e seus repositórios |
| `dto` | `mqtt` (mensagens do ESP32), `request` (corpos validados), `response` |
| `config` | propriedades `flowsense.*`, CORS, chave de API (`ApiKeyInterceptor`) |
| `exception` | exceções e `GlobalExceptionHandler` (erros em JSON) |

Migrations em `src/main/resources/db/migration`.

## Comandos

```bash
./mvnw test
```

```bash
./mvnw spring-boot:run
```

O `spring-boot:run` precisa de um MySQL local e das variáveis `DB_PASSWORD` e `FLOWSENSE_API_KEY`.
