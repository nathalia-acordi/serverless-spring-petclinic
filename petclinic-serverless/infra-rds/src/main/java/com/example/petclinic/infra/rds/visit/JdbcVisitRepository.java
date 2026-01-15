package com.example.petclinic.infra.rds.visit;

import com.example.petclinic.domain.visit.Visit;
import com.example.petclinic.domain.visit.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of VisitRepository for AWS Lambda + RDS.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcVisitRepository implements VisitRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String VISIT_INSERT =
            "INSERT INTO visits (pet_id, visit_date, description) VALUES (?, ?, ?)";

    private static final String VISIT_SELECT =
            "SELECT v.id, v.pet_id, v.visit_date, v.description FROM visits v";

    private static final String VISIT_WITH_OWNER = VISIT_SELECT +
            " JOIN pets p ON v.pet_id = p.id JOIN owners o ON p.owner_id = o.id";

    private final RowMapper<Visit> visitMapper = (rs, rowNum) -> new Visit(
            rs.getLong("owner_id"),
            rs.getLong("pet_id"),
            rs.getObject("visit_date", LocalDate.class),
            rs.getString("description")
    );

    @Override
    public Visit save(Visit visit) {
        if (visit.getId() == null) {
            // Insert
            int rows = jdbcTemplate.update(VISIT_INSERT,
                    visit.getPetId(),
                    visit.getVisitDate(),
                    visit.getDescription());
            if (rows == 0) {
                throw new IllegalStateException("Failed to insert visit");
            }
            log.debug("[JdbcVisitRepository] Visit created for petId={}", visit.getPetId());
        } else {
            // Update
            String updateSql = "UPDATE visits SET visit_date = ?, description = ? WHERE id = ?";
            int rows = jdbcTemplate.update(updateSql,
                    visit.getVisitDate(),
                    visit.getDescription(),
                    visit.getId());
            if (rows == 0) {
                throw new IllegalStateException("Failed to update visit with id: " + visit.getId());
            }
            log.debug("[JdbcVisitRepository] Visit updated id={}", visit.getId());
        }
        return visit;
    }

    @Override
    public Optional<Visit> findById(Long id) {
        String sql = VISIT_WITH_OWNER + " WHERE v.id = ?";
        try {
            Visit visit = jdbcTemplate.queryForObject(sql, new RowMapper<Visit>() {
                @Override
                public Visit mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Visit v = new Visit();
                    v.setId(rs.getLong("id"));
                    v.setOwnerId(rs.getLong("owner_id"));
                    v.setPetId(rs.getLong("pet_id"));
                    v.setVisitDate(rs.getObject("visit_date", LocalDate.class));
                    v.setDescription(rs.getString("description"));
                    return v;
                }
            }, id);
            return Optional.of(visit);
        } catch (Exception e) {
            log.debug("[JdbcVisitRepository] Visit not found id={}", id);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Visit> findByIdAndOwnerId(Long visitId, Long ownerId) {
        String sql = VISIT_WITH_OWNER + " WHERE v.id = ? AND o.id = ?";
        try {
            Visit visit = jdbcTemplate.queryForObject(sql, new RowMapper<Visit>() {
                @Override
                public Visit mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Visit v = new Visit();
                    v.setId(rs.getLong("id"));
                    v.setOwnerId(rs.getLong("owner_id"));
                    v.setPetId(rs.getLong("pet_id"));
                    v.setVisitDate(rs.getObject("visit_date", LocalDate.class));
                    v.setDescription(rs.getString("description"));
                    return v;
                }
            }, visitId, ownerId);
            return Optional.of(visit);
        } catch (Exception e) {
            log.debug("[JdbcVisitRepository] Visit not found visitId={} ownerId={}", visitId, ownerId);
            return Optional.empty();
        }
    }

    @Override
    public List<Visit> findByOwnerIdAndPetId(Long ownerId, Long petId) {
        String sql = VISIT_WITH_OWNER + " WHERE o.id = ? AND p.id = ? ORDER BY v.visit_date DESC";
        return jdbcTemplate.query(sql, new RowMapper<Visit>() {
            @Override
            public Visit mapRow(ResultSet rs, int rowNum) throws SQLException {
                Visit v = new Visit();
                v.setId(rs.getLong("id"));
                v.setOwnerId(rs.getLong("owner_id"));
                v.setPetId(rs.getLong("pet_id"));
                v.setVisitDate(rs.getObject("visit_date", LocalDate.class));
                v.setDescription(rs.getString("description"));
                return v;
            }
        }, ownerId, petId);
    }

    @Override
    public List<Visit> findAll(int page, int size) {
        String sql = VISIT_WITH_OWNER + " ORDER BY v.visit_date DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new RowMapper<Visit>() {
            @Override
            public Visit mapRow(ResultSet rs, int rowNum) throws SQLException {
                Visit v = new Visit();
                v.setId(rs.getLong("id"));
                v.setOwnerId(rs.getLong("owner_id"));
                v.setPetId(rs.getLong("pet_id"));
                v.setVisitDate(rs.getObject("visit_date", LocalDate.class));
                v.setDescription(rs.getString("description"));
                return v;
            }
        }, size, page * size);
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM visits WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        if (rows == 0) {
            log.warn("[JdbcVisitRepository] No visit found to delete with id={}", id);
        } else {
            log.debug("[JdbcVisitRepository] Visit deleted id={}", id);
        }
    }

    @Override
    public void deleteByIdAndOwnerId(Long visitId, Long ownerId) {
        String sql = "DELETE FROM visits WHERE id = ? AND pet_id IN " +
                "(SELECT id FROM pets WHERE owner_id = ?)";
        int rows = jdbcTemplate.update(sql, visitId, ownerId);
        if (rows == 0) {
            log.warn("[JdbcVisitRepository] No visit found to delete with visitId={} ownerId={}", visitId, ownerId);
        } else {
            log.debug("[JdbcVisitRepository] Visit deleted visitId={} ownerId={}", visitId, ownerId);
        }
    }
}
