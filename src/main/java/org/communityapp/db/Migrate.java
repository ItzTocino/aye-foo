package org.communityapp.db;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import org.communityapp.config.AppConfig;
import org.communityapp.config.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command-line entry point: applies pending migrations, then exits.
 */
public final class Migrate {

    private static final Logger log = LoggerFactory.getLogger(Migrate.class);
    private static final String DEFAULT_DIRECTORY = "src/main/resources/db/migration";

    private Migrate() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args.length > 0 ? args[0] : DEFAULT_DIRECTORY);
        try (HikariDataSource dataSource = DataSourceFactory.create(AppConfig.load())) {
            int applied = new DatabaseMigrator(dataSource, directory).migrate();
            log.info("migrations applied: {}", applied);
        }
    }
}
