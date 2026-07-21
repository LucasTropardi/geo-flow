package br.com.lucastropardi.geoflow.api.dto;

public record HealthResponse(
        String service,
        String status
) {
}
