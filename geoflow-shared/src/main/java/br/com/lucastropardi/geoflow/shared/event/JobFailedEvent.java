package br.com.lucastropardi.geoflow.shared.event;

import br.com.lucastropardi.geoflow.shared.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record JobFailedEvent(
        @NotNull UUID eventId,
        @NotNull EventType eventType,
        @NotBlank String eventVersion,
        @NotBlank String correlationId,
        @NotNull OffsetDateTime occurredAt,
        @NotNull Long jobId,
        @NotBlank String tenantId,
        @NotBlank String errorMessage
) {
}
