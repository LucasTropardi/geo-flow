CREATE TABLE IF NOT EXISTS processing_job (
    id UUID PRIMARY KEY,
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

CREATE TABLE IF NOT EXISTS processing_job_log (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES processing_job(id),
    level VARCHAR(20) NOT NULL,
    step VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_processing_job_status
    ON processing_job (status);

CREATE INDEX IF NOT EXISTS idx_processing_job_log_job_id
    ON processing_job_log (job_id);
