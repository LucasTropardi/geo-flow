package br.com.lucastropardi.geoflow.api.dto;

import br.com.lucastropardi.geoflow.shared.dto.AreaBounds;
import br.com.lucastropardi.geoflow.shared.enums.JobType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateJobRequest(
        @NotBlank String tenantId,
        @NotBlank String name,
        @NotNull JobType jobType,
        @Valid AreaBounds area
) {
}
