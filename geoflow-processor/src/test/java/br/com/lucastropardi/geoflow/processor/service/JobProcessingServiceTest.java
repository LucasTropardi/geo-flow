package br.com.lucastropardi.geoflow.processor.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.lucastropardi.geoflow.processor.kafka.JobResultEventPublisher;
import br.com.lucastropardi.geoflow.processor.model.ProcessingJobRecord;
import br.com.lucastropardi.geoflow.processor.repository.ProcessingJobLogRepository;
import br.com.lucastropardi.geoflow.processor.repository.ProcessingJobRepository;
import br.com.lucastropardi.geoflow.shared.dto.AreaBounds;
import br.com.lucastropardi.geoflow.shared.enums.JobStatus;
import br.com.lucastropardi.geoflow.shared.enums.JobType;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

class JobProcessingServiceTest {

    @Test
    void shouldScheduleRetryWhenAttemptsAreStillAvailable() {
        ProcessingJobRepository processingJobRepository = mock(ProcessingJobRepository.class);
        ProcessingJobLogRepository processingJobLogRepository = mock(ProcessingJobLogRepository.class);
        IdGeneratorService idGeneratorService = mock(IdGeneratorService.class);
        GeoJsonService geoJsonService = mock(GeoJsonService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        JobResultEventPublisher jobResultEventPublisher = mock(JobResultEventPublisher.class);
        RetrySchedulerService retrySchedulerService = mock(RetrySchedulerService.class);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC);

        JobProcessingService service = new JobProcessingService(
                processingJobRepository,
                processingJobLogRepository,
                idGeneratorService,
                geoJsonService,
                transactionTemplate,
                fixedClock,
                jobResultEventPublisher,
                retrySchedulerService
        );

        JobRequestedEvent event = jobRequestedEvent();
        ProcessingJobRecord processingJob = processingJobRecord(JobStatus.PROCESSING, 1, 3, "erro");
        ProcessingJobRecord retryPendingJob = processingJobRecord(JobStatus.RETRY_PENDING, 1, 3, "erro");

        when(processingJobRepository.findById(event.jobId())).thenReturn(Optional.of(processingJob), Optional.of(retryPendingJob));
        when(processingJobRepository.markRetryPending(eq(event.jobId()), eq("erro"), any())).thenReturn(1);
        when(idGeneratorService.nextId("processing_job_log")).thenReturn(1L, 2L);

        service.markFailed(event, "erro");

        verify(processingJobRepository).markRetryPending(eq(event.jobId()), eq("erro"), any());
        verify(processingJobRepository, never()).markFailed(any(), any(), any(), any());
        verify(retrySchedulerService).schedule(event, 1, 3);
        verify(jobResultEventPublisher, never()).publishFailed(any());
        verify(jobResultEventPublisher, never()).publishDeadLetter(any());
    }

    @Test
    void shouldPublishFailedAndDeadLetterWhenAttemptsAreExhausted() {
        ProcessingJobRepository processingJobRepository = mock(ProcessingJobRepository.class);
        ProcessingJobLogRepository processingJobLogRepository = mock(ProcessingJobLogRepository.class);
        IdGeneratorService idGeneratorService = mock(IdGeneratorService.class);
        GeoJsonService geoJsonService = mock(GeoJsonService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        JobResultEventPublisher jobResultEventPublisher = mock(JobResultEventPublisher.class);
        RetrySchedulerService retrySchedulerService = mock(RetrySchedulerService.class);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC);

        JobProcessingService service = new JobProcessingService(
                processingJobRepository,
                processingJobLogRepository,
                idGeneratorService,
                geoJsonService,
                transactionTemplate,
                fixedClock,
                jobResultEventPublisher,
                retrySchedulerService
        );

        JobRequestedEvent event = jobRequestedEvent();
        ProcessingJobRecord processingJob = processingJobRecord(JobStatus.PROCESSING, 3, 3, "erro final");
        ProcessingJobRecord failedJob = processingJobRecord(JobStatus.FAILED, 3, 3, "erro final");

        when(processingJobRepository.findById(event.jobId())).thenReturn(Optional.of(processingJob), Optional.of(failedJob));
        when(processingJobRepository.markFailed(eq(event.jobId()), eq("erro final"), any(), any())).thenReturn(1);
        when(idGeneratorService.nextId("processing_job_log")).thenReturn(1L);

        service.markFailed(event, "erro final");

        verify(processingJobRepository).markFailed(eq(event.jobId()), eq("erro final"), any(), any());
        verify(processingJobRepository, never()).markRetryPending(any(), any(), any());
        verify(jobResultEventPublisher).publishFailed(any());
        verify(jobResultEventPublisher).publishDeadLetter(any());
        verify(retrySchedulerService, never()).schedule(any(), any(Integer.class), any(Integer.class));
    }

    private static JobRequestedEvent jobRequestedEvent() {
        return new JobRequestedEvent(
                UUID.randomUUID(),
                br.com.lucastropardi.geoflow.shared.enums.EventType.JOB_REQUESTED,
                1,
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-07-21T09:00:00-03:00"),
                77L,
                "tenant-a",
                JobType.BBOX_TO_GEOJSON,
                new AreaBounds(-46.7, -23.7, -46.5, -23.5)
        );
    }

    private static ProcessingJobRecord processingJobRecord(JobStatus status, int attemptCount, int maxAttempts, String errorMessage) {
        return new ProcessingJobRecord(
                77L,
                "tenant-a",
                "job",
                JobType.BBOX_TO_GEOJSON,
                status,
                "corr-id",
                attemptCount,
                maxAttempts,
                new AreaBounds(-46.7, -23.7, -46.5, -23.5),
                null,
                errorMessage,
                OffsetDateTime.parse("2026-07-21T09:00:00-03:00"),
                OffsetDateTime.parse("2026-07-21T09:00:05-03:00"),
                null,
                OffsetDateTime.parse("2026-07-21T09:00:05-03:00")
        );
    }
}
