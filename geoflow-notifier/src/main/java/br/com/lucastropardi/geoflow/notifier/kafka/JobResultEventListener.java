package br.com.lucastropardi.geoflow.notifier.kafka;

import br.com.lucastropardi.geoflow.notifier.service.NotifierSseService;
import br.com.lucastropardi.geoflow.shared.event.JobCompletedEvent;
import br.com.lucastropardi.geoflow.shared.event.JobFailedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobResultEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobResultEventListener.class);

    private final ObjectMapper objectMapper;
    private final NotifierSseService notifierSseService;

    public JobResultEventListener(ObjectMapper objectMapper, NotifierSseService notifierSseService) {
        this.objectMapper = objectMapper;
        this.notifierSseService = notifierSseService;
    }

    @KafkaListener(
            topics = "${geoflow.kafka.topics.job-completed}",
            groupId = "geoflow-notifier-completed"
    )
    public void onJobCompleted(String message) throws JsonProcessingException {
        JobCompletedEvent event = objectMapper.readValue(message, JobCompletedEvent.class);
        LOGGER.info(
                "Received JobCompletedEvent for job {} with correlationId {}",
                event.jobId(),
                event.correlationId()
        );
        notifierSseService.notifyJobStatus(event.jobId());
    }

    @KafkaListener(
            topics = "${geoflow.kafka.topics.job-failed}",
            groupId = "geoflow-notifier-failed"
    )
    public void onJobFailed(String message) throws JsonProcessingException {
        JobFailedEvent event = objectMapper.readValue(message, JobFailedEvent.class);
        LOGGER.info(
                "Received JobFailedEvent for job {} with correlationId {} after {}/{} attempts",
                event.jobId(),
                event.correlationId(),
                event.attemptCount(),
                event.maxAttempts()
        );
        notifierSseService.notifyJobStatus(event.jobId());
    }
}
