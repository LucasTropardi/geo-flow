package br.com.lucastropardi.geoflow.notifier.dto;

import br.com.lucastropardi.geoflow.shared.enums.JobStatus;

public record JobNotificationStatusResponse(
        Long jobId,
        JobStatus status
) {
}
