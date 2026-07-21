package br.com.lucastropardi.geoflow.api.repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdGeneratorRepository {

    private static final String LOCK_NEXT_VALUE_SQL = """
            select next_value
            from id_generator
            where entity_name = :entityName
            for update
            """;

    private static final String UPDATE_NEXT_VALUE_SQL = """
            update id_generator
            set next_value = :nextValue,
                updated_at = :updatedAt
            where entity_name = :entityName
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public IdGeneratorRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> findAndLockNextValue(String entityName) {
        return jdbcTemplate.query(
                LOCK_NEXT_VALUE_SQL,
                Map.of("entityName", entityName),
                rs -> rs.next() ? Optional.of(rs.getLong("next_value")) : Optional.empty()
        );
    }

    public void updateNextValue(String entityName, long nextValue, OffsetDateTime updatedAt) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("entityName", entityName)
                .addValue("nextValue", nextValue)
                .addValue("updatedAt", updatedAt);

        jdbcTemplate.update(UPDATE_NEXT_VALUE_SQL, params);
    }
}
