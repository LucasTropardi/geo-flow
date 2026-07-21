# geoflow-processor

Servico responsavel por consumir jobs do Kafka e executar o processamento geografico assincrono. No projeto ele implementa o caso de uso `BBOX_TO_GEOJSON`.

## Responsabilidades

- consumir `JobRequestedEvent` do topico `geoflow.job.requested`
- desacoplar o processamento interno com `SEDA`
- mudar o status do job para `PROCESSING`
- gerar o GeoJSON de saida a partir da area recebida
- atualizar o job no PostgreSQL
- registrar logs de processamento
- publicar `JobCompletedEvent` ou `JobFailedEvent`
- republicar jobs quando ainda houver tentativas disponiveis
- publicar dead-letter quando o limite for excedido

## Stack

- Java 25
- Spring Boot 4.0.6
- Apache Camel 4.10.3
- Spring Kafka
- Spring JDBC
- Flyway
- PostgreSQL

## Configuracao

Arquivo principal: `src/main/resources/application.yml`

- banco: `jdbc:postgresql://localhost:5432/geoflow`
- usuario: `geoflow`
- senha: `geoflow123`
- Kafka: `localhost:9092`
- topicos:
  - `geoflow.job.requested`
  - `geoflow.job.completed`
  - `geoflow.job.failed`
  - `geoflow.job.dead-letter`
- delay padrao para retry: `PT5S`
- scheduler interno para retries assíncronos com pool de 2 threads
- timezone da aplicacao: `America/Sao_Paulo`

## Fluxo de processamento

1. A rota Camel `JobRequestedRoute` consome mensagens do topico `geoflow.job.requested`.
2. A mensagem e enviada para `seda:job-processing`.
3. `JobRequestedMessageProcessor` desserializa o payload para `JobRequestedEvent`.
4. `JobProcessingService` valida o tipo do job.
5. O job e marcado como `PROCESSING`, incrementa `attemptCount` e recebe um log `PROCESSING_STARTED`.
6. `GeoJsonService` valida o bounding box e gera um GeoJSON `Feature` com geometria `Polygon`.
7. O job e marcado como `COMPLETED` com `resultGeojson`, ou como `FAILED` se houver erro.
8. Se ainda houver tentativas disponiveis, o servico grava `RETRY_SCHEDULED`, marca o job como `RETRY_PENDING` e agenda a republicacao de `geoflow.job.requested`.
9. Se o limite for atingido, o servico publica `geoflow.job.failed` e `geoflow.job.dead-letter`.

## Caso de uso suportado

Tipo de job suportado:

- `BBOX_TO_GEOJSON`

Regras implementadas:

- `minLon` deve ser menor que `maxLon`
- `minLat` deve ser menor que `maxLat`
- longitude entre `-180` e `180`
- latitude entre `-90` e `90`

GeoJSON produzido:

- tipo: `Feature`
- geometria: `Polygon`
- propriedades:
  - `jobId`
  - `tenantId`
  - `jobType`

## Tratamento de falhas

- excecoes da rota Camel sao interceptadas por `onException`
- se o evento ja tiver sido desserializado, o job e marcado como `FAILED`
- um log `PROCESSING_FAILED` e gravado
- se houver margem para nova tentativa, o job vai para `RETRY_PENDING` e o evento original e republicado apos o atraso configurado
- se nao houver mais tentativas, `JobFailedEvent` e `JobDeadLetterEvent` sao publicados

## Banco e migracoes

Migracoes presentes:

- `V1__create_processing_tables.sql`
- `V2__create_id_generator_and_migrate_ids.sql`
- `V3__use_timestamptz_and_offsetdatetime.sql`
- `V4__add_retry_and_correlation_tracking.sql`

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

- `GeoflowProcessorApplicationTests`
- `JobProcessingServiceTest`
- `RetrySchedulerServiceTest`
