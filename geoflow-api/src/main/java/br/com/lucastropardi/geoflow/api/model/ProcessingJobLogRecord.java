package br.com.lucastropardi.geoflow.api.model;

import java.time.OffsetDateTime;

public record ProcessingJobLogRecord(
        Long id,
        Long jobId,
        String correlationId,
        String level,
        String step,
        String message,
        OffsetDateTime createdAt
) {
}
