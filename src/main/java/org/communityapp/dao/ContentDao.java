package org.communityapp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** Public content cache: each request checks the database version before reusing a snapshot. */
public final class ContentDao {
    private final DataSource dataSource;
    private volatile Snapshot snapshot;

    public ContentDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Snapshot publicContent() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            long version = version(connection);
            Snapshot current = snapshot;
            if (current != null && current.version() == version) {
                return current;
            }
            synchronized (this) {
                current = snapshot;
                if (current != null && current.version() == version) {
                    return current;
                }
                // Read the version on both sides of the queries to avoid caching a mixed snapshot.
                for (int attempt = 0; attempt < 2; attempt++) {
                    long before = version(connection);
                    List<Initiative> initiatives = initiatives(connection);
                    List<Partner> partners = partners(connection);
                    long after = version(connection);
                    if (before == after) {
                        current = new Snapshot(after, List.copyOf(initiatives), List.copyOf(partners));
                        snapshot = current;
                        return current;
                    }
                }
                throw new SQLException("public content changed during snapshot read");
            }
        }
    }

    private static long version(Connection connection) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT meta_value FROM app_meta WHERE meta_key = 'cache_version'");
             ResultSet rows = query.executeQuery()) {
            if (!rows.next()) {
                throw new SQLException("cache_version is not initialized; apply migrations");
            }
            long value = Long.parseLong(rows.getString(1));
            if (value < 0) {
                throw new SQLException("invalid cache_version: negative value");
            }
            return value;
        } catch (NumberFormatException badVersion) {
            throw new SQLException("invalid cache_version", badVersion);
        }
    }

    private static List<Initiative> initiatives(Connection connection) throws SQLException {
        List<Initiative> content = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT title, summary, body, location FROM initiatives "
                + "WHERE status = 'published' ORDER BY display_order, title");
             ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                content.add(new Initiative(rows.getString(1), rows.getString(2),
                        rows.getString(3), rows.getString(4)));
            }
        }
        return content;
    }

    private static List<Partner> partners(Connection connection) throws SQLException {
        List<Partner> content = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT name, blurb, website_url FROM partners WHERE is_active = 1 "
                + "ORDER BY display_order, name");
             ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                String url = rows.getString(3);
                // URLs come from admins, but must still be safe in an HTML link.
                if (url == null || !url.matches("https://[A-Za-z0-9.-]+(?::443)?(?:/[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]*)?")
                        || !url.substring(8).split("[/:?]", 2)[0].matches(
                                "(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}")) {
                    url = null;
                }
                content.add(new Partner(rows.getString(1), rows.getString(2), url));
            }
        }
        return content;
    }

    public record Initiative(String title, String summary, String body, String location) { }
    public record Partner(String name, String blurb, String websiteUrl) { }
    public record Snapshot(long version, List<Initiative> initiatives, List<Partner> partners) { }
}
