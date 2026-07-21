# geoflow-api

Servico HTTP principal do GeoFlow que recebe requisicoes para criacao de jobs, persiste os dados iniciais no PostgreSQL, registra logs e publica eventos `JobRequestedEvent` no Kafka para processamento assincrono.

## Responsabilidades

- expor endpoints HTTP para o frontend
- validar o payload de criacao do job
- persistir `processing_job` e `processing_job_log`
- gerar ids usando a tabela `id_generator`
- publicar `geoflow.job.requested`
- permitir consulta de status e logs do job
- permitir reprocessamento manual de jobs com falha
- expor correlationId e contagem de tentativas

## Stack

- Java 25
- Spring Boot 4.0.6
- Spring Web
- Spring JDBC
- Spring Kafka
- Flyway
- PostgreSQL
- Springdoc OpenAPI

## Porta e configuracao

Configuracao padrao em `src/main/resources/application.yml`:

- porta HTTP: `8080`
- banco: `jdbc:postgresql://localhost:5432/geoflow`
- usuario: `geoflow`
- senha: `geoflow123`
- Kafka: `localhost:9092`
- topico de entrada: `geoflow.job.requested`
- timezone da aplicacao: `America/Sao_Paulo`

## Endpoints

### `POST /api/jobs`

Cria um novo job e dispara o processamento assincrono.

Payload esperado:

```json
{
  "tenantId": "example-farm",
  "name": "BBOX example",
  "jobType": "BBOX_TO_GEOJSON",
  "area": {
    "minLon": -51.13,
    "minLat": -19.093,
    "maxLon": -49.156,
    "maxLat": -18.013
  }
}
```

Resposta:

```json
{
  "id": 1,
  "status": "PENDING",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "attemptCount": 0,
  "maxAttempts": 3,
  "createdAt": "2026-01-01T12:00:00-03:00"
}
```

### `GET /api/jobs/{id}`

Retorna o estado atual do job, incluindo:

- identificacao
- `tenantId`
- nome
- tipo
- status
- area original
- `resultGeojson`
- `errorMessage`
- datas de criacao, inicio, fim e atualizacao

### `GET /api/jobs/{id}/logs`

Retorna os logs persistidos para o job.

### `POST /api/jobs/{id}/reprocess`

Reinicia um job em status `FAILED`, zera a contagem de tentativas, gera um novo `correlationId` e republica `JobRequestedEvent`.

### `GET /api/health`

Healthcheck simples do servico.

## Swagger / OpenAPI

O servico tem Springdoc configurado com o titulo `GeoFlow API`.

Rota esperada do Swagger:

- `/swagger-ui/index.html`

## Fluxo interno

1. `JobController` recebe a requisicao.
2. `JobService` cria o job com status inicial `PENDING`.
3. O job nasce com `correlationId`, `attemptCount = 0` e `maxAttempts = 3`.
4. Um log `JOB_CREATED` e gravado.
5. `JobEventPublisher` publica um `JobRequestedEvent` no Kafka.
6. O processor assume o fluxo assincrono a partir desse ponto.

## Banco e migracoes

Migracoes presentes:

- `V1__create_processing_tables.sql`
- `V2__create_id_generator_and_migrate_ids.sql`
- `V3__use_timestamptz_and_offsetdatetime.sql`
- `V4__add_retry_and_correlation_tracking.sql`

Entidades de persistencia usadas:

- `processing_job`
- `processing_job_log`
- `id_generator`

## Como rodar

Antes de rodar, instale a dependencia compartilhada:

```bash
cd ../geoflow-shared
mvn install
```

Depois:

```bash
mvn spring-boot:run
```

## Testes

- `GeoflowApiApplicationTests`
- `JobEventPublisherTest`
- `IdGeneratorServiceTest`
- `JobServiceTest`
