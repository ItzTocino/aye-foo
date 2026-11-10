package org.communityapp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Answers one question: can the database answer a query right now?
 */
public final class HealthDao {

    private static final Logger log = LoggerFactory.getLogger(HealthDao.class);

    private final DataSource dataSource;

    public HealthDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean ping() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet result = statement.executeQuery()) {
            return result.next();
        } catch (SQLException e) {
            // A probe runs on every platform check; avoid logging driver messages containing connection data.
            log.warn("database health check failed: SQLState={} vendorCode={}",
                    e.getSQLState(), e.getErrorCode());
            return false;
        }
    }
}
