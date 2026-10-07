package br.com.rentafit.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

@Configuration
public class InstallationMigrationConfiguration {
    @Bean
    public FlywayMigrationStrategy installationMigrationStrategy(DataSource source) {
        JdbcTemplate database = new JdbcTemplate(source);
        return flyway -> {
            boolean fresh = flyway.info().applied().length == 0 && database.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() "
                            + "AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'", Integer.class) == 0;
            flyway.migrate();
            if (fresh) database.update("UPDATE installation_state SET status = 'PENDING' WHERE id = 1 AND status = 'LEGACY'");
        };
    }
}
