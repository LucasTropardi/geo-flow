package br.com.lucastropardi.geoflow.processor.camel.route;

import br.com.lucastropardi.geoflow.processor.camel.processor.JobRequestedMessageProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class JobRequestedRoute extends RouteBuilder {

    private final JobRequestedMessageProcessor jobRequestedMessageProcessor;

    public JobRequestedRoute(JobRequestedMessageProcessor jobRequestedMessageProcessor) {
        this.jobRequestedMessageProcessor = jobRequestedMessageProcessor;
    }

    @Override
    public void configure() {
        onException(Exception.class)
                .handled(true)
                .log("Failed to process job requested message: ${exception.message}")
                .bean(jobRequestedMessageProcessor, "handleFailure");

        from("kafka:{{geoflow.kafka.topics.job-requested}}"
                + "?brokers={{spring.kafka.bootstrap-servers}}"
                + "&groupId=geoflow-processor"
                + "&valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer")
                .routeId("job-requested-route")
                .convertBodyTo(String.class)
                .process(jobRequestedMessageProcessor);
    }
}
