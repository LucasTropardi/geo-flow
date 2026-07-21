DROP TABLE IF EXISTS processing_job_log;
DROP TABLE IF EXISTS processing_job;
DROP TABLE IF EXISTS id_generator;

CREATE TABLE processing_job (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    min_lon NUMERIC(12, 8),
    min_lat NUMERIC(12, 8),
    max_lon NUMERIC(12, 8),
    max_lat NUMERIC(12, 8),
    result_geojson TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE processing_job_log (
    id BIGINT PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES processing_job(id),
    level VARCHAR(20) NOT NULL,
    step VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE id_generator (
    entity_name VARCHAR(100) PRIMARY KEY,
    next_value BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_processing_job_status
    ON processing_job (status);

CREATE INDEX idx_processing_job_log_job_id
    ON processing_job_log (job_id);

INSERT INTO id_generator (entity_name, next_value, updated_at)
VALUES
    ('processing_job', COALESCE((SELECT MAX(id) + 1 FROM processing_job), 1), CURRENT_TIMESTAMP),
    ('processing_job_log', COALESCE((SELECT MAX(id) + 1 FROM processing_job_log), 1), CURRENT_TIMESTAMP);
