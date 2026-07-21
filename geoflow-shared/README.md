# geoflow-shared

Biblioteca Java compartilhada entre os servicos do GeoFlow que concentra os contratos que precisam ser reutilizados por `geoflow-api`, `geoflow-processor` e `geoflow-notifier`.

## Conteudo

- DTO `AreaBounds`
- enums:
  - `EventType`
  - `JobStatus`
  - `JobType`
- eventos:
  - `JobRequestedEvent`
  - `JobCompletedEvent`
  - `JobFailedEvent`
  - `JobDeadLetterEvent`

## Objetivo

Evitar duplicacao de contratos entre servicos e manter compatibilidade entre:

- payload publicado pela API
- payload consumido pelo processor
- payloads finais observados pelo notifier

## Empacotamento

- artifact: `br.com.lucastropardi:geoflow-shared:0.0.1-SNAPSHOT`
- type: `jar`

## Dependencias

- `jackson-annotations`
- `jakarta.validation-api`

## Como instalar localmente

```bash
mvn install
```

Depois disso, os outros servicos conseguem resolver a dependencia `0.0.1-SNAPSHOT` no Maven local.

## Contratos principais

### `AreaBounds`

Representa os limites da area:

- `minLon`
- `minLat`
- `maxLon`
- `maxLat`

### `JobRequestedEvent`

Evento publicado pela API quando um job e criado. Contem:

- identificadores do evento
- `correlationId`
- data/hora da ocorrencia
- `jobId`
- `tenantId`
- `jobType`
- `area`

### `JobCompletedEvent`

Evento publicado pelo processor ao concluir com sucesso. Contem:

- identificadores do evento
- `correlationId`
- `jobId`
- `tenantId`
- `resultGeojson`

### `JobFailedEvent`

Evento publicado pelo processor quando o job falha. Contem:

- identificadores do evento
- `correlationId`
- `jobId`
- `tenantId`
- `errorMessage`
- `attemptCount`
- `maxAttempts`

### `JobDeadLetterEvent`

Evento publicado quando o job excede o limite de tentativas. Contem:

- identificadores do evento
- `correlationId`
- `jobId`
- `tenantId`
- `errorMessage`
- `attemptCount`
- `maxAttempts`
