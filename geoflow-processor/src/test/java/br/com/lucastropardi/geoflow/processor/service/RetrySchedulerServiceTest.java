package br.com.lucastropardi.geoflow.processor.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.lucastropardi.geoflow.processor.kafka.JobResultEventPublisher;
import br.com.lucastropardi.geoflow.processor.repository.ProcessingJobLogRepository;
import br.com.lucastropardi.geoflow.shared.dto.AreaBounds;
import br.com.lucastropardi.geoflow.shared.enums.EventType;
import br.com.lucastropardi.geoflow.shared.enums.JobType;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

class RetrySchedulerServiceTest {

    @Test
    void shouldDispatchRetryAfterConfiguredDelay() {
        ProcessingJobLogRepository processingJobLogRepository = mock(ProcessingJobLogRepository.class);
        IdGeneratorService idGeneratorService = mock(IdGeneratorService.class);
        JobResultEventPublisher jobResultEventPublisher = mock(JobResultEventPublisher.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC);
        RetrySchedulerService service = new RetrySchedulerService(
                taskScheduler,
                jobResultEventPublisher,
                processingJobLogRepository,
                idGeneratorService,
                fixedClock,
                Duration.ofSeconds(5)
        );

        when(idGeneratorService.nextId("processing_job_log")).thenReturn(10L);

        JobRequestedEvent event = new JobRequestedEvent(
                UUID.randomUUID(),
                EventType.JOB_REQUESTED,
                1,
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-07-21T09:00:00-03:00"),
                77L,
                "tenant-a",
                JobType.BBOX_TO_GEOJSON,
                new AreaBounds(-46.7, -23.7, -46.5, -23.5)
        );

        service.schedule(event, 1, 3);

        verify(processingJobLogRepository).insertLog(
                eq(10L),
                eq(event.jobId()),
                eq(event.correlationId().toString()),
                eq("INFO"),
                eq("RETRY_DISPATCHED"),
                any(),
                any()
        );
        verify(jobResultEventPublisher).republishRequested(event);
    }
}
