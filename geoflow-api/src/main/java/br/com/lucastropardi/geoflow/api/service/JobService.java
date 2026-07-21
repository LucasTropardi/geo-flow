package br.com.lucastropardi.geoflow.api.service;

import br.com.lucastropardi.geoflow.api.dto.CreateJobRequest;
import br.com.lucastropardi.geoflow.api.dto.CreateJobResponse;
import br.com.lucastropardi.geoflow.api.dto.JobLogResponse;
import br.com.lucastropardi.geoflow.api.dto.JobResponse;
import br.com.lucastropardi.geoflow.api.exception.JobNotFoundException;
import br.com.lucastropardi.geoflow.api.kafka.JobEventPublisher;
import br.com.lucastropardi.geoflow.api.repository.ProcessingJobLogRepository;
import br.com.lucastropardi.geoflow.api.repository.ProcessingJobRepository;
import br.com.lucastropardi.geoflow.api.model.ProcessingJobLogRecord;
import br.com.lucastropardi.geoflow.api.model.ProcessingJobRecord;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import br.com.lucastropardi.geoflow.shared.enums.EventType;
import br.com.lucastropardi.geoflow.shared.enums.JobStatus;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private static final String PROCESSING_JOB_ENTITY = "processing_job";
    private static final String PROCESSING_JOB_LOG_ENTITY = "processing_job_log";

    private final ProcessingJobRepository processingJobRepository;
    private final ProcessingJobLogRepository processingJobLogRepository;
    private final JobEventPublisher jobEventPublisher;
    private final IdGeneratorService idGeneratorService;
    private final Clock applicationClock;

    public JobService(
            ProcessingJobRepository processingJobRepository,
            ProcessingJobLogRepository processingJobLogRepository,
            JobEventPublisher jobEventPublisher,
            IdGeneratorService idGeneratorService,
            Clock applicationClock
    ) {
        this.processingJobRepository = processingJobRepository;
        this.processingJobLogRepository = processingJobLogRepository;
        this.jobEventPublisher = jobEventPublisher;
        this.idGeneratorService = idGeneratorService;
        this.applicationClock = applicationClock;
    }

    @Transactional
    public CreateJobResponse createJob(CreateJobRequest request) {
        OffsetDateTime now = OffsetDateTime.now(applicationClock);
        long jobId = idGeneratorService.nextId(PROCESSING_JOB_ENTITY);
        UUID correlationId = UUID.randomUUID();

        ProcessingJobRecord job = new ProcessingJobRecord(
                jobId,
                request.tenantId(),
                request.name(),
                request.jobType(),
                JobStatus.PENDING,
                request.area(),
                null,
                null,
                now,
                null,
                null,
                now
        );
        processingJobRepository.insert(job);

        processingJobLogRepository.insert(new ProcessingJobLogRecord(
                idGeneratorService.nextId(PROCESSING_JOB_LOG_ENTITY),
                jobId,
                "INFO",
                "JOB_CREATED",
                "Job created and persisted by geoflow-api",
                now
        ));

        jobEventPublisher.publishJobRequested(buildJobRequestedEvent(request, jobId, correlationId));

        return new CreateJobResponse(jobId, JobStatus.PENDING, now);
    }

    JobRequestedEvent buildJobRequestedEvent(CreateJobRequest request, Long jobId, UUID correlationId) {
        return new JobRequestedEvent(
                UUID.randomUUID(),
                EventType.JOB_REQUESTED,
                1,
                correlationId,
                OffsetDateTime.now(applicationClock),
                jobId,
                request.tenantId(),
                request.jobType(),
                request.area()
        );
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(Long id) {
        ProcessingJobRecord job = processingJobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        return new JobResponse(
                job.id(),
                job.tenantId(),
                job.name(),
                job.jobType(),
                job.status(),
                job.area(),
                job.resultGeojson(),
                job.errorMessage(),
                job.createdAt(),
                job.startedAt(),
                job.finishedAt(),
                job.updatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<JobLogResponse> getJobLogs(Long id) {
        ensureExists(id);
        return processingJobLogRepository.findByJobId(id).stream()
                .map(log -> new JobLogResponse(
                        log.id(),
                        log.jobId(),
                        log.level(),
                        log.step(),
                        log.message(),
                        log.createdAt()
                ))
                .toList();
    }

    private void ensureExists(Long id) {
        if (processingJobRepository.findById(id).isEmpty()) {
            throw new JobNotFoundException(id);
        }
    }
}
