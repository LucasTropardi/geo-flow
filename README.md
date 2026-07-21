# GeoFlow

GeoFlow is an open source study project focused on asynchronous geographic job processing with a microservices-oriented architecture. The stack is centered on Java 25, Spring Boot, Apache Kafka, Apache Camel, PostgreSQL/PostGIS, Flyway, Docker Compose and React.

This first stage creates the initial foundation for the backend services and shared module, keeping the codebase simple, compilable and ready to evolve.

## Initial architecture

- `geoflow-shared`: shared Java/Maven module with enums, DTOs and Kafka event contracts
- `geoflow-api`: HTTP API for job creation and consultation
- `geoflow-processor`: background worker that consumes Kafka events with Apache Camel and writes processing results to PostgreSQL
- `geoflow-notifier`: notification service that consumes completion/failure events and notifies the browser through SSE
- `geoflow-front`: React + Vite frontend project
- `docker-compose.yml`: local infrastructure for PostgreSQL/PostGIS, Kafka and Kafka UI

## Infrastructure

Start the local infrastructure from the repository root:

```bash
docker compose up -d
```

Services and ports:

- PostgreSQL/PostGIS: `localhost:5432`
- Kafka broker for local applications: `localhost:9092`
- Kafka UI: `http://localhost:8085`
- geoflow-api: `http://localhost:8080`
- geoflow-notifier: `http://localhost:8082`

## Running the services

The backend modules are independent Maven projects. Build and install the shared module first so the Spring Boot services can resolve it locally:

```bash
cd geoflow-shared
./mvnw clean install
```

Then build each service:

```bash
cd ../geoflow-api
./mvnw clean package
```

```bash
cd ../geoflow-processor
./mvnw clean package
```

```bash
cd ../geoflow-notifier
./mvnw clean package
```

Run them locally with Maven or directly from IntelliJ:

```bash
cd geoflow-api
./mvnw spring-boot:run
```

```bash
cd geoflow-processor
./mvnw spring-boot:run
```

```bash
cd geoflow-notifier
./mvnw spring-boot:run
```

Frontend:

```bash
cd geoflow-front
npm run dev
```

## Database and persistence

GeoFlow does not use JPA or Hibernate.

- No `spring-boot-starter-data-jpa`
- No `@Entity`
- No `JpaRepository`
- No Hibernate ORM

Persistence is implemented with explicit SQL using:

- `spring-boot-starter-jdbc`
- `JdbcTemplate`
- `NamedParameterJdbcTemplate`
- Flyway migrations
- PostgreSQL driver

The first Flyway migration creates:

- `processing_job`
- `processing_job_log`

The current persistence strategy for table IDs is:

- Table IDs are `BIGINT`, not UUID
- IDs are generated in Java through the `id_generator` table
- The project still uses explicit JDBC with `JdbcTemplate` and `NamedParameterJdbcTemplate`
- The project still does not use JPA, Hibernate, `@Entity` or `JpaRepository`
- Timestamp columns now use PostgreSQL `TIMESTAMPTZ`
- Java date/time fields now use `OffsetDateTime`
- Application timestamps are standardized to `America/Sao_Paulo`

`id_generator` keeps one row per entity, for example:

- `processing_job`
- `processing_job_log`

The ID generator uses a Spring transaction plus PostgreSQL pessimistic locking:

- `SELECT ... FOR UPDATE` locks the entity row in `id_generator`
- `next_value` is incremented in the same transaction
- The job insert happens in the same transaction as the reserved ID
- Concurrent requests stay safe because the coordination happens in PostgreSQL, not with Java `synchronized`

Event tracking IDs remain separate from table IDs:

- `eventId` stays `UUID`
- `correlationId` stays `UUID`
- `jobId` in Kafka events is numeric because it mirrors the database ID

## Available endpoints in this stage

`geoflow-api`

- `GET /api/health`
- `POST /api/jobs`
- `GET /api/jobs/{id}`
- `GET /api/jobs/{id}/logs`
- Publishes `JobRequestedEvent` to Kafka topic `geoflow.job.requested`

