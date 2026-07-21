package br.com.lucastropardi.geoflow.api.kafka;

import br.com.lucastropardi.geoflow.api.exception.JobEventPublishException;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class JobEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobEventPublisher.class);

    private final KafkaTemplate<String, JobRequestedEvent> kafkaTemplate;
    private final String jobRequestedTopic;

    public JobEventPublisher(
            KafkaTemplate<String, JobRequestedEvent> kafkaTemplate,
            @Value("${geoflow.kafka.topics.job-requested}") String jobRequestedTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.jobRequestedTopic = jobRequestedTopic;
    }

    public void publishJobRequested(JobRequestedEvent event) {
        try {
            SendResult<String, JobRequestedEvent> result = kafkaTemplate
                    .send(jobRequestedTopic, String.valueOf(event.jobId()), event)
                    .join();

            LOGGER.info(
                    "Published JobRequestedEvent to topic {} for job {} at offset {}",
                    jobRequestedTopic,
                    event.jobId(),
                    result.getRecordMetadata().offset()
            );
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to publish JobRequestedEvent for job {}", event.jobId(), exception);
            throw new JobEventPublishException(
                    "Failed to publish JobRequestedEvent for job " + event.jobId(),
                    exception
            );
        }
    }
}
