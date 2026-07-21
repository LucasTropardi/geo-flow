package br.com.lucastropardi.geoflow.processor.config;

import br.com.lucastropardi.geoflow.shared.event.JobCompletedEvent;
import br.com.lucastropardi.geoflow.shared.event.JobDeadLetterEvent;
import br.com.lucastropardi.geoflow.shared.event.JobFailedEvent;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, JobCompletedEvent> jobCompletedEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            ObjectMapper objectMapper
    ) {
        return producerFactory(bootstrapServers, objectMapper);
    }

    @Bean
    public ProducerFactory<String, JobFailedEvent> jobFailedEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            ObjectMapper objectMapper
    ) {
        return producerFactory(bootstrapServers, objectMapper);
    }

    @Bean
    public ProducerFactory<String, JobRequestedEvent> jobRequestedEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            ObjectMapper objectMapper
    ) {
        return producerFactory(bootstrapServers, objectMapper);
    }

    @Bean
    public ProducerFactory<String, JobDeadLetterEvent> jobDeadLetterEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            ObjectMapper objectMapper
    ) {
        return producerFactory(bootstrapServers, objectMapper);
    }

    @Bean
    public KafkaTemplate<String, JobCompletedEvent> jobCompletedEventKafkaTemplate(
            ProducerFactory<String, JobCompletedEvent> jobCompletedEventProducerFactory
    ) {
        return new KafkaTemplate<>(jobCompletedEventProducerFactory);
    }

    @Bean
    public KafkaTemplate<String, JobFailedEvent> jobFailedEventKafkaTemplate(
            ProducerFactory<String, JobFailedEvent> jobFailedEventProducerFactory
    ) {
        return new KafkaTemplate<>(jobFailedEventProducerFactory);
    }

    @Bean
    public KafkaTemplate<String, JobRequestedEvent> jobRequestedEventKafkaTemplate(
            ProducerFactory<String, JobRequestedEvent> jobRequestedEventProducerFactory
    ) {
        return new KafkaTemplate<>(jobRequestedEventProducerFactory);
    }

    @Bean
    public KafkaTemplate<String, JobDeadLetterEvent> jobDeadLetterEventKafkaTemplate(
            ProducerFactory<String, JobDeadLetterEvent> jobDeadLetterEventProducerFactory
    ) {
        return new KafkaTemplate<>(jobDeadLetterEventProducerFactory);
    }

    private <T> ProducerFactory<String, T> producerFactory(String bootstrapServers, ObjectMapper objectMapper) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(
                properties,
                new StringSerializer(),
                new JsonSerializer<>(objectMapper)
        );
    }
}
