package com.example.petclinic.infra.rds;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.sql.DataSource;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;

/**
 * DataSource configuration for AWS Lambda using RDS Proxy with credentials from
 * Secrets Manager.
 * Always builds jdbcUrl from endpoint + db name; fetches username/password on
 * cold start.
 */
@Slf4j
@Configuration
public class RdsDataSourceConfig {

    @Value("${DB_PROXY_ENDPOINT:}")
    private String proxyEndpoint;

    @Value("${DB_NAME:petclinic}")
    private String dbName;

    @Value("${DB_SECRET_ARN:}")
    private String secretArn;

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        // Region comes from AWS_REGION env automatically; let SDK resolve.
        return SecretsManagerClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public DataSource dataSource(SecretsManagerClient smClient) {
        // Defensive fallback: try values via @Value, then env.
        String effectiveEndpoint = firstNonBlank(proxyEndpoint, System.getenv("DB_PROXY_ENDPOINT"));
        String effectiveDbName = firstNonBlank(dbName, System.getenv("DB_NAME"), "petclinic");
        String effectiveSecretArn = firstNonBlank(secretArn, System.getenv("DB_SECRET_ARN"));

        // Fallback local (sem Secrets / Proxy): if no secret ARN provided, use plain host/user/pass
        String localHost = System.getenv("DB_HOST");
        String localUser = System.getenv("DB_USER");
        String localPass = System.getenv("DB_PASS");
        boolean usingLocalFallback = (effectiveSecretArn == null || effectiveSecretArn.isBlank());

        if (usingLocalFallback) {
            validateEnv("DB_HOST", localHost);
            validateEnv("DB_USER", localUser);
            validateEnv("DB_PASS", localPass);
            log.warn("[RdsDataSourceConfig] Secret ARN ausente → ativando fallback local simples (DB_HOST, DB_USER, DB_PASS). NÃO usar em produção.");
            String jdbcUrl = String.format(
                "jdbc:mysql://%s:3306/%s?useUnicode=true&characterEncoding=utf8&useSSL=false",
                localHost, effectiveDbName);
            if (jdbcUrl.contains("null")) {
            throw new IllegalStateException("jdbcUrl construído inválido (fallback): " + jdbcUrl);
            }
            HikariConfig cfg = new HikariConfig();
            cfg.setPoolName("PetclinicPoolLocal");
            cfg.setJdbcUrl(jdbcUrl);
            cfg.setUsername(localUser);
            cfg.setPassword(localPass);
            cfg.setMaximumPoolSize(5);
            cfg.setMinimumIdle(0);
            cfg.setInitializationFailTimeout(-1);
            return new HikariDataSource(cfg);
        }

        log.info(
            "[RdsDataSourceConfig] Creating DataSource endpoint='{}' db='{}' secretArn='{}' (raw env present: endpoint={} db={} secret={})",
            effectiveEndpoint,
            effectiveDbName,
            redact(effectiveSecretArn),
            System.getenv().containsKey("DB_PROXY_ENDPOINT"),
            System.getenv().containsKey("DB_NAME"),
            System.getenv().containsKey("DB_SECRET_ARN"));

        validateEnv("DB_PROXY_ENDPOINT", effectiveEndpoint);
        validateEnv("DB_SECRET_ARN", effectiveSecretArn);
        validateEnv("DB_NAME", effectiveDbName);

        DbCredentials creds = fetchCredentials(smClient, effectiveSecretArn);

        String jdbcUrl = String.format(
            "jdbc:mysql://%s:3306/%s?useUnicode=true&characterEncoding=utf8&useSSL=true&requireSSL=true&verifyServerCertificate=false",
            effectiveEndpoint, effectiveDbName);
        if (jdbcUrl.contains("null")) {
            throw new IllegalStateException("jdbcUrl construído inválido: " + jdbcUrl);
        }
        log.info("[RdsDataSourceConfig] Built jdbcUrl for host='{}' db='{}' (credentials not logged)",
            effectiveEndpoint, effectiveDbName);

        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("PetclinicPool");
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(creds.username());
        cfg.setPassword(creds.password());
        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(0);
        cfg.setInitializationFailTimeout(-1);
        return new HikariDataSource(cfg);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource ds) {
        return new JdbcTemplate(ds);
    }

    private void validateEnv(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " ausente ou vazio");
        }
    }

    private DbCredentials fetchCredentials(SecretsManagerClient sm, String secretArn) {
        try {
            GetSecretValueResponse resp = sm
                    .getSecretValue(GetSecretValueRequest.builder().secretId(secretArn).build());
            String json = resp.secretString();
            if (json == null || json.isBlank()) {
                throw new IllegalStateException("Secret vazio para ARN: " + secretArn);
            }
            Map<String, Object> map = JsonUtil.parse(json);
            Object u = map.get("username");
            Object p = map.get("password");
            if (u == null || p == null) {
                throw new IllegalStateException("Secret não contém 'username' e/ou 'password'");
            }
            return new DbCredentials(Objects.toString(u), Objects.toString(p));
        } catch (RuntimeException e) {
            log.error("Falha ao obter credenciais do Secrets Manager: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String redact(String v) {
        if (v == null)
            return null;
        if (v.length() <= 10)
            return "***";
        return v.substring(0, 6) + "***" + v.substring(v.length() - 4);
    }

    private record DbCredentials(String username, String password) {
    }

    private String firstNonBlank(String... values) {
        if (values == null)
            return null;
        for (String v : values) {
            if (v != null && !v.isBlank())
                return v;
        }
        return null;
    }
}

// Minimal JSON util (avoids adding full Jackson here; core modules already
// bring Jackson on classpath via Boot BOM)
class JsonUtil {
    static Map<String, Object> parse(String json) {
        // Use Jackson if available on classpath (Spring Boot brings it). Fallback would
        // throw if absent.
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            return om.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
            });
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Falha ao parsear secret JSON", e);
        }
    }
}
