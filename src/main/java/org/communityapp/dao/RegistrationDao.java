package org.communityapp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.sql.DataSource;

/** Atomically allocates a seat or a waitlist entry and queues a confirmation. */
public final class RegistrationDao {
    private final DataSource source;

    public RegistrationDao(DataSource source) {
        this.source = source;
    }

    public enum Result { REGISTERED, WAITLISTED, DUPLICATE, UNAVAILABLE }

    public record Applicant(String first, String last, String email, String phone,
                            String city, String state) { }

    /** Persistent per-hour limits; count failed and duplicate attempts as well as successes. */
    public boolean allowAttempt(String address, String email, Instant now) throws SQLException {
        LocalDateTime hour = LocalDateTime.ofInstant(now.truncatedTo(ChronoUnit.HOURS), ZoneOffset.UTC);
        try (Connection connection = source.getConnection()) {
            return incrementLimit(connection, "registration:ip:" + digest(address), hour, 100)
                    && incrementLimit(connection, "registration:email:" + digest(email), hour, 5);
        }
    }

    private static boolean incrementLimit(Connection connection, String bucket, LocalDateTime hour,
                                          int max) throws SQLException {
        try (PreparedStatement increment = connection.prepareStatement(
                "INSERT INTO rate_limits (bucket_key,window_start,count) VALUES (?,?,1) "
                + "ON DUPLICATE KEY UPDATE count=count+1")) {
            increment.setString(1, bucket);
            increment.setObject(2, hour);
            increment.executeUpdate();
        }
        try (PreparedStatement read = connection.prepareStatement(
                "SELECT count FROM rate_limits WHERE bucket_key=? AND window_start=?")) {
            read.setString(1, bucket);
            read.setObject(2, hour);
            try (ResultSet result = read.executeQuery()) {
                return result.next() && result.getInt(1) <= max;
            }
        }
    }

    private static String digest(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public Result register(String slug, Applicant person, Instant now) throws SQLException {
        try (Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long eventId;
                String title;
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT id, title FROM events WHERE slug=? AND status='published' "
                        + "AND starts_at > ? FOR UPDATE")) {
                    lock.setString(1, slug);
                    lock.setObject(2, LocalDateTime.ofInstant(now, ZoneOffset.UTC));
                    try (ResultSet row = lock.executeQuery()) {
                        if (!row.next()) {
                            connection.rollback();
                            return Result.UNAVAILABLE;
                        }
                        eventId = row.getLong(1);
                        title = row.getString(2);
                    }
                }
                // The volunteer identity is email; the existing record is never overwritten.
                long volunteerId;
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO volunteers (first_name,last_name,email,phone,city,state,consent,source) "
                        + "VALUES (?,?,?,?,?,?,1,'event') ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)")) {
                    insert.setString(1, person.first());
                    insert.setString(2, person.last());
                    insert.setString(3, person.email());
                    insert.setString(4, person.phone());
                    insert.setString(5, person.city());
                    insert.setString(6, person.state());
                    insert.executeUpdate();
                }
                try (PreparedStatement idQuery = connection.prepareStatement("SELECT LAST_INSERT_ID()");
                     ResultSet row = idQuery.executeQuery()) {
                    if (!row.next()) throw new SQLException("volunteer key missing");
                    volunteerId = row.getLong(1);
                }
                long cancelledId = 0;
                try (PreparedStatement existing = connection.prepareStatement(
                        "SELECT id,status FROM event_registrations WHERE event_id=? AND volunteer_id=? FOR UPDATE")) {
                    existing.setLong(1, eventId);
                    existing.setLong(2, volunteerId);
                    try (ResultSet row = existing.executeQuery()) {
                        if (row.next()) {
                            if ("cancelled".equals(row.getString(2))) {
                                cancelledId = row.getLong(1);
                            } else {
                                connection.rollback();
                                return Result.DUPLICATE;
                            }
                        }
                    }
                }
                boolean hasSeat;
                try (PreparedStatement reserve = connection.prepareStatement(
                        "UPDATE events SET seats_taken=seats_taken+1 WHERE id=? "
                        + "AND status='published' AND starts_at > ? AND seats_taken < capacity")) {
                    reserve.setLong(1, eventId);
                    reserve.setObject(2, LocalDateTime.ofInstant(now, ZoneOffset.UTC));
                    hasSeat = reserve.executeUpdate() == 1;
                }
                long registrationId;
                if (cancelledId != 0) {
                    try (PreparedStatement reactivate = connection.prepareStatement(
                            "UPDATE event_registrations SET status=?,waitlisted_at=?,cancelled_at=NULL,"
                            + "token_hash=NULL,token_expires_at=NULL WHERE id=? AND status='cancelled'")) {
                        reactivate.setString(1, hasSeat ? "registered" : "waitlisted");
                        reactivate.setObject(2, hasSeat ? null : LocalDateTime.ofInstant(now, ZoneOffset.UTC));
                        reactivate.setLong(3, cancelledId);
                        if (reactivate.executeUpdate() != 1) throw new SQLException("reactivation failed");
                    }
                    registrationId = cancelledId;
                } else {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO event_registrations (event_id,volunteer_id,status,waitlisted_at) "
                            + "VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                        insert.setLong(1, eventId);
                        insert.setLong(2, volunteerId);
                        insert.setString(3, hasSeat ? "registered" : "waitlisted");
                        insert.setObject(4, hasSeat ? null : LocalDateTime.ofInstant(now, ZoneOffset.UTC));
                        insert.executeUpdate();
                        try (ResultSet keys = insert.getGeneratedKeys()) {
                            if (!keys.next()) throw new SQLException("registration key missing");
                            registrationId = keys.getLong(1);
                        }
                    }
                }
                String status = hasSeat ? "confirmed" : "on the waitlist";
                try (PreparedStatement mail = connection.prepareStatement(
                        "INSERT INTO email_outbox (template,recipient_email,subject,body_text,related_type,related_id) "
                        + "VALUES ('event-registration',?,?,?,'registration',?)")) {
                    mail.setString(1, person.email());
                    mail.setString(2, "Cleanup registration: " + status);
                    mail.setString(3, "Your signup for " + title + " is " + status
                            + ". Please check the event page for updates.");
                    mail.setLong(4, registrationId);
                    mail.executeUpdate();
                }
                connection.commit();
                return hasSeat ? Result.REGISTERED : Result.WAITLISTED;
            } catch (SQLException | RuntimeException failure) {
                connection.rollback();
                throw failure;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
