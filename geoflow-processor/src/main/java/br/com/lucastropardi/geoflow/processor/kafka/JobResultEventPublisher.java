package br.com.lucastropardi.geoflow.processor.kafka;

import br.com.lucastropardi.geoflow.shared.event.JobCompletedEvent;
import br.com.lucastropardi.geoflow.shared.event.JobFailedEvent;
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
    private final String jobCompletedTopic;
    private final String jobFailedTopic;

    public JobResultEventPublisher(
            KafkaTemplate<String, JobCompletedEvent> jobCompletedEventKafkaTemplate,
            KafkaTemplate<String, JobFailedEvent> jobFailedEventKafkaTemplate,
            @Value("${geoflow.kafka.topics.job-completed}") String jobCompletedTopic,
            @Value("${geoflow.kafka.topics.job-failed}") String jobFailedTopic
    ) {
        this.jobCompletedEventKafkaTemplate = jobCompletedEventKafkaTemplate;
        this.jobFailedEventKafkaTemplate = jobFailedEventKafkaTemplate;
        this.jobCompletedTopic = jobCompletedTopic;
        this.jobFailedTopic = jobFailedTopic;
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
}
