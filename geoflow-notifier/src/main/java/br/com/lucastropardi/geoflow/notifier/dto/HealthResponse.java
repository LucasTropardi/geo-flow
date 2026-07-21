package br.com.lucastropardi.geoflow.notifier.dto;

public record HealthResponse(
        String service,
        String status
) {
}
