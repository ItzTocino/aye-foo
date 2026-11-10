package org.communityapp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Database settings for one running instance: environment variables win over app.properties.
 */
public record AppConfig(String dbUrl, String dbUser, String dbPassword) {

    private static final String PROPERTIES = "/app.properties";

    public static AppConfig load() {
        return load(readDefaults(), System.getenv());
    }

    public static AppConfig load(Properties defaults, Map<String, String> environment) {
        return new AppConfig(
                value("DB_URL", "db.url", defaults, environment),
                value("DB_USER", "db.user", defaults, environment),
                value("DB_PASSWORD", "db.password", defaults, environment));
    }

    private static String value(String environmentKey, String propertyKey,
                                Properties defaults, Map<String, String> environment) {
        String fromEnvironment = environment.get(environmentKey);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment;
        }
        String fromDefaults = defaults.getProperty(propertyKey);
        if (fromDefaults == null) {
            throw new IllegalStateException(propertyKey + " has no value");
        }
        return fromDefaults;
    }

    private static Properties readDefaults() {
        Properties defaults = new Properties();
        try (InputStream input = AppConfig.class.getResourceAsStream(PROPERTIES)) {
            if (input == null) {
                throw new IllegalStateException(PROPERTIES + " is missing from the classpath");
            }
            defaults.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + PROPERTIES, e);
        }
        return defaults;
    }

    @Override
    public String toString() {
        return "AppConfig[dbUrl=***, dbUser=***, dbPassword=***]";
    }
}
