package br.com.lucastropardi.geoflow.api.repository;

import br.com.lucastropardi.geoflow.api.model.ProcessingJobLogRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProcessingJobLogRepository {

    private static final String INSERT_SQL = """
            INSERT INTO processing_job_log (
                id, job_id, level, step, message, created_at
            ) VALUES (
                :id, :jobId, :level, :step, :message, :createdAt
            )
            """;

    private static final String FIND_BY_JOB_ID_SQL = """
            SELECT id, job_id, level, step, message, created_at
            FROM processing_job_log
            WHERE job_id = :jobId
            ORDER BY created_at ASC
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProcessingJobLogRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(ProcessingJobLogRecord logRecord) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", logRecord.id())
                .addValue("jobId", logRecord.jobId())
                .addValue("level", logRecord.level())
                .addValue("step", logRecord.step())
                .addValue("message", logRecord.message())
                .addValue("createdAt", logRecord.createdAt());

        jdbcTemplate.update(INSERT_SQL, params);
    }

    public List<ProcessingJobLogRecord> findByJobId(Long jobId) {
        return jdbcTemplate.query(FIND_BY_JOB_ID_SQL, Map.of("jobId", jobId), rowMapper());
    }

    private RowMapper<ProcessingJobLogRecord> rowMapper() {
        return this::mapRow;
    }

    private ProcessingJobLogRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ProcessingJobLogRecord(
                rs.getLong("id"),
                rs.getLong("job_id"),
                rs.getString("level"),
                rs.getString("step"),
                rs.getString("message"),
                rs.getObject("created_at", java.time.OffsetDateTime.class)
        );
    }
}
