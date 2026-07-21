package br.com.lucastropardi.geoflow.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import br.com.lucastropardi.geoflow.api.dto.CreateJobRequest;
import br.com.lucastropardi.geoflow.api.kafka.JobEventPublisher;
import br.com.lucastropardi.geoflow.api.repository.ProcessingJobLogRepository;
import br.com.lucastropardi.geoflow.api.repository.ProcessingJobRepository;
import br.com.lucastropardi.geoflow.shared.dto.AreaBounds;
import br.com.lucastropardi.geoflow.shared.enums.EventType;
import br.com.lucastropardi.geoflow.shared.enums.JobType;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobServiceTest {

    @Test
    void shouldBuildJobRequestedEventWithValidMetadata() {
        JobService jobService = new JobService(
                mock(ProcessingJobRepository.class),
                mock(ProcessingJobLogRepository.class),
                mock(JobEventPublisher.class),
                mock(IdGeneratorService.class)
        );

        CreateJobRequest request = new CreateJobRequest(
                "tenant-a",
                "Primeiro job",
                JobType.BBOX_TO_GEOJSON,
                new AreaBounds(-46.7, -23.7, -46.5, -23.5)
        );
        Long jobId = 123L;
        UUID correlationId = UUID.randomUUID();

        JobRequestedEvent event = jobService.buildJobRequestedEvent(request, jobId, correlationId);

        assertNotNull(event.eventId());
        assertEquals(EventType.JOB_REQUESTED, event.eventType());
        assertEquals(1, event.eventVersion());
        assertEquals(correlationId, event.correlationId());
        assertNotNull(event.occurredAt());
        assertEquals(jobId, event.jobId());
        assertEquals("tenant-a", event.tenantId());
        assertEquals(JobType.BBOX_TO_GEOJSON, event.jobType());
        assertEquals(request.area(), event.area());
        assertTrue(event.occurredAt().getEpochSecond() > 0);
    }
}
