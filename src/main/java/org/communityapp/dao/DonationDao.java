package org.communityapp.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

/** Reads the recorded USD donation ledger; pending and refunded gifts never count. */
public final class DonationDao {
    private final DataSource dataSource;

    public DonationDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Totals succeededUsd() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT COALESCE(SUM(amount_cents), 0), COUNT(*) FROM donations "
                     + "WHERE currency = 'USD' AND status = 'succeeded'");
             ResultSet row = query.executeQuery()) {
            if (!row.next()) {
                throw new SQLException("donation totals query returned no row");
            }
            BigDecimal amount = row.getBigDecimal(1);
            BigDecimal gifts = row.getBigDecimal(2);
            if (amount == null || gifts == null) {
                throw new SQLException("invalid donation totals");
            }
            try {
                long cents = amount.longValueExact();
                long count = gifts.longValueExact();
                if (cents < 0 || count < 0 || (cents == 0) != (count == 0)) {
                    throw new SQLException("invalid donation totals");
                }
                return new Totals(cents, count);
            } catch (ArithmeticException invalid) {
                throw new SQLException("donation totals are out of range", invalid);
            }
        }
    }

    public record Totals(long cents, long count) { }
}
