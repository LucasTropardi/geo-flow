package br.com.lucastropardi.geoflow.api.model;

import br.com.lucastropardi.geoflow.shared.dto.AreaBounds;
import br.com.lucastropardi.geoflow.shared.enums.JobStatus;
import br.com.lucastropardi.geoflow.shared.enums.JobType;
import java.time.OffsetDateTime;

public record ProcessingJobRecord(
        Long id,
        String tenantId,
        String name,
        JobType jobType,
        JobStatus status,
        String correlationId,
        Integer attemptCount,
        Integer maxAttempts,
        AreaBounds area,
        String resultGeojson,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime updatedAt
) {
}
