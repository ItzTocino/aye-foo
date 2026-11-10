package org.communityapp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Checks source conventions on every machine.
 */
class CodeStyleTest {

    private static final Path SOURCES = Path.of(System.getProperty("project.basedir"), "src/main/java");
    private static final Path WEBAPP = Path.of(System.getProperty("project.basedir"), "src/main/webapp");

    private static final Map<String, Pattern> BANNED_IN_JAVA = Map.of(
            "createStatement()", Pattern.compile("createStatement\\("),
            "SELECT *", Pattern.compile("SELECT \\*"),
            "printStackTrace()", Pattern.compile("printStackTrace\\("),
            "System.out", Pattern.compile("System\\.out"),
            "Instant.now()", Pattern.compile("Instant\\.now"),
            "LocalDateTime.now()", Pattern.compile("LocalDateTime\\.now"),
            "TODO marker", Pattern.compile("TODO"));

    private static final Pattern SCRIPTLET = Pattern.compile("<%(?!@)");

    @Test
    void java_whenScanned_hasNoBannedPattern() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path source : files(SOURCES, ".java")) {
            String text = Files.readString(source);
            BANNED_IN_JAVA.forEach((name, pattern) -> {
                if (pattern.matcher(text).find()) {
                    violations.add(name + " in " + SOURCES.relativize(source));
                }
            });
        }

        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    @Test
    void java_whenOutsideDatabaseLayers_hasNoJdbcImport() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path source : files(SOURCES, ".java")) {
            String path = source.toString().replace('\\', '/');
            boolean databaseLayer = path.contains("/dao/") || path.contains("/db/");
            if (!databaseLayer && Files.readString(source).contains("import java.sql.")) {
                violations.add(SOURCES.relativize(source).toString());
            }
        }

        assertTrue(violations.isEmpty(), "java.sql outside dao/: " + String.join(", ", violations));
    }

    @Test
    void webapp_whenScanned_hasNoInlineStyleOrScriptlet() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String suffix : List.of(".jsp", ".tag")) {
            for (Path page : files(WEBAPP, suffix)) {
                String text = Files.readString(page);
                if (text.contains("style=\"")) {
                    violations.add("inline style in " + WEBAPP.relativize(page));
                }
                if (SCRIPTLET.matcher(text).find()) {
                    violations.add("scriptlet in " + WEBAPP.relativize(page));
                }
            }
        }

        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    @Test
    void css_whenScanned_hasNoImportant() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path stylesheet : files(WEBAPP.resolve("css"), ".css")) {
            if (Files.readString(stylesheet).contains("!important")) {
                violations.add(WEBAPP.relativize(stylesheet).toString());
            }
        }

        assertTrue(violations.isEmpty(), "!important in: " + String.join(", ", violations));
    }

    private static List<Path> files(Path root, String suffix) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .toList();
        }
    }
}
