package com.example.petclinic.infra.pet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.petclinic.domain.pet.Pet;
import com.example.petclinic.domain.pet.PetRepository;
import com.example.petclinic.domain.pet.PetType;

@Repository
public class JdbcPetRepository implements PetRepository {

	private static final String INSERT_PET = """
			INSERT INTO pets (name, birth_date, type_id, owner_id)
			VALUES (?, ?, ?, ?)
			""";

	private static final String UPDATE_PET = """
			UPDATE pets SET name = ?, birth_date = ?, type_id = ?
			WHERE id = ?
			""";

	private static final String SELECT_BY_ID = """
			SELECT p.id, p.name, p.birth_date, p.owner_id, t.id as type_id, t.name as type_name
			FROM pets p
			LEFT JOIN types t ON p.type_id = t.id
			WHERE p.id = ?
			""";

	private static final String SELECT_BY_OWNER_ID = """
			SELECT p.id, p.name, p.birth_date, p.owner_id, t.id as type_id, t.name as type_name
			FROM pets p
			LEFT JOIN types t ON p.type_id = t.id
			WHERE p.owner_id = ?
			ORDER BY p.name
			""";

	private static final String SELECT_BY_OWNER_AND_NAME = """
			SELECT p.id, p.name, p.birth_date, p.owner_id, t.id as type_id, t.name as type_name
			FROM pets p
			LEFT JOIN types t ON p.type_id = t.id
			WHERE p.owner_id = ? AND p.name = ?
			""";

	private final JdbcTemplate jdbcTemplate;

	private final RowMapper<Pet> petRowMapper = (rs, rowNum) -> mapPet(rs);

	public JdbcPetRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Pet save(Pet pet) {
		if (pet.getId() == null) {
			return insert(pet);
		} else {
			return update(pet);
		}
	}

	@Override
	public Pet findById(Integer id) {
		try {
			return jdbcTemplate.queryForObject(SELECT_BY_ID, petRowMapper, id);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<Pet> findByOwnerId(Integer ownerId) {
		return jdbcTemplate.query(SELECT_BY_OWNER_ID, petRowMapper, ownerId);
	}

	@Override
	public Pet findByOwnerIdAndName(Integer ownerId, String name) {
		try {
			return jdbcTemplate.queryForObject(SELECT_BY_OWNER_AND_NAME, petRowMapper, ownerId, name);
		} catch (Exception e) {
			return null;
		}
	}

	private Pet insert(Pet pet) {
		jdbcTemplate.update(INSERT_PET,
				pet.getName(),
				pet.getBirthDate(),
				pet.getType() != null ? pet.getType().getId() : null,
				pet.getOwnerId());

		// Retrieve the generated ID
		Integer generatedId = jdbcTemplate.queryForObject(
				"SELECT LAST_INSERT_ID()", Integer.class);
		pet.setId(generatedId);
		return pet;
	}

	private Pet update(Pet pet) {
		jdbcTemplate.update(UPDATE_PET,
				pet.getName(),
				pet.getBirthDate(),
				pet.getType() != null ? pet.getType().getId() : null,
				pet.getId());
		return pet;
	}

	private Pet mapPet(ResultSet rs) throws SQLException {
		Pet pet = new Pet();
		pet.setId(rs.getInt("id"));
		pet.setName(rs.getString("name"));
		pet.setBirthDate(rs.getDate("birth_date") != null ? rs.getDate("birth_date").toLocalDate() : null);
		pet.setOwnerId(rs.getInt("owner_id"));

		int typeId = rs.getInt("type_id");
		if (!rs.wasNull()) {
			PetType type = new PetType();
			type.setId(typeId);
			type.setName(rs.getString("type_name"));
			pet.setType(type);
		}

		return pet;
	}

}
