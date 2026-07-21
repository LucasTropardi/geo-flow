package br.com.lucastropardi.geoflow.processor.service;

import br.com.lucastropardi.geoflow.processor.exception.ProcessorException;
import br.com.lucastropardi.geoflow.shared.dto.AreaBounds;
import br.com.lucastropardi.geoflow.shared.event.JobRequestedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GeoJsonService {

    private final ObjectMapper objectMapper;

    public GeoJsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildPolygonFeature(JobRequestedEvent event) {
        AreaBounds area = event.area();
        validate(area);

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("geometry", Map.of(
                "type", "Polygon",
                "coordinates", List.of(List.of(
                        List.of(area.minLon(), area.minLat()),
                        List.of(area.maxLon(), area.minLat()),
                        List.of(area.maxLon(), area.maxLat()),
                        List.of(area.minLon(), area.maxLat()),
                        List.of(area.minLon(), area.minLat())
                ))
        ));
        feature.put("properties", Map.of(
                "jobId", event.jobId(),
                "tenantId", event.tenantId(),
                "jobType", event.jobType().name()
        ));

        try {
            return objectMapper.writeValueAsString(feature);
        } catch (JsonProcessingException exception) {
            throw new ProcessorException("Failed to serialize GeoJSON result", exception);
        }
    }

    private void validate(AreaBounds area) {
        if (area == null) {
            throw new ProcessorException("Area bounds are required");
        }
        validateLongitude(area.minLon(), "minLon");
        validateLongitude(area.maxLon(), "maxLon");
        validateLatitude(area.minLat(), "minLat");
        validateLatitude(area.maxLat(), "maxLat");

        if (area.minLon() >= area.maxLon()) {
            throw new ProcessorException("minLon must be less than maxLon");
        }
        if (area.minLat() >= area.maxLat()) {
            throw new ProcessorException("minLat must be less than maxLat");
        }
    }

    private void validateLongitude(Double value, String fieldName) {
        if (value == null || value < -180.0 || value > 180.0) {
            throw new ProcessorException(fieldName + " must be between -180 and 180");
        }
    }

    private void validateLatitude(Double value, String fieldName) {
        if (value == null || value < -90.0 || value > 90.0) {
            throw new ProcessorException(fieldName + " must be between -90 and 90");
        }
    }
}
