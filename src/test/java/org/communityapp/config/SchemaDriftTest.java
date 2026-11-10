package org.communityapp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Fails the build when the spec, the baseline schema, and the migration disagree.
 */
class SchemaDriftTest {

    private static final Path ROOT = Path.of(System.getProperty("project.basedir"));
    private static final Path SPEC = ROOT.resolve("src/test/resources/schema-spec");
    private static final Path SCHEMA = ROOT.resolve("src/main/resources/db/schema.sql");
    private static final Path MIGRATION = ROOT.resolve("src/main/resources/db/migration/V001__init.sql");
    private static final String USE_STATEMENT = "USE saveourworld;";

    @Test
    void schemaSql_whenComparedToSpec_isIdenticalBelowTheHeader() throws IOException {
        List<String> blocks = specBlocks();

        assertEquals(specSchema(blocks), body(SCHEMA));
    }

    @Test
    void migration_whenComparedToSpec_isIdenticalBelowTheHeader() throws IOException {
        List<String> blocks = specBlocks();

        assertEquals(migrationSchema(blocks), body(MIGRATION));
    }

    private static String specSchema(List<String> blocks) {
        return blocks.get(0) + "\n\n" + blocks.get(1);
    }

    private static String migrationSchema(List<String> blocks) {
        String schema = blocks.get(0);
        int use = schema.indexOf(USE_STATEMENT);
        assertTrue(use > 0, "the spec schema must contain " + USE_STATEMENT);
        return schema.substring(use + USE_STATEMENT.length()).trim() + "\n\n" + blocks.get(1);
    }

    private static List<String> specBlocks() throws IOException {
        return List.of(normalize(Files.readString(SPEC.resolve("schema.sql"))),
                normalize(Files.readString(SPEC.resolve("views.sql"))));
    }

    private static String body(Path file) throws IOException {
        String text = normalize(Files.readString(file));
        int separator = text.indexOf("\n\n");
        assertTrue(separator > 0, file + " has no header");
        return text.substring(separator + 2).trim();
    }

    private static String normalize(String text) {
        String unix = text.replace("\r\n", "\n");
        StringBuilder normalized = new StringBuilder();
        for (String line : unix.split("\n", -1)) {
            normalized.append(line.stripTrailing()).append('\n');
        }
        return normalized.toString().trim();
    }
}
