package br.com.lucastropardi.geoflow.processor.kafka;

import br.com.lucastropardi.geoflow.shared.event.JobCompletedEvent;
import br.com.lucastropardi.geoflow.shared.event.JobDeadLetterEvent;
import br.com.lucastropardi.geoflow.shared.event.JobFailedEvent;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class JobResultEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobResultEventPublisher.class);

    private final KafkaTemplate<String, JobCompletedEvent> jobCompletedEventKafkaTemplate;
    private final KafkaTemplate<String, JobFailedEvent> jobFailedEventKafkaTemplate;
    private final KafkaTemplate<String, JobRequestedEvent> jobRequestedEventKafkaTemplate;
    private final KafkaTemplate<String, JobDeadLetterEvent> jobDeadLetterEventKafkaTemplate;
    private final String jobCompletedTopic;
    private final String jobFailedTopic;
    private final String jobRequestedTopic;
    private final String jobDeadLetterTopic;

    public JobResultEventPublisher(
            KafkaTemplate<String, JobCompletedEvent> jobCompletedEventKafkaTemplate,
            KafkaTemplate<String, JobFailedEvent> jobFailedEventKafkaTemplate,
            KafkaTemplate<String, JobRequestedEvent> jobRequestedEventKafkaTemplate,
            KafkaTemplate<String, JobDeadLetterEvent> jobDeadLetterEventKafkaTemplate,
            @Value("${geoflow.kafka.topics.job-completed}") String jobCompletedTopic,
            @Value("${geoflow.kafka.topics.job-failed}") String jobFailedTopic,
            @Value("${geoflow.kafka.topics.job-requested}") String jobRequestedTopic,
            @Value("${geoflow.kafka.topics.job-dead-letter}") String jobDeadLetterTopic
    ) {
        this.jobCompletedEventKafkaTemplate = jobCompletedEventKafkaTemplate;
        this.jobFailedEventKafkaTemplate = jobFailedEventKafkaTemplate;
        this.jobRequestedEventKafkaTemplate = jobRequestedEventKafkaTemplate;
        this.jobDeadLetterEventKafkaTemplate = jobDeadLetterEventKafkaTemplate;
        this.jobCompletedTopic = jobCompletedTopic;
        this.jobFailedTopic = jobFailedTopic;
        this.jobRequestedTopic = jobRequestedTopic;
        this.jobDeadLetterTopic = jobDeadLetterTopic;
    }

    public void publishCompleted(JobCompletedEvent event) {
        SendResult<String, JobCompletedEvent> result = jobCompletedEventKafkaTemplate
                .send(jobCompletedTopic, String.valueOf(event.jobId()), event)
                .join();

        LOGGER.info(
                "Published JobCompletedEvent to topic {} for job {} at offset {}",
                jobCompletedTopic,
                event.jobId(),
                result.getRecordMetadata().offset()
        );
    }

    public void publishFailed(JobFailedEvent event) {
        SendResult<String, JobFailedEvent> result = jobFailedEventKafkaTemplate
                .send(jobFailedTopic, String.valueOf(event.jobId()), event)
                .join();

        LOGGER.info(
                "Published JobFailedEvent to topic {} for job {} at offset {}",
                jobFailedTopic,
                event.jobId(),
                result.getRecordMetadata().offset()
        );
    }

    public void republishRequested(JobRequestedEvent event) {
        SendResult<String, JobRequestedEvent> result = jobRequestedEventKafkaTemplate
                .send(jobRequestedTopic, String.valueOf(event.jobId()), event)
                .join();

        LOGGER.info(
                "Republished JobRequestedEvent to topic {} for job {} at offset {}",
                jobRequestedTopic,
                event.jobId(),
                result.getRecordMetadata().offset()
        );
    }

    public void publishDeadLetter(JobDeadLetterEvent event) {
        SendResult<String, JobDeadLetterEvent> result = jobDeadLetterEventKafkaTemplate
                .send(jobDeadLetterTopic, String.valueOf(event.jobId()), event)
                .join();

        LOGGER.info(
                "Published JobDeadLetterEvent to topic {} for job {} at offset {}",
                jobDeadLetterTopic,
                event.jobId(),
                result.getRecordMetadata().offset()
        );
    }
}
