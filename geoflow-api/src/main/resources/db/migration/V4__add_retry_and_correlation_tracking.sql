ALTER TABLE processing_job
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(36),
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_attempts INTEGER NOT NULL DEFAULT 3;

ALTER TABLE processing_job_log
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(36);

CREATE INDEX IF NOT EXISTS idx_processing_job_correlation_id
    ON processing_job (correlation_id);
