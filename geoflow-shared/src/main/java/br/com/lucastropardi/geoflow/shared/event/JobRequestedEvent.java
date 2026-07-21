package br.com.lucastropardi.geoflow.shared.event;

import br.com.lucastropardi.geoflow.shared.dto.AreaBounds;
import br.com.lucastropardi.geoflow.shared.enums.EventType;
import br.com.lucastropardi.geoflow.shared.enums.JobType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record JobRequestedEvent(
        @NotNull UUID eventId,
        @NotNull EventType eventType,
        int eventVersion,
        @NotNull UUID correlationId,
        @NotNull OffsetDateTime occurredAt,
        @NotNull Long jobId,
        @jakarta.validation.constraints.NotBlank String tenantId,
        @NotNull JobType jobType,
        @NotNull @Valid AreaBounds area
) {
}
