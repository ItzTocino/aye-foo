package org.communityapp.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** Reads defined impact metrics from the database-owned aggregation view. */
public final class ImpactDao {
    // Hikari bounds connection acquisition separately; bound the aggregation itself too.
    private static final int QUERY_TIMEOUT_SECONDS = 2;
    private final DataSource dataSource;

    public ImpactDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Metric> metrics() throws SQLException {
        List<Metric> metrics = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT metric_key, label, unit, category, is_featured, value, last_fact_on "
                     + "FROM v_impact_summary ORDER BY display_order, metric_key")) {
            query.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    String key = rows.getString(1);
                    String label = rows.getString(2);
                    String unit = rows.getString(3);
                    String category = rows.getString(4);
                    boolean featured = rows.getBoolean(5);
                    BigDecimal value = rows.getBigDecimal(6);
                    java.sql.Date date = rows.getDate(7);
                    if (key == null || key.isBlank() || label == null || label.isBlank()
                            || unit == null || category == null || value == null
                            || value.signum() < 0 || !List.of("cleanup", "conservation", "community")
                                    .contains(category)) {
                        throw new SQLException("invalid impact summary");
                    }
                    // A defined metric with no fact has no reported value. Never present its
                    // view-generated zero as an observed measurement.
                    if (date != null) {
                        metrics.add(new Metric(key, label, unit, category, featured,
                                value.stripTrailingZeros().toPlainString(), date.toLocalDate()));
                    }
                }
            }
        }
        return List.copyOf(metrics);
    }

    public record Metric(String key, String label, String unit, String category,
                         boolean featured, String value, LocalDate lastFactOn) { }
}
