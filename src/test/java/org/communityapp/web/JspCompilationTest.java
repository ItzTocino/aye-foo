package org.communityapp.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.apache.jasper.JspC;
import org.junit.jupiter.api.Test;

/**
 * Translates every JSP and tag file, so a typo fails the build instead of a request.
 */
class JspCompilationTest {

    @Test
    void webapp_whenTranslated_hasEveryJspCompiled() throws Exception {
        Path webapp = Path.of(System.getProperty("webapp.source.dir")).toAbsolutePath();
        assertTrue(Files.isDirectory(webapp), "webapp directory is missing: " + webapp);

        Path output = Files.createTempDirectory("jspc");
        JspC compiler = new JspC();
        compiler.setUriroot(webapp.toString());
        compiler.setOutputDir(output.toString());
        compiler.setJavaEncoding("UTF-8");
        compiler.setCompile(false);
        compiler.setFailOnError(true);
        compiler.execute();

        long translated = countJavaFiles(output);
        assertTrue(translated >= 9, "expected every page and error view to translate, got " + translated);
    }

    private static long countJavaFiles(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            List<Path> generated = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList();
            return generated.size();
        }
    }
}
