package br.com.lucastropardi.geoflow.processor.service;

import br.com.lucastropardi.geoflow.processor.exception.ProcessorException;
import br.com.lucastropardi.geoflow.processor.kafka.JobResultEventPublisher;
import br.com.lucastropardi.geoflow.processor.repository.ProcessingJobLogRepository;
import br.com.lucastropardi.geoflow.processor.repository.ProcessingJobRepository;
import br.com.lucastropardi.geoflow.shared.enums.EventType;
import br.com.lucastropardi.geoflow.shared.event.JobCompletedEvent;
import br.com.lucastropardi.geoflow.shared.event.JobFailedEvent;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import br.com.lucastropardi.geoflow.shared.enums.JobType;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class JobProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobProcessingService.class);
    private static final String PROCESSING_JOB_LOG_ENTITY = "processing_job_log";

    private final ProcessingJobRepository processingJobRepository;
    private final ProcessingJobLogRepository processingJobLogRepository;
    private final IdGeneratorService idGeneratorService;
    private final GeoJsonService geoJsonService;
    private final TransactionTemplate transactionTemplate;
    private final Clock applicationClock;
    private final JobResultEventPublisher jobResultEventPublisher;

    public JobProcessingService(
            ProcessingJobRepository processingJobRepository,
            ProcessingJobLogRepository processingJobLogRepository,
            IdGeneratorService idGeneratorService,
            GeoJsonService geoJsonService,
            TransactionTemplate transactionTemplate,
            Clock applicationClock,
            JobResultEventPublisher jobResultEventPublisher
    ) {
        this.processingJobRepository = processingJobRepository;
        this.processingJobLogRepository = processingJobLogRepository;
        this.idGeneratorService = idGeneratorService;
        this.geoJsonService = geoJsonService;
        this.transactionTemplate = transactionTemplate;
        this.applicationClock = applicationClock;
        this.jobResultEventPublisher = jobResultEventPublisher;
    }

    public void process(JobRequestedEvent event) {
        if (event.jobType() != JobType.BBOX_TO_GEOJSON) {
            throw new ProcessorException("Unsupported job type: " + event.jobType());
        }

        OffsetDateTime now = OffsetDateTime.now(applicationClock);
        transactionTemplate.executeWithoutResult(status -> markProcessing(event, now));

        String geoJson = geoJsonService.buildPolygonFeature(event);
        OffsetDateTime finishedAt = OffsetDateTime.now(applicationClock);

        transactionTemplate.executeWithoutResult(status -> markCompleted(event, geoJson, finishedAt));
        publishCompletedEvent(event, geoJson, finishedAt);

        LOGGER.info("Job {} processed successfully", event.jobId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(JobRequestedEvent event, String errorMessage) {
        OffsetDateTime finishedAt = OffsetDateTime.now(applicationClock);
        int updatedRows = processingJobRepository.markFailed(event.jobId(), errorMessage, finishedAt, finishedAt);

        if (updatedRows == 0) {
            LOGGER.error("Failed to mark job {} as FAILED because it was not found. Cause: {}", event.jobId(), errorMessage);
            return;
        }

        processingJobLogRepository.insertLog(
                idGeneratorService.nextId(PROCESSING_JOB_LOG_ENTITY),
                event.jobId(),
                "ERROR",
                "PROCESSING_FAILED",
                errorMessage,
                finishedAt
        );

        publishFailedEvent(event, errorMessage, finishedAt);
        LOGGER.error("Job {} failed: {}", event.jobId(), errorMessage);
    }

    private void markProcessing(JobRequestedEvent event, OffsetDateTime startedAt) {
        int updatedRows = processingJobRepository.markProcessing(event.jobId(), startedAt, startedAt);
        if (updatedRows == 0) {
            throw new ProcessorException("Processing job " + event.jobId() + " was not found");
        }

        processingJobLogRepository.insertLog(
                idGeneratorService.nextId(PROCESSING_JOB_LOG_ENTITY),
                event.jobId(),
                "INFO",
                "PROCESSING_STARTED",
                "Started BBOX_TO_GEOJSON processing for job " + event.jobId(),
                startedAt
        );
    }

    private void markCompleted(JobRequestedEvent event, String geoJson, OffsetDateTime finishedAt) {
        int updatedRows = processingJobRepository.markCompleted(event.jobId(), geoJson, finishedAt, finishedAt);
        if (updatedRows == 0) {
            throw new ProcessorException("Processing job " + event.jobId() + " was not found during completion");
        }

        processingJobLogRepository.insertLog(
                idGeneratorService.nextId(PROCESSING_JOB_LOG_ENTITY),
                event.jobId(),
                "INFO",
                "PROCESSING_COMPLETED",
                "Completed BBOX_TO_GEOJSON processing for job " + event.jobId(),
                finishedAt
        );
    }

    private void publishCompletedEvent(JobRequestedEvent event, String geoJson, OffsetDateTime occurredAt) {
        try {
            jobResultEventPublisher.publishCompleted(new JobCompletedEvent(
                    UUID.randomUUID(),
                    EventType.JOB_COMPLETED,
                    "1",
                    event.correlationId().toString(),
                    occurredAt,
                    event.jobId(),
                    event.tenantId(),
                    geoJson
            ));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to publish JobCompletedEvent for job {}", event.jobId(), exception);
        }
    }

    private void publishFailedEvent(JobRequestedEvent event, String errorMessage, OffsetDateTime occurredAt) {
        try {
            jobResultEventPublisher.publishFailed(new JobFailedEvent(
                    UUID.randomUUID(),
                    EventType.JOB_FAILED,
                    "1",
                    event.correlationId().toString(),
                    occurredAt,
                    event.jobId(),
                    event.tenantId(),
                    errorMessage
            ));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to publish JobFailedEvent for job {}", event.jobId(), exception);
        }
    }
}
