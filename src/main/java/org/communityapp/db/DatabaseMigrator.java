package org.communityapp.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.communityapp.util.SqlScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies every unapplied file in db/migration in name order and records its SHA-256.
 *
 * <p>MySQL commits DDL immediately, so a file that fails halfway is left partly applied.
 * Keep each migration small, and never edit one that has run anywhere.
 */
public final class DatabaseMigrator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrator.class);
    private static final String LOCK_NAME = "saveourworld_migrate";
    private static final int LOCK_TIMEOUT_SECONDS = 10;
    private static final String MIGRATION_SEPARATOR = "__";

    // Must stay identical to the schema_migrations table in V001.
    private static final String CREATE_MIGRATIONS_TABLE = """
            CREATE TABLE IF NOT EXISTS schema_migrations (
              version    VARCHAR(20)  NOT NULL,
              checksum   CHAR(64)     NOT NULL,
              applied_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (version)
            ) ENGINE=InnoDB""";

    private final DataSource dataSource;
    private final Path directory;

    public DatabaseMigrator(DataSource dataSource, Path directory) {
        this.dataSource = dataSource;
        this.directory = directory;
    }

    public int migrate() throws IOException, SQLException {
        List<Path> files = migrationFiles();
        try (Connection connection = dataSource.getConnection()) {
            acquireLock(connection);
            try {
                execute(connection, CREATE_MIGRATIONS_TABLE);
                Map<String, String> applied = appliedMigrations(connection);
                int count = 0;
                for (Path file : files) {
                    String version = versionOf(file);
                    String checksum = checksumOf(file);
                    String previous = applied.get(version);
                    if (previous != null) {
                        if (!previous.equals(checksum)) {
                            throw new IllegalStateException("migration " + version
                                    + " changed after it was applied");
                        }
                        continue;
                    }
                    for (String statement : SqlScript.statements(Files.readString(file))) {
                        execute(connection, statement);
                    }
                    record(connection, version, checksum);
                    log.info("applied migration version={}", version);
                    count++;
                }
                return count;
            } finally {
                releaseLock(connection);
            }
        }
    }

    private List<Path> migrationFiles() throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
        }
    }

    private static String versionOf(Path file) {
        String name = file.getFileName().toString();
        int separator = name.indexOf(MIGRATION_SEPARATOR);
        if (separator <= 0) {
            throw new IllegalStateException("migration file must be named V<number>__<name>.sql: " + name);
        }
        return name.substring(0, separator);
    }

    private static String checksumOf(Path file) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static Map<String, String> appliedMigrations(Connection connection) throws SQLException {
        Map<String, String> applied = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT version, checksum FROM schema_migrations");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                applied.put(result.getString(1), result.getString(2));
            }
        }
        return applied;
    }

    private static void record(Connection connection, String version, String checksum) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO schema_migrations (version, checksum) VALUES (?, ?)")) {
            statement.setString(1, version);
            statement.setString(2, checksum);
            statement.executeUpdate();
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    private static void acquireLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, LOCK_NAME);
            statement.setInt(2, LOCK_TIMEOUT_SECONDS);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new IllegalStateException("another process is migrating the database");
                }
            }
        }
    }

    private static void releaseLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, LOCK_NAME);
            statement.executeQuery().close();
        }
    }
}
