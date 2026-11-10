package org.communityapp.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.communityapp.service.ImpactService;
import org.junit.jupiter.api.Test;

class ImpactDaoTest {
    @Test
    void reportsDatedFactsAndKeepsFeaturedSelectionInDisplayOrder() throws Exception {
        List<ImpactDao.Metric> metrics = new ImpactService(new ImpactDao(source(new Object[][] {
                {"trash_lb", "Trash collected", "lb", "cleanup", true,
                        new BigDecimal("12.50"), java.sql.Date.valueOf("2026-10-01")},
                {"future", "No facts", "mi", "conservation", true,
                        BigDecimal.ZERO, null},
                {"people", "Volunteers", "people", "community", false,
                        new BigDecimal("2"), java.sql.Date.valueOf("2026-10-02")}
        }))).metrics();
        assertEquals(2, metrics.size());
        assertEquals("12.5", metrics.get(0).value());
        assertEquals(LocalDate.of(2026, 10, 1), metrics.get(0).lastFactOn());
        assertEquals(List.of(metrics.get(0)), new ImpactService(new ImpactDao(source(new Object[0][])))
                .featured(metrics));
    }

    @Test
    void emptyLedgerHasNoInventedImpact() throws Exception {
        assertTrue(new ImpactDao(source(new Object[0][])).metrics().isEmpty());
    }

    @Test
    void malformedAndNegativeMetricsFailClosed() {
        assertThrows(SQLException.class, () -> new ImpactDao(source(new Object[][] {
                {"trash", "Trash", "lb", "cleanup", true,
                        new BigDecimal("-1"), java.sql.Date.valueOf("2026-10-01")}
        })).metrics());
        assertThrows(SQLException.class, () -> new ImpactDao(source(new Object[][] {
                {"trash", "Trash", "lb", "unknown", true,
                        BigDecimal.ONE, java.sql.Date.valueOf("2026-10-01")}
        })).metrics());
    }

    @Test
    void databaseFailurePropagates() {
        DataSource unavailable = proxy(DataSource.class, (name, args) -> {
            throw new SQLException("offline");
        });
        assertThrows(SQLException.class, () -> new ImpactDao(unavailable).metrics());
    }

    @Test
    void configuresBoundedQueryAndClosesResourcesAfterTimeout() {
        AtomicInteger timeoutSeconds = new AtomicInteger();
        AtomicBoolean statementClosed = new AtomicBoolean();
        AtomicBoolean connectionClosed = new AtomicBoolean();
        PreparedStatement statement = proxy(PreparedStatement.class, (name, args) -> {
            if (name.equals("setQueryTimeout")) {
                timeoutSeconds.set((int) args[0]);
            } else if (name.equals("executeQuery")) {
                throw new SQLTimeoutException("query exceeded deadline");
            } else if (name.equals("close")) {
                statementClosed.set(true);
            }
            return null;
        });
        Connection connection = proxy(Connection.class, (name, args) -> {
            if (name.equals("prepareStatement")) {
                return statement;
            }
            if (name.equals("close")) {
                connectionClosed.set(true);
            }
            return null;
        });
        DataSource source = proxy(DataSource.class,
                (name, args) -> name.equals("getConnection") ? connection : null);

        assertThrows(SQLTimeoutException.class, () -> new ImpactDao(source).metrics());
        assertEquals(2, timeoutSeconds.get());
        assertTrue(statementClosed.get());
        assertTrue(connectionClosed.get());
    }

    private static DataSource source(Object[][] entries) {
        ResultSet rows = proxy(ResultSet.class, new Call() {
            int index = -1;
            @Override
            public Object invoke(String name, Object[] args) {
                if (name.equals("next")) {
                    return ++index < entries.length;
                }
                if (name.equals("getBoolean")) {
                    return entries[index][(int) args[0] - 1];
                }
                if (name.equals("getString") || name.equals("getBigDecimal")
                        || name.equals("getDate")) {
                    return entries[index][(int) args[0] - 1];
                }
                return null;
            }
        });
        PreparedStatement query = proxy(PreparedStatement.class,
                (name, args) -> name.equals("executeQuery") ? rows : null);
        Connection connection = proxy(Connection.class,
                (name, args) -> name.equals("prepareStatement") ? query : null);
        return proxy(DataSource.class,
                (name, args) -> name.equals("getConnection") ? connection : null);
    }

    interface Call {
        Object invoke(String name, Object[] args) throws Exception;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (object, method, args) -> call.invoke(method.getName(), args));
    }
}
