package org.communityapp.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

/** Exercises the versioned cache boundary with deterministic JDBC results. */
class ContentDaoTest {

    @Test
    void missHitAndVersionChange_rebuildOnlyWhenNeeded() throws Exception {
        Database db = new Database();
        db.version = "1";
        db.partnerName = "Confirmed partner";
        ContentDao dao = new ContentDao(db.source());

        ContentDao.Snapshot first = dao.publicContent();
        assertEquals("Confirmed partner", first.partners().get(0).name());
        assertEquals(1, db.partnerQueries.get());
        assertSame(first, dao.publicContent());
        assertEquals(1, db.partnerQueries.get(), "cache hit must not query content again");

        db.version = "2";
        db.partnerName = null;
        ContentDao.Snapshot refreshed = dao.publicContent();
        assertNotSame(first, refreshed);
        assertTrue(refreshed.partners().isEmpty(), "removed partner must disappear after version bump");
        assertEquals(2, db.partnerQueries.get());
        assertSame(refreshed, dao.publicContent());
    }

    @Test
    void unavailableOrMissingVersion_neverServesStaleSnapshot() throws Exception {
        Database db = new Database();
        db.version = "7";
        db.partnerName = "Confirmed partner";
        ContentDao dao = new ContentDao(db.source());
        dao.publicContent();

        db.fail = true;
        assertThrows(SQLException.class, dao::publicContent);
        db.fail = false;
        db.version = null;
        assertTrue(assertThrows(SQLException.class, dao::publicContent)
                .getMessage().contains("not initialized"));
        db.version = "invalid";
        assertTrue(assertThrows(SQLException.class, dao::publicContent)
                .getMessage().contains("invalid cache_version"));
        db.version = "-1";
        assertTrue(assertThrows(SQLException.class, dao::publicContent)
                .getMessage().contains("negative"));
        db.version = "7";
        assertEquals("Confirmed partner", dao.publicContent().partners().get(0).name());
    }

    @Test
    void concurrentChangeDuringRead_retriesInsteadOfCachingMixedContent() throws Exception {
        Database db = new Database();
        db.version = "1";
        db.partnerName = "Confirmed partner";
        db.bumpOnPartnerRead = 1;
        ContentDao.Snapshot snapshot = new ContentDao(db.source()).publicContent();
        assertEquals(2, snapshot.version());
        assertEquals(2, db.partnerQueries.get());
    }

    @Test
    void constantlyChangingContent_failsWithoutPublishingPartialSnapshot() {
        Database db = new Database();
        db.version = "1";
        db.bumpOnPartnerRead = 3;
        assertTrue(assertThrows(SQLException.class, () -> new ContentDao(db.source()).publicContent())
                .getMessage().contains("changed during snapshot read"));
        assertEquals(2, db.partnerQueries.get(), "retry budget is bounded");
    }

    @Test
    void malformedPartnerUrls_areNotPublishedAsLinks() throws Exception {
        Database db = new Database();
        db.version = "1";
        db.partnerName = "Confirmed partner";
        ContentDao dao = new ContentDao(db.source());
        for (String url : List.of("https://.", "https://-invalid", "https://example..org",
                "https://example.org:444", "javascript:alert(1)")) {
            db.partnerUrl = url;
            db.version = Long.toString(Long.parseLong(db.version) + 1);
            assertEquals(null, dao.publicContent().partners().get(0).websiteUrl(), url);
        }
        db.partnerUrl = "https://example.org/partners?ref=community";
        db.version = "10";
        assertEquals(db.partnerUrl, dao.publicContent().partners().get(0).websiteUrl());
    }

    /** Minimal query-aware JDBC substitute; no external database or new test dependency. */
    private static final class Database {
        volatile String version;
        volatile String partnerName;
        volatile String partnerUrl = "https://example.org";
        volatile boolean fail;
        volatile int bumpOnPartnerRead;
        final AtomicInteger partnerQueries = new AtomicInteger();

        DataSource source() {
            return proxy(DataSource.class, (proxy, method, args) -> {
                if (method.getName().equals("getConnection")) {
                    if (fail) {
                        throw new SQLException("database unavailable");
                    }
                    return connection();
                }
                return standard(method.getName());
            });
        }

        Connection connection() {
            return proxy(Connection.class, (proxy, method, args) -> {
                if (method.getName().equals("prepareStatement")) {
                    return statement((String) args[0]);
                }
                return standard(method.getName());
            });
        }

        PreparedStatement statement(String sql) {
            return proxy(PreparedStatement.class, (proxy, method, args) -> {
                if (method.getName().equals("executeQuery")) {
                    if (sql.contains("FROM app_meta")) {
                        return rows(version == null ? List.of() : List.<String[]>of(new String[]{version}));
                    }
                    if (sql.contains("FROM initiatives")) {
                        return rows(List.of());
                    }
                    if (sql.contains("FROM partners")) {
                        partnerQueries.incrementAndGet();
                        String name = partnerName;
                        if (bumpOnPartnerRead > 0) {
                            bumpOnPartnerRead--;
                            version = Long.toString(Long.parseLong(version) + 1);
                        }
                        return rows(name == null ? List.of() : List.<String[]>of(
                                new String[]{name, "Approved description", partnerUrl}));
                    }
                    throw new SQLException("unexpected query: " + sql);
                }
                return standard(method.getName());
            });
        }

        ResultSet rows(List<String[]> data) {
            AtomicInteger index = new AtomicInteger(-1);
            return proxy(ResultSet.class, (proxy, method, args) -> {
                if (method.getName().equals("next")) {
                    return index.incrementAndGet() < data.size();
                }
                if (method.getName().equals("getString")) {
                    return data.get(index.get())[(Integer) args[0] - 1];
                }
                return standard(method.getName());
            });
        }

        private static Object standard(String method) {
            if (method.equals("close")) {
                return null;
            }
            if (method.equals("isClosed")) {
                return false;
            }
            throw new UnsupportedOperationException(method);
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
        }
    }
}
