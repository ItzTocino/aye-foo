package org.communityapp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SqlScriptTest {

    @Test
    void statements_whenSemicolonInLineComment_doesNotSplit() {
        String script = """
                CREATE TABLE a (id INT);
                -- prune rows older than 24 h; this semicolon is not a terminator
                CREATE TABLE b (id INT);
                """;

        assertEquals(2, SqlScript.statements(script).size());
    }

    @Test
    void statements_whenSemicolonInString_keepsItInsideTheStatement() {
        List<String> statements = SqlScript.statements("INSERT INTO a (t) VALUES ('x;y'); SELECT 1;");

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("'x;y'"));
    }

    @Test
    void statements_whenBlockComment_ignoresContent() {
        String script = "/* why; this; has semicolons */ SELECT 1;";

        assertEquals(List.of("SELECT 1"), SqlScript.statements(script));
    }

    @Test
    void statements_whenQuotedIdentifier_keepsSemicolonInside() {
        List<String> statements = SqlScript.statements("SELECT `odd;name` FROM t;");

        assertEquals(1, statements.size());
        assertTrue(statements.get(0).contains("`odd;name`"));
    }

    @Test
    void statements_whenEscapedQuote_keepsStringOpen() {
        String script = "INSERT INTO a (t) VALUES ('it\\'s; fine'); SELECT 1;";

        assertEquals(2, SqlScript.statements(script).size());
    }

    @Test
    void statements_whenLastStatementHasNoSemicolon_stillReturnsIt() {
        assertEquals(List.of("SELECT 1"), SqlScript.statements("SELECT 1"));
    }

    @Test
    void statements_whenOnlyComments_returnsEmptyList() {
        assertTrue(SqlScript.statements("\n-- nothing to run\n/* or here */\n").isEmpty());
    }

    @Test
    void baselineMigration_whenSplit_hasOneStatementPerObject() throws IOException {
        Path migration = Path.of(System.getProperty("project.basedir"),
                "src/main/resources/db/migration/V001__init.sql");

        List<String> statements = SqlScript.statements(Files.readString(migration));

        assertEquals(21, statements.size(), "17 tables plus 4 views");
        assertTrue(statements.stream()
                .allMatch(statement -> statement.startsWith("CREATE TABLE") || statement.startsWith("CREATE VIEW")));
    }

    @Test
    void cacheVersionMigration_whenSplit_preservesExistingValue() throws IOException {
        Path migration = Path.of(System.getProperty("project.basedir"),
                "src/main/resources/db/migration/V002__initialize_content_cache_version.sql");
        List<String> statements = SqlScript.statements(Files.readString(migration));
        assertEquals(1, statements.size());
        assertTrue(statements.get(0).contains("ON DUPLICATE KEY UPDATE meta_value = meta_value"));
    }
}
