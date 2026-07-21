# GeoFlow

GeoFlow e um monorepo para processamento geografico assincrono com arquitetura orientada a servicos. Hoje o fluxo implementado cobre a criacao de jobs via HTTP, publicacao em Kafka, processamento no backend, persistencia em PostgreSQL e notificacao de conclusao no frontend via Server-Sent Events (SSE).

## Visao geral

O repositorio esta dividido nestes projetos:

- `geoflow-front`: interface React + TypeScript + Vite para criar jobs, aguardar notificacoes e visualizar o resultado no mapa.
- `geoflow-api`: API HTTP principal para criar jobs, consultar status, consultar logs e publicar eventos `JobRequestedEvent`.
- `geoflow-processor`: consumidor Kafka baseado em Spring Boot + Apache Camel que processa jobs `BBOX_TO_GEOJSON`.
- `geoflow-notifier`: servico HTTP que expõe SSE para o frontend e reage aos eventos finais do processamento.
- `geoflow-shared`: biblioteca Java com contratos compartilhados entre os servicos.

## Fluxo atual

1. O frontend envia um `POST /api/jobs` para `geoflow-api`.
2. A API grava o job em `processing_job`, registra o log inicial em `processing_job_log` e publica `geoflow.job.requested` no Kafka.
3. O `geoflow-processor` consome esse topico, muda o status para `PROCESSING`, monta um GeoJSON de poligono a partir do bounding box e atualiza o job no banco.
4. Internamente, o `geoflow-processor` desacopla o consumo Kafka do processamento usando `seda:job-processing`.
5. Ao terminar, o processor publica `geoflow.job.completed` ou `geoflow.job.failed`.
6. Se uma tentativa falhar e o job ainda estiver abaixo do limite configurado, o processor marca o job como `RETRY_PENDING` e agenda uma nova publicacao de `geoflow.job.requested`.
7. Se o limite de tentativas for excedido, o processor tambem publica `geoflow.job.dead-letter`.
8. O `geoflow-notifier` consome os topicos finais e acorda os clientes conectados em `/notifications/jobs/{jobId}/stream`.
9. O frontend recebe a notificacao SSE, redireciona para a tela de resultado e busca detalhes do job e dos logs na API.


## Stack utilizada

- Java 25
- Spring Boot 4.0.6
- Apache Kafka 3.9
- Apache Camel 4.10
- PostgreSQL 16 + PostGIS
- Flyway
- React 19
- TypeScript 6
- Vite 8
- React Leaflet
- Docker Compose

## Estrutura do repositorio

```text
.
├── docker-compose.yml
├── geoflow-api
├── geoflow-front
├── geoflow-notifier
├── geoflow-processor
└── geoflow-shared
```

## Infraestrutura local

O arquivo `docker-compose.yml` sobe:

- `postgres` em `localhost:5432`
- `kafka` em `localhost:9092`
- `kafka-ui` em `http://localhost:8085`

Credenciais padrao do banco:

- banco: `geoflow`
- usuario: `geoflow`
- senha: `geoflow123`

Para subir a infraestrutura:

```bash
docker compose up -d
```

## Portas e endpoints

Portas configuradas hoje:

- `geoflow-api`: `8080`
- `geoflow-notifier`: `8082`
- `geoflow-front` em desenvolvimento: `5173` por padrao do Vite
- `kafka-ui`: `8085`

Principais endpoints:

- `POST /api/jobs`
- `GET /api/jobs/{id}`
- `GET /api/jobs/{id}/logs`
- `GET /api/health` na API
- `GET /api/health` no notifier
- `GET /notifications/jobs/{jobId}/stream`
- Swagger UI da API via Springdoc em `/swagger-ui/index.html`

## Topicos Kafka

Topicos usados no estado atual do projeto:

- `geoflow.job.requested`
- `geoflow.job.completed`
- `geoflow.job.failed`
- `geoflow.job.dead-letter`

## Como executar localmente

### 1. Suba a infraestrutura

```bash
docker compose up -d
```

### 2. Gere e instale a biblioteca compartilhada

```bash
cd geoflow-shared
mvn install
```

### 3. Suba os servicos Java

Em terminais separados:

```bash
cd geoflow-api
mvn spring-boot:run
```

```bash
cd geoflow-processor
mvn spring-boot:run
```

```bash
cd geoflow-notifier
mvn spring-boot:run
```

### 4. Suba o frontend

```bash
cd geoflow-front
npm install
npm run dev
```

O frontend usa proxy do Vite:

- `/api` -> `http://localhost:8080`
- `/notifications` -> `http://localhost:8082`

## Exemplo de uso

Exemplo de criacao de job:

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "demo-farm",
    "name": "BBOX via curl",
    "jobType": "BBOX_TO_GEOJSON",
    "area": {
      "minLon": -51.13,
      "minLat": -19.093,
      "maxLon": -49.156,
      "maxLat": -18.013
    }
  }'
```

Consulta de job:

```bash
curl http://localhost:8080/api/jobs/1
```

Consulta de logs:

```bash
curl http://localhost:8080/api/jobs/1/logs
```

## Banco e migracoes

`geoflow-api` e `geoflow-processor` possuem migracoes Flyway equivalentes para manter o schema necessario ao fluxo:

- `processing_job`
- `processing_job_log`
- `id_generator`

As migracoes presentes hoje sao:

- `V1__create_processing_tables.sql`
- `V2__create_id_generator_and_migrate_ids.sql`
- `V3__use_timestamptz_and_offsetdatetime.sql`
- `V4__add_retry_and_correlation_tracking.sql`

## Estado funcional

O caso de uso implementado hoje e:

- tipo de job suportado: `BBOX_TO_GEOJSON`
- entrada: dois cantos opostos de um bounding box
- saida: um GeoJSON `Feature` com geometria `Polygon`
- retry automatico com status intermediario `RETRY_PENDING` e contagem de tentativas persistida
- correlationId persistido no job e nos logs
- reprocessamento manual via API e frontend para jobs com falha


## Testes

Arquivos de teste presentes:

- `geoflow-api`: testes de contexto, `JobService`, `IdGeneratorService` e `JobEventPublisher`
- `geoflow-processor`: teste de contexto, `JobProcessingServiceTest` e `RetrySchedulerServiceTest`
- `geoflow-notifier`: teste de contexto

## Documentacao por modulo

- [geoflow-front/README.md](./geoflow-front/README.md)
- [geoflow-api/README.md](./geoflow-api/README.md)
- [geoflow-processor/README.md](./geoflow-processor/README.md)
- [geoflow-notifier/README.md](./geoflow-notifier/README.md)
- [geoflow-shared/README.md](./geoflow-shared/README.md)
