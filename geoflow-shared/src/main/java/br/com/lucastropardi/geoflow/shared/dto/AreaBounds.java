package br.com.lucastropardi.geoflow.shared.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AreaBounds(
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double minLon,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double minLat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double maxLon,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double maxLat
) {
}
