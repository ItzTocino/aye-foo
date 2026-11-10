package org.communityapp.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

/**
 * Starts the built webapp in an embedded Tomcat for tests and local viewing.
 */
public final class EmbeddedTomcat {

    private EmbeddedTomcat() {
    }

    public static Tomcat start(Path webapp, int port) throws LifecycleException, IOException {
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("tomcat").toString());
        tomcat.setPort(port);
        tomcat.getConnector();
        tomcat.addWebapp("", webapp.toAbsolutePath().toString());
        tomcat.start();
        return tomcat;
    }

    public static int port(Tomcat tomcat) {
        return tomcat.getConnector().getLocalPort();
    }
}
