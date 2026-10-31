package io.github.messaoudidev.terminbuchung;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Tests laufen gegen ein frisches PostgreSQL im Container, nie gegen die lokale Datenbank.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // Gleiche Version wie im docker-compose.yml.
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18");
    }
}
