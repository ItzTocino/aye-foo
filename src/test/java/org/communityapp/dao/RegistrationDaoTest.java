package org.communityapp.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

/** Checks write sequencing and rollback; populated MySQL concurrency remains an integration gate. */
class RegistrationDaoTest {
    private static final Instant NOW = Instant.parse("2026-11-09T19:00:00Z");
    private static final RegistrationDao.Applicant PERSON =
            new RegistrationDao.Applicant("Ada", "Lovelace", "ada@example.org", null, null, null);

    @Test
    void availableEventReservesSeatAndQueuesConfirmationBeforeCommit() throws Exception {
        JdbcScenario scenario = new JdbcScenario();
        assertEquals(RegistrationDao.Result.REGISTERED, scenario.dao().register("clean-bay", PERSON, NOW));
        assertEquals(1, scenario.seatsReserved);
        assertEquals(1, scenario.registrationsWritten);
        assertEquals(1, scenario.emailsQueued);
        assertTrue(scenario.committedAfterEmail);
    }

    @Test
    void fullEventWaitlistsWithoutConsumingSeat() throws Exception {
        JdbcScenario scenario = new JdbcScenario();
        scenario.full = true;
        assertEquals(RegistrationDao.Result.WAITLISTED, scenario.dao().register("clean-bay", PERSON, NOW));
        assertEquals(0, scenario.seatsReserved);
        assertEquals("waitlisted", scenario.writtenStatus);
        assertTrue(scenario.waitlistedTimestampSet);
        assertEquals(1, scenario.emailsQueued);
    }

    @Test
    void duplicateDoesNotTakeSeatOrQueueMail() throws Exception {
        JdbcScenario scenario = new JdbcScenario();
        scenario.existing = "registered";
        assertEquals(RegistrationDao.Result.DUPLICATE, scenario.dao().register("clean-bay", PERSON, NOW));
        assertEquals(0, scenario.seatsReserved);
        assertEquals(0, scenario.emailsQueued);
        assertTrue(scenario.rolledBack);
    }

    @Test
    void cancelledSignupIsReactivatedWithoutSecondUniqueRow() throws Exception {
        JdbcScenario scenario = new JdbcScenario();
        scenario.existing = "cancelled";
        assertEquals(RegistrationDao.Result.REGISTERED, scenario.dao().register("clean-bay", PERSON, NOW));
        assertEquals(1, scenario.seatsReserved);
        assertEquals(0, scenario.registrationsWritten);
        assertEquals(1, scenario.reactivations);
        assertEquals(1, scenario.emailsQueued);
    }

    @Test
    void missingOrUnpublishedEventMakesNoWrite() throws Exception {
        JdbcScenario scenario = new JdbcScenario();
        scenario.missingEvent = true;
        assertEquals(RegistrationDao.Result.UNAVAILABLE, scenario.dao().register("clean-bay", PERSON, NOW));
        assertEquals(0, scenario.registrationsWritten);
        assertTrue(scenario.rolledBack);
    }

    @Test
    void outboxFailureRollsBackSeatAndRegistration() {
        JdbcScenario scenario = new JdbcScenario();
        scenario.failOutbox = true;
        assertThrows(SQLException.class, () -> scenario.dao().register("clean-bay", PERSON, NOW));
        assertEquals(0, scenario.emailsQueued);
        assertTrue(scenario.rolledBack);
        assertTrue(!scenario.committedAfterEmail);
    }

    private interface Action {
        Object call(String method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Action action) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (instance, method, arguments) -> action.call(method.getName(), arguments));
    }

    private static final class JdbcScenario {
        boolean full, missingEvent, failOutbox, rolledBack, committedAfterEmail, waitlistedTimestampSet;
        String existing, writtenStatus;
        int seatsReserved, registrationsWritten, emailsQueued, reactivations;

        RegistrationDao dao() {
            Connection connection = proxy(Connection.class, (method, args) -> switch (method) {
                case "prepareStatement" -> statement((String) args[0]);
                case "commit" -> { committedAfterEmail = emailsQueued == 1; yield null; }
                case "rollback" -> { rolledBack = true; yield null; }
                default -> null;
            });
            DataSource source = proxy(DataSource.class, (method, args) -> connection);
            return new RegistrationDao(source);
        }

        private PreparedStatement statement(String sql) {
            Map<Integer, Object> bound = new HashMap<>();
            return proxy(PreparedStatement.class, (method, args) -> switch (method) {
                case "setString", "setObject", "setLong" -> {
                    bound.put((int) args[0], args[1]);
                    yield null;
                }
                case "executeQuery" -> {
                    if (sql.startsWith("SELECT id, title FROM events"))
                        yield rows(missingEvent ? 0 : 1, Map.of(1, 4L, 2, "Cleanup"));
                    if (sql.equals("SELECT LAST_INSERT_ID()")) yield rows(1, Map.of(1, 9L));
                    if (sql.startsWith("SELECT id,status FROM event_registrations"))
                        yield rows(existing == null ? 0 : 1, Map.of(1, 8L, 2, existing == null ? "" : existing));
                    throw new AssertionError("unexpected SQL: " + sql);
                }
                case "executeUpdate" -> {
                    if (sql.startsWith("UPDATE events SET")) {
                        if (!full) seatsReserved++;
                        yield full ? 0 : 1;
                    }
                    if (sql.startsWith("INSERT INTO event_registrations")) {
                        registrationsWritten++;
                        writtenStatus = (String) bound.get(3);
                        waitlistedTimestampSet = bound.get(4) != null;
                    }
                    if (sql.startsWith("UPDATE event_registrations")) reactivations++;
                    if (sql.startsWith("INSERT INTO email_outbox")) {
                        if (failOutbox) throw new SQLException("outbox unavailable");
                        emailsQueued++;
                    }
                    yield 1;
                }
                case "getGeneratedKeys" -> rows(1, Map.of(1, 12L));
                default -> null;
            });
        }

        private ResultSet rows(int count, Map<Integer, Object> data) {
            int[] cursor = {0};
            return proxy(ResultSet.class, (method, args) -> switch (method) {
                case "next" -> cursor[0]++ < count;
                case "getLong" -> ((Number) data.get((int) args[0])).longValue();
                case "getString" -> data.get((int) args[0]);
                default -> null;
            });
        }
    }
}
