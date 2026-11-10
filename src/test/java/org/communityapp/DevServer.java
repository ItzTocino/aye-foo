package org.communityapp;

import java.nio.file.Path;
import org.apache.catalina.startup.Tomcat;
import org.communityapp.web.EmbeddedTomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves target/saveourworld on http://localhost:8080 for local review.
 */
public final class DevServer {

    private static final Logger log = LoggerFactory.getLogger(DevServer.class);
    private static final int PORT = 8080;

    private DevServer() {
    }

    public static void main(String[] args) throws Exception {
        Path webapp = Path.of("target/saveourworld");
        Tomcat tomcat = EmbeddedTomcat.start(webapp, PORT);
        log.info("SaveOurWorld on http://localhost:{}", EmbeddedTomcat.port(tomcat));
        tomcat.getServer().await();
    }
}
