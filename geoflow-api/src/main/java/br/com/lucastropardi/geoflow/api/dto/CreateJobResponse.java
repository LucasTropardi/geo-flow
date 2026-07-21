package br.com.lucastropardi.geoflow.api.dto;

import br.com.lucastropardi.geoflow.shared.enums.JobStatus;
import java.time.OffsetDateTime;

public record CreateJobResponse(
        Long id,
        JobStatus status,
        String correlationId,
        Integer attemptCount,
        Integer maxAttempts,
        OffsetDateTime createdAt
) {
}
