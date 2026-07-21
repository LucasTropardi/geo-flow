package br.com.lucastropardi.geoflow.processor.repository;

import br.com.lucastropardi.geoflow.processor.model.ProcessingJobRecord;
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

    private static final String MARK_PROCESSING_SQL = """
            UPDATE processing_job
            SET status = :status,
                started_at = COALESCE(started_at, :startedAt),
                updated_at = :updatedAt
            WHERE id = :id
            """;

    private static final String MARK_COMPLETED_SQL = """
            UPDATE processing_job
            SET status = :status,
                result_geojson = :resultGeojson,
                error_message = NULL,
                finished_at = :finishedAt,
                updated_at = :updatedAt
            WHERE id = :id
            """;

    private static final String MARK_FAILED_SQL = """
            UPDATE processing_job
            SET status = :status,
                error_message = :errorMessage,
                finished_at = :finishedAt,
                updated_at = :updatedAt
            WHERE id = :id
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT
                id, tenant_id, name, job_type, status,
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

    public int markProcessing(Long id, OffsetDateTime startedAt, OffsetDateTime updatedAt) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", JobStatus.PROCESSING.name())
                .addValue("startedAt", startedAt)
                .addValue("updatedAt", updatedAt);

        return jdbcTemplate.update(MARK_PROCESSING_SQL, params);
    }

    public int markCompleted(Long jobId, String resultGeojson, OffsetDateTime finishedAt, OffsetDateTime updatedAt) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", jobId)
                .addValue("status", JobStatus.COMPLETED.name())
                .addValue("resultGeojson", resultGeojson)
                .addValue("finishedAt", finishedAt)
                .addValue("updatedAt", updatedAt);

        return jdbcTemplate.update(MARK_COMPLETED_SQL, params);
    }

    public int markFailed(Long jobId, String errorMessage, OffsetDateTime finishedAt, OffsetDateTime updatedAt) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", jobId)
                .addValue("status", JobStatus.FAILED.name())
                .addValue("errorMessage", errorMessage)
                .addValue("finishedAt", finishedAt)
                .addValue("updatedAt", updatedAt);

        return jdbcTemplate.update(MARK_FAILED_SQL, params);
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
