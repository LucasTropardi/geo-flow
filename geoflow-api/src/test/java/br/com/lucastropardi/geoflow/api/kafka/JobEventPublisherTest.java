package br.com.lucastropardi.geoflow.api.kafka;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.lucastropardi.geoflow.shared.dto.AreaBounds;
import br.com.lucastropardi.geoflow.shared.enums.EventType;
import br.com.lucastropardi.geoflow.shared.enums.JobType;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.apache.kafka.clients.producer.RecordMetadata;

@ExtendWith(MockitoExtension.class)
class JobEventPublisherTest {

    @Mock
    private KafkaTemplate<String, JobRequestedEvent> kafkaTemplate;

    @Test
    void shouldUseJobIdAsKafkaMessageKey() {
        JobRequestedEvent event = new JobRequestedEvent(
                UUID.randomUUID(),
                EventType.JOB_REQUESTED,
                1,
                UUID.randomUUID(),
                OffsetDateTime.now(),
                42L,
                "tenant-a",
                JobType.BBOX_TO_GEOJSON,
                new AreaBounds(-46.7, -23.7, -46.5, -23.5)
        );

        JobEventPublisher publisher = new JobEventPublisher(kafkaTemplate, "geoflow.job.requested");
        SendResult<String, JobRequestedEvent> sendResult = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(metadata.offset()).thenReturn(7L);
        when(kafkaTemplate.send(
                eq("geoflow.job.requested"),
                eq(String.valueOf(event.jobId())),
                eq(event)
        )).thenReturn(CompletableFuture.completedFuture(sendResult));

        publisher.publishJobRequested(event);

        verify(kafkaTemplate).send(
                eq("geoflow.job.requested"),
                eq(String.valueOf(event.jobId())),
                eq(event)
        );
    }
}
