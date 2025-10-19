package com.example.petclinic.infra.rds;

import com.example.petclinic.domain.owner.Owner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class OwnerJdbcRepositoryIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.3.0")
            .withDatabaseName("petclinic")
            .withUsername("test")
            .withPassword("test");

    static OwnerJdbcRepository repository;

    @BeforeAll
    static void init() {
        mysql.start();
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(mysql.getJdbcUrl());
        cfg.setUsername(mysql.getUsername());
        cfg.setPassword(mysql.getPassword());
        cfg.setMaximumPoolSize(2);
        JdbcTemplate jdbc = new JdbcTemplate(new HikariDataSource(cfg));
        jdbc.execute("CREATE TABLE owners (id BIGINT PRIMARY KEY AUTO_INCREMENT, first_name VARCHAR(30), last_name VARCHAR(30), address VARCHAR(255), city VARCHAR(80), telephone VARCHAR(20))");
        repository = new OwnerJdbcRepository(jdbc);
    }

    @Test
    void saveAndFind() {
        Owner saved = repository.save(Owner.builder().firstName("Ana").lastName("Silva").build());
        assertNotNull(saved.getId());
        assertTrue(repository.findById(saved.getId()).isPresent());
    }
}
