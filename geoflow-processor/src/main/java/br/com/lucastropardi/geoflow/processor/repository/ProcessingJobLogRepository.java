package br.com.lucastropardi.geoflow.processor.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;

@Repository
public class ProcessingJobLogRepository {

    private static final String INSERT_SQL = """
            INSERT INTO processing_job_log (
                id, job_id, correlation_id, level, step, message, created_at
            ) VALUES (
                :id, :jobId, :correlationId, :level, :step, :message, :createdAt
            )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProcessingJobLogRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertLog(
            Long id,
            Long jobId,
            String correlationId,
            String level,
            String step,
            String message,
            OffsetDateTime createdAt
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("jobId", jobId)
                .addValue("correlationId", correlationId)
                .addValue("level", level)
                .addValue("step", step)
                .addValue("message", message)
                .addValue("createdAt", createdAt);

        return jdbcTemplate.update(INSERT_SQL, params);
    }
}
