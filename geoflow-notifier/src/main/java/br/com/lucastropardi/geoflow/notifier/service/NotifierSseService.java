package br.com.lucastropardi.geoflow.notifier.service;

import br.com.lucastropardi.geoflow.notifier.dto.JobNotificationStatusResponse;
import br.com.lucastropardi.geoflow.notifier.repository.JobNotificationRepository;
import br.com.lucastropardi.geoflow.shared.enums.JobStatus;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotifierSseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierSseService.class);

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emittersByJobId = new ConcurrentHashMap<>();
    private final JobNotificationRepository jobNotificationRepository;

    public NotifierSseService(JobNotificationRepository jobNotificationRepository) {
        this.jobNotificationRepository = jobNotificationRepository;
    }

    public Optional<SseEmitter> subscribe(Long jobId) {
        Optional<JobNotificationStatusResponse> statusResponse = jobNotificationRepository.findStatusById(jobId);
        if (statusResponse.isEmpty()) {
            return Optional.empty();
        }

        SseEmitter emitter = new SseEmitter(0L);
        JobNotificationStatusResponse response = statusResponse.get();

        if (isTerminal(response.status())) {
            sendAndComplete(emitter, response);
            return Optional.of(emitter);
        }

        emittersByJobId.computeIfAbsent(jobId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(jobId, emitter));
        emitter.onTimeout(() -> removeEmitter(jobId, emitter));
        emitter.onError(exception -> removeEmitter(jobId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("subscribed")
                    .data(response));
        } catch (IOException exception) {
            removeEmitter(jobId, emitter);
            emitter.completeWithError(exception);
        }

        return Optional.of(emitter);
    }

    public void notifyJobStatus(Long jobId) {
        Optional<JobNotificationStatusResponse> statusResponse = jobNotificationRepository.findStatusById(jobId);
        if (statusResponse.isEmpty()) {
            return;
        }

        List<SseEmitter> emitters = emittersByJobId.get(jobId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        JobNotificationStatusResponse response = statusResponse.get();
        for (SseEmitter emitter : emitters) {
            sendAndComplete(emitter, response);
        }
        emittersByJobId.remove(jobId);
    }

    private void sendAndComplete(SseEmitter emitter, JobNotificationStatusResponse response) {
        try {
            emitter.send(SseEmitter.event()
                    .name("job-status")
                    .data(response));
            emitter.complete();
        } catch (IOException exception) {
            LOGGER.warn("Failed to send SSE notification for job {}", response.jobId(), exception);
            emitter.completeWithError(exception);
        }
    }

    private void removeEmitter(Long jobId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByJobId.get(jobId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByJobId.remove(jobId);
        }
    }

    private boolean isTerminal(JobStatus status) {
        return status == JobStatus.COMPLETED || status == JobStatus.FAILED;
    }
}
