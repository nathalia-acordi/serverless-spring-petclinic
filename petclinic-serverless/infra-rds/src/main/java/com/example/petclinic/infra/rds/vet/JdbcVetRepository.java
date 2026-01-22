package com.example.petclinic.infra.rds.vet;

import com.example.petclinic.domain.vet.Specialty;
import com.example.petclinic.domain.vet.Vet;
import com.example.petclinic.domain.vet.VetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of VetRepository for AWS Lambda + RDS.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcVetRepository implements VetRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String VET_SELECT =
            "SELECT id, first_name, last_name FROM vets";

    private static final String SPECIALTY_SELECT =
            "SELECT s.id, s.name " +
            "FROM specialties s " +
            "JOIN vet_specialties vs ON s.id = vs.specialty_id " +
            "WHERE vs.vet_id = ? " +
            "ORDER BY s.name";

    private final RowMapper<Vet> vetMapper = (rs, rowNum) -> {
        Vet vet = new Vet();
        vet.setId(rs.getLong("id"));
        vet.setFirstName(rs.getString("first_name"));
        vet.setLastName(rs.getString("last_name"));
        return vet;
    };

    private final RowMapper<Specialty> specialtyMapper = (rs, rowNum) -> {
        Specialty specialty = new Specialty();
        specialty.setId(rs.getLong("id"));
        specialty.setName(rs.getString("name"));
        return specialty;
    };

    @Override
    public List<Vet> findAll(int page, int size) {
        String sql = VET_SELECT + " ORDER BY last_name, first_name LIMIT ? OFFSET ?";
        List<Vet> vets = jdbcTemplate.query(sql, vetMapper, size, page * size);
        log.debug("[JdbcVetRepository] Found {} vets (page={}, size={})", vets.size(), page, size);
        return vets;
    }

    @Override
    public Optional<Vet> findById(Long id) {
        String sql = VET_SELECT + " WHERE id = ?";
        try {
            Vet vet = jdbcTemplate.queryForObject(sql, vetMapper, id);
            log.debug("[JdbcVetRepository] Found vet id={}", id);
            return Optional.of(vet);
        } catch (Exception e) {
            log.debug("[JdbcVetRepository] Vet not found id={}", id);
            return Optional.empty();
        }
    }

    @Override
    public List<Specialty> findSpecialtiesByVetId(Long vetId) {
        List<Specialty> specialties = jdbcTemplate.query(SPECIALTY_SELECT, specialtyMapper, vetId);
        log.debug("[JdbcVetRepository] Found {} specialties for vet id={}", specialties.size(), vetId);
        return specialties;
    }
}
