package br.com.lucastropardi.geoflow.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.lucastropardi.geoflow.api.dto.CreateJobRequest;
import br.com.lucastropardi.geoflow.api.kafka.JobEventPublisher;
import br.com.lucastropardi.geoflow.api.model.ProcessingJobRecord;
import br.com.lucastropardi.geoflow.api.repository.ProcessingJobLogRepository;
import br.com.lucastropardi.geoflow.api.repository.ProcessingJobRepository;
import br.com.lucastropardi.geoflow.shared.dto.AreaBounds;
import br.com.lucastropardi.geoflow.shared.enums.EventType;
import br.com.lucastropardi.geoflow.shared.enums.JobStatus;
import br.com.lucastropardi.geoflow.shared.enums.JobType;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobServiceTest {

    @Test
    void shouldBuildJobRequestedEventWithValidMetadata() {
        JobService jobService = new JobService(
                mock(ProcessingJobRepository.class),
                mock(ProcessingJobLogRepository.class),
                mock(JobEventPublisher.class),
                mock(IdGeneratorService.class),
                Clock.systemUTC()
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
        assertTrue(event.occurredAt().toEpochSecond() > 0);
    }

    @Test
    void shouldReprocessFailedJobWithNewCorrelationId() {
        ProcessingJobRepository processingJobRepository = mock(ProcessingJobRepository.class);
        ProcessingJobLogRepository processingJobLogRepository = mock(ProcessingJobLogRepository.class);
        JobEventPublisher jobEventPublisher = mock(JobEventPublisher.class);
        IdGeneratorService idGeneratorService = mock(IdGeneratorService.class);

        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC);
        JobService jobService = new JobService(
                processingJobRepository,
                processingJobLogRepository,
                jobEventPublisher,
                idGeneratorService,
                fixedClock
        );

        ProcessingJobRecord failedJob = new ProcessingJobRecord(
                99L,
                "tenant-a",
                "Job com falha",
                JobType.BBOX_TO_GEOJSON,
                JobStatus.FAILED,
                "old-correlation",
                3,
                3,
                new AreaBounds(-46.7, -23.7, -46.5, -23.5),
                null,
                "boom",
                OffsetDateTime.now(fixedClock),
                OffsetDateTime.now(fixedClock),
                OffsetDateTime.now(fixedClock),
                OffsetDateTime.now(fixedClock)
        );

        ProcessingJobRecord reprocessedJob = new ProcessingJobRecord(
                99L,
                "tenant-a",
                "Job com falha",
                JobType.BBOX_TO_GEOJSON,
                JobStatus.PENDING,
                "new-correlation",
                0,
                3,
                new AreaBounds(-46.7, -23.7, -46.5, -23.5),
                null,
                null,
                OffsetDateTime.now(fixedClock),
                null,
                null,
                OffsetDateTime.now(fixedClock)
        );

        when(processingJobRepository.findById(99L)).thenReturn(Optional.of(failedJob), Optional.of(reprocessedJob));
        when(processingJobRepository.markPendingForReprocess(any(), any(), any())).thenReturn(1);
        when(idGeneratorService.nextId("processing_job_log")).thenReturn(1000L);

        var response = jobService.reprocessJob(99L);

        assertEquals(JobStatus.PENDING, response.status());
        assertEquals(0, response.attemptCount());
        assertEquals(3, response.maxAttempts());
        verify(processingJobRepository).markPendingForReprocess(any(), any(), any());
        verify(processingJobLogRepository).insert(any());
        verify(jobEventPublisher).publishJobRequested(any());
    }
}
