package br.com.lucastropardi.geoflow.api.dto;

import java.time.OffsetDateTime;

public record JobLogResponse(
        Long id,
        Long jobId,
        String level,
        String step,
        String message,
        OffsetDateTime createdAt
) {
}
