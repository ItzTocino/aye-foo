package org.communityapp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** Reads upcoming published events without caching time-sensitive availability. */
public final class EventDao {
    private final DataSource dataSource;

    public EventDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Event> upcoming(Instant now) throws SQLException {
        List<Event> events = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT title, summary, location_name, city, state, starts_at, timezone, "
                     + "capacity, seats_taken, slug FROM events WHERE status = 'published' "
                     + "AND starts_at > ? ORDER BY starts_at, id LIMIT 50")) {
            // DATETIME stores UTC wall time. Avoid driver/session timezone conversion.
            query.setObject(1, LocalDateTime.ofInstant(now, ZoneOffset.UTC));
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    LocalDateTime start = rows.getObject(6, LocalDateTime.class);
                    if (start == null) {
                        throw new SQLException("published event has no start time");
                    }
                    events.add(new Event(rows.getString(1), rows.getString(2), rows.getString(3),
                            rows.getString(4), rows.getString(5), start.toInstant(ZoneOffset.UTC),
                            rows.getString(7), rows.getInt(8), rows.getInt(9), rows.getString(10)));
                }
            }
        }
        return events;
    }

    /** Returns only published events that have not started; drafts and cancelled events stay private. */
    public Detail findUpcomingPublished(String slug, Instant now) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement query = connection.prepareStatement(
                     "SELECT e.title, e.summary, e.description, e.location_name, e.address, "
                     + "e.city, e.state, e.zip, e.map_url, e.starts_at, e.ends_at, e.timezone, "
                     + "c.capacity, c.seats_left, c.waitlist_count FROM events e "
                     + "JOIN v_event_capacity c ON c.id = e.id "
                     + "WHERE e.slug = ? AND e.status = 'published' AND e.starts_at > ?")) {
            query.setString(1, slug);
            query.setObject(2, LocalDateTime.ofInstant(now, ZoneOffset.UTC));
            try (ResultSet rows = query.executeQuery()) {
                if (!rows.next()) {
                    return null;
                }
                LocalDateTime start = rows.getObject(10, LocalDateTime.class);
                LocalDateTime end = rows.getObject(11, LocalDateTime.class);
                if (start == null) {
                    throw new SQLException("published event has no start time");
                }
                return new Detail(rows.getString(1), rows.getString(2), rows.getString(3),
                        rows.getString(4), rows.getString(5), rows.getString(6), rows.getString(7),
                        rows.getString(8), rows.getString(9), start.toInstant(ZoneOffset.UTC),
                        end == null ? null : end.toInstant(ZoneOffset.UTC), rows.getString(12),
                        rows.getInt(13), rows.getInt(14), rows.getInt(15));
            }
        }
    }

    public record Event(String title, String summary, String location, String city, String state,
                        Instant startsAt, String timezone, int capacity, int seatsTaken, String slug) { }
    public record Detail(String title, String summary, String description, String location,
                         String address, String city, String state, String zip, String mapUrl,
                         Instant startsAt, Instant endsAt, String timezone, int capacity,
                         int seatsLeft, int waitlistCount) { }
}
