package br.com.lucastropardi.geoflow.processor.camel.processor;

import br.com.lucastropardi.geoflow.processor.service.JobProcessingService;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JobRequestedMessageProcessor implements Processor {

    public static final String JOB_REQUESTED_EVENT_PROPERTY = "jobRequestedEvent";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRequestedMessageProcessor.class);

    private final ObjectMapper objectMapper;
    private final JobProcessingService jobProcessingService;

    public JobRequestedMessageProcessor(ObjectMapper objectMapper, JobProcessingService jobProcessingService) {
        this.objectMapper = objectMapper;
        this.jobProcessingService = jobProcessingService;
    }

    @Override
    public void process(Exchange exchange) throws JsonProcessingException {
        String rawMessage = exchange.getIn().getBody(String.class);
        JobRequestedEvent event = objectMapper.readValue(rawMessage, JobRequestedEvent.class);

        exchange.setProperty(JOB_REQUESTED_EVENT_PROPERTY, event);
        LOGGER.info("Received JobRequestedEvent for job {}", event.jobId());

        jobProcessingService.process(event);
    }

    public void handleFailure(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        JobRequestedEvent event = exchange.getProperty(JOB_REQUESTED_EVENT_PROPERTY, JobRequestedEvent.class);

        if (event == null) {
            LOGGER.error("Failed to process Kafka message before event deserialization", exception);
            return;
        }

        String errorMessage = exception != null && exception.getMessage() != null
                ? exception.getMessage()
                : "Unexpected processor error";

        try {
            jobProcessingService.markFailed(event, errorMessage);
        } catch (Exception failureException) {
            LOGGER.error("Failed while marking job {} as FAILED", event.jobId(), failureException);
        }
    }
}
