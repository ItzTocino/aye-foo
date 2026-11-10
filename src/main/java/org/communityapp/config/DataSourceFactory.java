package org.communityapp.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Builds the one connection pool the application uses.
 */
public final class DataSourceFactory {

    private static final int MAXIMUM_POOL_SIZE = 10;
    private static final int MINIMUM_IDLE = 2;
    private static final long CONNECTION_TIMEOUT_MS = 3_000;

    private DataSourceFactory() {
    }

    public static HikariDataSource create(AppConfig config) {
        HikariConfig pool = new HikariConfig();
        pool.setPoolName("saveourworld");
        pool.setJdbcUrl(config.dbUrl());
        pool.setUsername(config.dbUser());
        pool.setPassword(config.dbPassword());
        pool.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        pool.setMinimumIdle(MINIMUM_IDLE);
        pool.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        // Start even when MySQL is down; /healthz reports the failure instead of a dead context.
        pool.setInitializationFailTimeout(-1);
        return new HikariDataSource(pool);
    }
}
