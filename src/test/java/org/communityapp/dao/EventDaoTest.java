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
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class EventDaoTest {
    @Test
    void upcoming_filtersPublishedFutureEventsAndBindsUtcCutoff() throws Exception {
        AtomicReference<String> sql = new AtomicReference<>();
        AtomicReference<Object> bound = new AtomicReference<>();
        ResultSet rows = proxy(ResultSet.class, (method, args) -> {
            if (method.equals("next")) {
                return false;
            }
            if (method.equals("close")) {
                return null;
            }
            throw new UnsupportedOperationException(method);
        });
        PreparedStatement statement = proxy(PreparedStatement.class, (method, args) -> {
            if (method.equals("setObject")) {
                assertEquals(1, args[0]);
                bound.set(args[1]);
            }
            if (method.equals("executeQuery")) {
                return rows;
            }
            return null;
        });
        Connection connection = proxy(Connection.class, (method, args) -> {
            if (method.equals("prepareStatement")) {
                sql.set((String) args[0]);
                return statement;
            }
            return null;
        });
        DataSource source = proxy(DataSource.class, (method, args) -> {
            if (method.equals("getConnection")) {
                return connection;
            }
            return null;
        });
        assertTrue(new EventDao(source).upcoming(Instant.parse("2026-11-09T19:00:00Z")).isEmpty());
        assertTrue(sql.get().contains("status = 'published' AND starts_at > ?"));
        assertTrue(sql.get().contains("ORDER BY starts_at, id LIMIT 50"));
        assertEquals(LocalDateTime.of(2026, 11, 9, 19, 0), bound.get());
    }

    @Test
    void upstreamFailurePropagatesForExplicitUnavailableState() throws Exception {
        DataSource source = proxy(DataSource.class, (method, args) -> {
            throw new SQLException("database offline");
        });
        assertThrows(SQLException.class, () -> new EventDao(source).upcoming(Instant.now()));
    }

    @Test
    void eventDetailFiltersPublishedUpcomingAndBindsSlugAndUtc() throws Exception {
        AtomicReference<String> sql = new AtomicReference<>();
        AtomicReference<Object> slug = new AtomicReference<>();
        AtomicReference<Object> cutoff = new AtomicReference<>();
        ResultSet rows = proxy(ResultSet.class, (method, args) -> {
            if (method.equals("next")) {
                return false;
            }
            return null;
        });
        PreparedStatement statement = proxy(PreparedStatement.class, (method, args) -> {
            if (method.equals("setString")) {
                slug.set(args[1]);
            }
            if (method.equals("setObject")) {
                cutoff.set(args[1]);
            }
            if (method.equals("executeQuery")) {
                return rows;
            }
            return null;
        });
        Connection connection = proxy(Connection.class, (method, args) -> {
            if (method.equals("prepareStatement")) {
                sql.set((String) args[0]);
                return statement;
            }
            return null;
        });
        DataSource source = proxy(DataSource.class, (method, args) -> {
            if (method.equals("getConnection")) {
                return connection;
            }
            return null;
        });
        assertEquals(null, new EventDao(source).findUpcomingPublished("cleanup-1",
                Instant.parse("2026-11-09T19:00:00Z")));
        assertTrue(sql.get().contains("e.slug = ? AND e.status = 'published' AND e.starts_at > ?"));
        assertTrue(sql.get().contains("v_event_capacity"));
        assertEquals("cleanup-1", slug.get());
        assertEquals(LocalDateTime.of(2026, 11, 9, 19, 0), cutoff.get());
    }

    @Test
    void eventDetailMapsResultWithoutDriverTimezoneConversion() throws Exception {
        ResultSet rows = proxy(ResultSet.class, new Call() {
            private boolean first = true;
            @Override
            public Object invoke(String method, Object[] args) {
                if (method.equals("next")) {
                    boolean result = first;
                    first = false;
                    return result;
                }
                if (method.equals("getObject")) {
                    return (int) args[0] == 10 ? LocalDateTime.of(2026, 7, 15, 14, 0) : null;
                }
                if (method.equals("getString")) {
                    return switch ((int) args[0]) {
                        case 1 -> "Cleanup";
                        case 2 -> "River";
                        case 4 -> "Park";
                        case 12 -> "America/New_York";
                        default -> null;
                    };
                }
                if (method.equals("getInt")) {
                    return switch ((int) args[0]) {
                        case 13 -> 10;
                        case 14 -> 3;
                        case 15 -> 2;
                        default -> 0;
                    };
                }
                return null;
            }
        });
        PreparedStatement stmt = proxy(PreparedStatement.class, (method, args) ->
                method.equals("executeQuery") ? rows : null);
        Connection conn = proxy(Connection.class, (method, args) ->
                method.equals("prepareStatement") ? stmt : null);
        DataSource source = proxy(DataSource.class, (method, args) ->
                method.equals("getConnection") ? conn : null);
        EventDao.Detail event = new EventDao(source).findUpcomingPublished("river-cleanup",
                Instant.parse("2026-07-01T00:00:00Z"));
        assertEquals(Instant.parse("2026-07-15T14:00:00Z"), event.startsAt());
        assertEquals(null, event.endsAt());
        assertEquals(10, event.capacity());
        assertEquals(3, event.seatsLeft());
        assertEquals(2, event.waitlistCount());
    }

    interface Call {
        Object invoke(String method, Object[] args) throws Exception;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (object, method, args) -> call.invoke(method.getName(), args));
    }
}
