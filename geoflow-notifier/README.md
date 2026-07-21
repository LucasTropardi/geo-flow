# geoflow-notifier

Servico de notificacao do GeoFlow que consome eventos finais do Kafka e expõe um endpoint SSE para que o frontend seja avisado quando um job terminar.

## Responsabilidades

- consumir `JobCompletedEvent`
- consumir `JobFailedEvent`
- consultar o status atual do job no PostgreSQL
- manter conexoes SSE abertas por `jobId`
- notificar clientes quando o job chegar a um estado terminal
- registrar melhor rastreabilidade nos logs com `correlationId`

## Stack

- Java 25
- Spring Boot 4.0.6
- Spring Web
- Spring Kafka
- Spring JDBC
- PostgreSQL

## Porta e configuracao

Configuracao padrao em `src/main/resources/application.yml`:

- porta HTTP: `8082`
- banco: `jdbc:postgresql://localhost:5432/geoflow`
- usuario: `geoflow`
- senha: `geoflow123`
- Kafka: `localhost:9092`
- topicos:
  - `geoflow.job.completed`
  - `geoflow.job.failed`

## Endpoints

### `GET /notifications/jobs/{jobId}/stream`

Abre uma conexao SSE para acompanhar o job.

Comportamento atual:

- se o job nao existir, responde `404`
- se o job ja estiver em estado terminal, envia o evento final e encerra
- se o job ainda nao terminou, envia um evento inicial `subscribed`
- quando chega a notificacao final, envia `job-status` e encerra a conexao

### `GET /api/health`

Healthcheck simples do servico.

## Eventos SSE emitidos

- `subscribed`
- `job-status`

Payload atual:

```json
{
  "jobId": 1,
  "status": "COMPLETED"
}
```

## Fluxo interno

1. O frontend abre `EventSource` em `/notifications/jobs/{jobId}/stream`.
2. `NotifierSseService` consulta o status atual no banco.
3. Se o job ainda nao terminou, o `SseEmitter` fica armazenado em memoria por `jobId`.
4. `JobResultEventListener` consome mensagens dos topicos finais.
5. Ao receber um evento, o notifier consulta o status no banco e envia SSE para os clientes conectados.

## Observacoes

- os emitters sao mantidos em memoria com `ConcurrentHashMap`
- o servico considera terminais os status `COMPLETED` e `FAILED`
- o notifier nao persiste eventos; ele consulta o estado consolidado do job na tabela `processing_job`

## Como rodar

Instale primeiro a dependencia compartilhada:

```bash
cd ../geoflow-shared
mvn install
```

Depois:

```bash
mvn spring-boot:run
```

## Testes

- `GeoflowNotifierApplicationTests`
