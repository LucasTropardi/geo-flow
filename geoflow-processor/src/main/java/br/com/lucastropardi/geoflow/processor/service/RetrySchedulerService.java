package br.com.lucastropardi.geoflow.processor.service;

import br.com.lucastropardi.geoflow.processor.kafka.JobResultEventPublisher;
import br.com.lucastropardi.geoflow.processor.repository.ProcessingJobLogRepository;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class RetrySchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetrySchedulerService.class);
    private static final String PROCESSING_JOB_LOG_ENTITY = "processing_job_log";

    private final TaskScheduler taskScheduler;
    private final JobResultEventPublisher jobResultEventPublisher;
    private final ProcessingJobLogRepository processingJobLogRepository;
    private final IdGeneratorService idGeneratorService;
    private final Clock applicationClock;
    private final Duration retryDelay;

    public RetrySchedulerService(
            TaskScheduler taskScheduler,
            JobResultEventPublisher jobResultEventPublisher,
            ProcessingJobLogRepository processingJobLogRepository,
            IdGeneratorService idGeneratorService,
            Clock applicationClock,
            @Value("${geoflow.processing.retry-delay:PT5S}") Duration retryDelay
    ) {
        this.taskScheduler = taskScheduler;
        this.jobResultEventPublisher = jobResultEventPublisher;
        this.processingJobLogRepository = processingJobLogRepository;
        this.idGeneratorService = idGeneratorService;
        this.applicationClock = applicationClock;
        this.retryDelay = retryDelay;
    }

    public void schedule(JobRequestedEvent event, int attemptCount, int maxAttempts) {
        OffsetDateTime scheduledAt = OffsetDateTime.now(applicationClock).plus(retryDelay);

        taskScheduler.schedule(
                () -> {
                    OffsetDateTime dispatchedAt = OffsetDateTime.now(applicationClock);
                    processingJobLogRepository.insertLog(
                            idGeneratorService.nextId(PROCESSING_JOB_LOG_ENTITY),
                            event.jobId(),
                            event.correlationId().toString(),
                            "INFO",
                            "RETRY_DISPATCHED",
                            "Dispatching retry attempt " + (attemptCount + 1) + "/" + maxAttempts
                                    + " after waiting " + retryDelay,
                            dispatchedAt
                    );

                    jobResultEventPublisher.republishRequested(event);

                    LOGGER.info(
                            "Retry dispatched for job {} with correlationId {} as attempt {}/{}",
                            event.jobId(),
                            event.correlationId(),
                            attemptCount + 1,
                            maxAttempts
                    );
                },
                scheduledAt.toInstant()
        );

        LOGGER.warn(
                "Retry scheduled for job {} with correlationId {} at {} after attempt {}/{}",
                event.jobId(),
                event.correlationId(),
                scheduledAt,
                attemptCount,
                maxAttempts
        );
    }
}
