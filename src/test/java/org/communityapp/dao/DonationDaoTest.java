package org.communityapp.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.Introspector;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.communityapp.service.DonationService;
import org.junit.jupiter.api.Test;

class DonationDaoTest {
    @Test
    void donationProgressExposesPropertiesForJspExpressionLanguage() throws Exception {
        var descriptors = Introspector.getBeanInfo(DonationService.Progress.class)
                .getPropertyDescriptors();
        for (String property : new String[] {"amount", "goal", "count", "percent"}) {
            assertTrue(java.util.Arrays.stream(descriptors)
                    .anyMatch(descriptor -> descriptor.getName().equals(property)
                            && descriptor.getReadMethod() != null), property);
        }
    }

    @Test
    void totalsOnlyIncludeSuccessfulUsdAndDisplayBoundedProgress() throws Exception {
        AtomicReference<String> sql = new AtomicReference<>();
        DonationService.Progress progress = new DonationService(new DonationDao(source(325_050L, 4L, sql)))
                .progress();
        assertEquals("$3,250.50", progress.amount());
        assertEquals("$2,500.00", progress.goal());
        assertEquals(4L, progress.count());
        assertEquals(100L, progress.percent());
        assertTrue(sql.get().contains("currency = 'USD' AND status = 'succeeded'"));
        assertTrue(sql.get().contains("COALESCE(SUM(amount_cents), 0)"));
    }

    @Test
    void emptyLedgerIsZeroWithoutInventingPayments() throws Exception {
        DonationService.Progress progress = new DonationService(new DonationDao(source(0, 0,
                new AtomicReference<>()))).progress();
        assertEquals("$0.00", progress.amount());
        assertEquals(0L, progress.count());
        assertEquals(0L, progress.percent());
    }

    @Test
    void databaseFailurePropagatesForUnavailablePageState() {
        DataSource broken = proxy(DataSource.class, (name, args) -> {
            throw new SQLException("offline");
        });
        assertThrows(SQLException.class, () -> new DonationService(new DonationDao(broken)).progress());
    }

    @Test
    void malformedNegativeTotalsAreNotPresentedAsRealProgress() {
        assertThrows(SQLException.class, () -> new DonationDao(source(-5, 1,
                new AtomicReference<>())).succeededUsd());
    }

    @Test
    void overflowAndFractionalAggregatesFailClosed() {
        assertThrows(SQLException.class, () -> new DonationDao(source(
                new BigDecimal("9223372036854775808"), BigDecimal.ONE)).succeededUsd());
        assertThrows(SQLException.class, () -> new DonationDao(source(
                new BigDecimal("1.5"), BigDecimal.ONE)).succeededUsd());
    }

    @Test
    void nullAndInconsistentAggregatesFailClosed() {
        assertThrows(SQLException.class, () -> new DonationDao(source(null, BigDecimal.ONE)).succeededUsd());
        assertThrows(SQLException.class, () -> new DonationDao(source(BigDecimal.ZERO, BigDecimal.ONE))
                .succeededUsd());
        assertThrows(SQLException.class, () -> new DonationDao(source(BigDecimal.ONE, BigDecimal.ZERO))
                .succeededUsd());
    }

    private static DataSource source(long cents, long count, AtomicReference<String> sql) {
        return source(BigDecimal.valueOf(cents), BigDecimal.valueOf(count), sql);
    }

    private static DataSource source(BigDecimal cents, BigDecimal count) {
        return source(cents, count, new AtomicReference<>());
    }

    private static DataSource source(BigDecimal cents, BigDecimal count, AtomicReference<String> sql) {
        ResultSet row = proxy(ResultSet.class, new Call() {
            boolean first = true;
            @Override
            public Object invoke(String name, Object[] args) {
                if (name.equals("next")) {
                    boolean result = first;
                    first = false;
                    return result;
                }
                if (name.equals("getBigDecimal")) {
                    return (int) args[0] == 1 ? cents : count;
                }
                return null;
            }
        });
        PreparedStatement statement = proxy(PreparedStatement.class, (name, args) ->
                name.equals("executeQuery") ? row : null);
        Connection connection = proxy(Connection.class, (name, args) -> {
            if (name.equals("prepareStatement")) {
                sql.set((String) args[0]);
                return statement;
            }
            return null;
        });
        return proxy(DataSource.class, (name, args) ->
                name.equals("getConnection") ? connection : null);
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
