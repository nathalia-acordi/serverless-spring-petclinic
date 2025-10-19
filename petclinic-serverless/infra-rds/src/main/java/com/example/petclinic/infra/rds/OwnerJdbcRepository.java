package com.example.petclinic.infra.rds;

import com.example.petclinic.domain.owner.Owner;
import com.example.petclinic.domain.owner.OwnerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Minimal JDBC implementation backed by RDS Proxy. Connection details provided via environment variables.
 * (In real migration: may reuse legacy schema or a dedicated view/table; keep mapping logic here.)
 */
@Repository
@RequiredArgsConstructor
public class OwnerJdbcRepository implements OwnerRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Owner> MAPPER = new RowMapper<>() {
        @Override
        public Owner mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Owner.builder()
                    .id(rs.getLong("id"))
                    .firstName(rs.getString("first_name"))
                    .lastName(rs.getString("last_name"))
                    .address(rs.getString("address"))
                    .city(rs.getString("city"))
                    .telephone(rs.getString("telephone"))
                    .build();
        }
    };

    @Override
    public Owner save(Owner owner) {
        if (owner.getId() == null) {
            final String sql = "INSERT INTO owners(first_name, last_name, address, city, telephone) VALUES (?,?,?,?,?)";
            KeyHolder kh = new GeneratedKeyHolder();
            try {
                jdbcTemplate.update(con -> {
                    var ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, owner.getFirstName());
                    ps.setString(2, owner.getLastName());
                    ps.setString(3, owner.getAddress());
                    ps.setString(4, owner.getCity());
                    ps.setString(5, owner.getTelephone());
                    return ps;
                }, kh);
            } catch (DuplicateKeyException ex) {
                throw new com.example.petclinic.domain.owner.OwnerValidationException("Telephone already registered");
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                if (ex.getCause() instanceof java.sql.SQLIntegrityConstraintViolationException ||
                        (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("duplicate"))) {
                    throw new com.example.petclinic.domain.owner.OwnerValidationException("Telephone already registered");
                }
                throw ex;
            }
            Long id = ((Number) java.util.Objects.requireNonNull(kh.getKey(), "Generated key is null")).longValue();
            return owner.toBuilder().id(id).build();
        } else {
            jdbcTemplate.update("UPDATE owners SET first_name=?, last_name=?, address=?, city=?, telephone=? WHERE id=?",
                    owner.getFirstName(), owner.getLastName(), owner.getAddress(), owner.getCity(), owner.getTelephone(), owner.getId());
            return owner;
        }
    }

    @PostConstruct
        void initSchema() {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS owners (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  first_name VARCHAR(80) NOT NULL,
                  last_name  VARCHAR(80) NOT NULL,
                  address    VARCHAR(255) NOT NULL,
                  city       VARCHAR(80) NOT NULL,
                  telephone  VARCHAR(20) NOT NULL,
                  UNIQUE KEY owners_telephone_uq (telephone)
                )
                """);
            // Attempt to add unique index if table existed without it
            try {
                jdbcTemplate.execute("CREATE UNIQUE INDEX owners_telephone_uq ON owners(telephone)");
            } catch (Exception e) {
                // Ignore if already exists
                String msg = e.getMessage();
                if (msg != null && !(msg.toLowerCase().contains("duplicate") || msg.toLowerCase().contains("exists"))) {
                    // log? kept silent to avoid noisy cold starts
                }
            }
        }

    @Override
    public Optional<Owner> findById(Long id) {
        List<Owner> list = jdbcTemplate.query("SELECT * FROM owners WHERE id=?", MAPPER, id);
        return list.stream().findFirst();
    }

    @Override
    public List<Owner> findAll(int page, int size) {
        int offset = page * size;
        return jdbcTemplate.query("SELECT * FROM owners ORDER BY id LIMIT ? OFFSET ?", MAPPER, size, offset);
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM owners WHERE id=?", id);
    }

    @Override
    public boolean existsByTelephone(String telephone) {
        List<Integer> rows = jdbcTemplate.query("SELECT 1 FROM owners WHERE telephone=? LIMIT 1", (rs, rn) -> 1, telephone);
        return !rows.isEmpty();
    }

    @Override
    public boolean existsById(Long id) {
        List<Integer> rows = jdbcTemplate.query("SELECT 1 FROM owners WHERE id=? LIMIT 1", (rs, rn) -> 1, id);
        return !rows.isEmpty();
    }

    @Override
    public boolean existsByTelephoneExcludingId(String telephone, Long excludeId) {
        List<Integer> rows = jdbcTemplate.query("SELECT 1 FROM owners WHERE telephone=? AND id<>? LIMIT 1", (rs, rn) -> 1, telephone, excludeId);
        return !rows.isEmpty();
    }

    public int updateOwner(Long id, Owner owner) {
        try {
            return jdbcTemplate.update("UPDATE owners SET first_name=?, last_name=?, address=?, city=?, telephone=? WHERE id=?",
                    owner.getFirstName(), owner.getLastName(), owner.getAddress(), owner.getCity(), owner.getTelephone(), id);
        } catch (DuplicateKeyException ex) {
            throw new com.example.petclinic.domain.owner.OwnerValidationException("Telephone already registered");
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof java.sql.SQLIntegrityConstraintViolationException ||
                    (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("duplicate"))) {
                throw new com.example.petclinic.domain.owner.OwnerValidationException("Telephone already registered");
            }
            throw ex;
        }
    }
}
