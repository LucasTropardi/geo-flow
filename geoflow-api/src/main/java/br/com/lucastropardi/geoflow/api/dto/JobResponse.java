package br.com.lucastropardi.geoflow.api.dto;

import br.com.lucastropardi.geoflow.shared.dto.AreaBounds;
import br.com.lucastropardi.geoflow.shared.enums.JobStatus;
import br.com.lucastropardi.geoflow.shared.enums.JobType;
import java.time.OffsetDateTime;

public record JobResponse(
        Long id,
        String tenantId,
        String name,
        JobType jobType,
        JobStatus status,
        AreaBounds area,
        String resultGeojson,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime updatedAt
) {
}
