package br.com.lucastropardi.geoflow.api.repository;

import br.com.lucastropardi.geoflow.api.model.ProcessingJobRecord;
import br.com.lucastropardi.geoflow.shared.dto.AreaBounds;
import br.com.lucastropardi.geoflow.shared.enums.JobStatus;
import br.com.lucastropardi.geoflow.shared.enums.JobType;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProcessingJobRepository {

    private static final String INSERT_SQL = """
            INSERT INTO processing_job (
                id, tenant_id, name, job_type, status,
                correlation_id, attempt_count, max_attempts,
                min_lon, min_lat, max_lon, max_lat,
                result_geojson, error_message,
                created_at, started_at, finished_at, updated_at
            ) VALUES (
                :id, :tenantId, :name, :jobType, :status,
                :correlationId, :attemptCount, :maxAttempts,
                :minLon, :minLat, :maxLon, :maxLat,
                :resultGeojson, :errorMessage,
                :createdAt, :startedAt, :finishedAt, :updatedAt
            )
            """;

    private static final String REPROCESS_SQL = """
            UPDATE processing_job
            SET status = :status,
                correlation_id = :correlationId,
                attempt_count = 0,
                result_geojson = NULL,
                error_message = NULL,
                started_at = NULL,
                finished_at = NULL,
                updated_at = :updatedAt
            WHERE id = :id
              AND status = :currentStatus
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT
                id, tenant_id, name, job_type, status,
                correlation_id, attempt_count, max_attempts,
                min_lon, min_lat, max_lon, max_lat,
                result_geojson, error_message,
                created_at, started_at, finished_at, updated_at
            FROM processing_job
            WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProcessingJobRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(ProcessingJobRecord job) {
        AreaBounds area = job.area();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", job.id())
                .addValue("tenantId", job.tenantId())
                .addValue("name", job.name())
                .addValue("jobType", job.jobType().name())
                .addValue("status", job.status().name())
                .addValue("correlationId", job.correlationId())
                .addValue("attemptCount", job.attemptCount())
                .addValue("maxAttempts", job.maxAttempts())
                .addValue("minLon", area != null ? area.minLon() : null)
                .addValue("minLat", area != null ? area.minLat() : null)
                .addValue("maxLon", area != null ? area.maxLon() : null)
                .addValue("maxLat", area != null ? area.maxLat() : null)
                .addValue("resultGeojson", job.resultGeojson())
                .addValue("errorMessage", job.errorMessage())
                .addValue("createdAt", job.createdAt())
                .addValue("startedAt", job.startedAt())
                .addValue("finishedAt", job.finishedAt())
                .addValue("updatedAt", job.updatedAt());

        jdbcTemplate.update(INSERT_SQL, params);
    }

    public int markPendingForReprocess(Long id, String correlationId, OffsetDateTime updatedAt) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", JobStatus.PENDING.name())
                .addValue("currentStatus", JobStatus.FAILED.name())
                .addValue("correlationId", correlationId)
                .addValue("updatedAt", updatedAt);

        return jdbcTemplate.update(REPROCESS_SQL, params);
    }

    public Optional<ProcessingJobRecord> findById(Long id) {
        List<ProcessingJobRecord> results = jdbcTemplate.query(FIND_BY_ID_SQL, Map.of("id", id), rowMapper());
        return results.stream().findFirst();
    }

    private RowMapper<ProcessingJobRecord> rowMapper() {
        return this::mapRow;
    }

    private ProcessingJobRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        Double minLon = decimalToDouble(rs.getBigDecimal("min_lon"));
        Double minLat = decimalToDouble(rs.getBigDecimal("min_lat"));
        Double maxLon = decimalToDouble(rs.getBigDecimal("max_lon"));
        Double maxLat = decimalToDouble(rs.getBigDecimal("max_lat"));

        AreaBounds area = null;
        if (minLon != null && minLat != null && maxLon != null && maxLat != null) {
            area = new AreaBounds(minLon, minLat, maxLon, maxLat);
        }

        return new ProcessingJobRecord(
                rs.getLong("id"),
                rs.getString("tenant_id"),
                rs.getString("name"),
                JobType.valueOf(rs.getString("job_type")),
                JobStatus.valueOf(rs.getString("status")),
                rs.getString("correlation_id"),
                rs.getInt("attempt_count"),
                rs.getInt("max_attempts"),
                area,
                rs.getString("result_geojson"),
                rs.getString("error_message"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("started_at", OffsetDateTime.class),
                rs.getObject("finished_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    private static Double decimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

}