`geoflow-notifier`

- `GET /api/health`
- `GET /notifications/jobs/{jobId}/stream`
- Consumes `geoflow.job.completed` and `geoflow.job.failed`
- Reads the official job status from PostgreSQL through JDBC before notifying the browser

`geoflow-processor`

- Consumes `JobRequestedEvent` from Kafka topic `geoflow.job.requested`
- Processes `BBOX_TO_GEOJSON` jobs
- Updates `processing_job` status from `PENDING` to `PROCESSING` and then to `COMPLETED` or `FAILED`
- Persists processing logs in `processing_job_log`
- Stores the generated GeoJSON in `processing_job.result_geojson`

## Kafka topics

Kafka topics are managed manually in this project for local studies of partitions, concurrency and async flows.

Create the topic before testing event publication:

```bash
docker exec -it geoflow-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic geoflow.job.requested \
  --partitions 3 \
  --replication-factor 1
```

```bash
docker exec -it geoflow-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic geoflow.job.completed \
  --partitions 3 \
  --replication-factor 1
```

```bash
docker exec -it geoflow-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic geoflow.job.failed \
  --partitions 3 \
  --replication-factor 1
```

Useful commands:

List topics:

```bash
docker exec -it geoflow-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
```

Describe topic:

```bash
docker exec -it geoflow-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic geoflow.job.requested
```

`JobRequestedEvent` details in the current stage:

- Topic: `geoflow.job.requested`
- Kafka message key: `String.valueOf(jobId)`
- `jobId`: numeric database ID
- `eventId`: `UUID`
- `correlationId`: `UUID`
- `occurredAt`: ISO-8601 timestamp string

Result event topics in the current stage:

- `geoflow.job.completed`
- `geoflow.job.failed`

## Processor flow

`geoflow-processor` now performs the first real asynchronous workflow end to end.

When a job is created by `geoflow-api`:

- `geoflow-api` persists the job with status `PENDING`
- `geoflow-api` publishes `JobRequestedEvent` to `geoflow.job.requested`
- `geoflow-processor` consumes the message with Apache Camel
- The processor marks the job as `PROCESSING` and writes a `PROCESSING_STARTED` log
- For `BBOX_TO_GEOJSON`, the processor validates the bounding box and converts it to a GeoJSON `Feature` with `Polygon`
- The generated GeoJSON is saved in `processing_job.result_geojson`
- The processor marks the job as `COMPLETED`, writes a `PROCESSING_COMPLETED` log and publishes `JobCompletedEvent`
- If validation or processing fails, the processor marks the job as `FAILED`, fills `error_message`, writes a `PROCESSING_FAILED` log and publishes `JobFailedEvent`
- `geoflow-notifier` consumes the terminal event and notifies any subscribed browser through SSE

Run the processor locally:

```bash
cd geoflow-processor
./mvnw spring-boot:run
```

Current processor configuration:

- Kafka topic: `geoflow.job.requested`
- Kafka consumer group: `geoflow-processor`
- Kafka bootstrap servers: `localhost:9092`
- Database result column: `processing_job.result_geojson`

## Frontend flow

The current frontend works with two main screens:

- Screen 1 submits the BBox job
- Screen 2 waits for `geoflow-notifier` through SSE and then shows the final result

Current browser flow:

- The user fills two corners of the area in the form
- The frontend normalizes min/max longitude and latitude before calling the API
- The frontend creates the job with `POST /api/jobs`
- The frontend opens an SSE connection to `GET /notifications/jobs/{jobId}/stream`
- When the notifier receives a completion/failure event, it reads the official status from PostgreSQL and pushes it to the browser
- The frontend navigates to the final result page
- The final page loads the job from `geoflow-api`, shows the GeoJSON, renders it on Leaflet and displays the processing logs

## Next steps

- Add more job types and specialized Camel routes in `geoflow-processor`
- Introduce retry, DLQ and more robust consumer concurrency controls
- Consume completion/failure events in `geoflow-notifier`
- Build the first React screens for job submission and monitoring
