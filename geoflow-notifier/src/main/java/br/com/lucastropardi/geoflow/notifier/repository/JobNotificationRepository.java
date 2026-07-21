package br.com.lucastropardi.geoflow.notifier.repository;

import br.com.lucastropardi.geoflow.notifier.dto.JobNotificationStatusResponse;
import br.com.lucastropardi.geoflow.shared.enums.JobStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JobNotificationRepository {

    private static final String FIND_STATUS_BY_ID_SQL = """
            SELECT id, status
            FROM processing_job
            WHERE id = :id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JobNotificationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<JobNotificationStatusResponse> findStatusById(Long jobId) {
        List<JobNotificationStatusResponse> results = jdbcTemplate.query(
                FIND_STATUS_BY_ID_SQL,
                Map.of("id", jobId),
                (rs, rowNum) -> new JobNotificationStatusResponse(
                        rs.getLong("id"),
                        JobStatus.valueOf(rs.getString("status"))
                )
        );

        return results.stream().findFirst();
    }
}
