package org.communityapp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    private static Properties defaults() {
        Properties properties = new Properties();
        properties.setProperty("db.url", "jdbc:mysql://default:3306/db");
        properties.setProperty("db.user", "default-user");
        properties.setProperty("db.password", "default-password");
        return properties;
    }

    @Test
    void load_whenEnvironmentIsSet_winsOverDefaults() {
        AppConfig config = AppConfig.load(defaults(), Map.of(
                "DB_URL", "jdbc:mysql://env:3306/db",
                "DB_USER", "env-user",
                "DB_PASSWORD", "env-password"));

        assertEquals("jdbc:mysql://env:3306/db", config.dbUrl());
        assertEquals("env-user", config.dbUser());
        assertEquals("env-password", config.dbPassword());
    }

    @Test
    void load_whenEnvironmentIsMissing_usesDefaults() {
        AppConfig config = AppConfig.load(defaults(), Map.of());

        assertEquals("jdbc:mysql://default:3306/db", config.dbUrl());
        assertEquals("default-user", config.dbUser());
    }

    @Test
    void load_whenEnvironmentIsBlank_usesDefaults() {
        AppConfig config = AppConfig.load(defaults(), Map.of("DB_USER", "  "));

        assertEquals("default-user", config.dbUser());
    }

    @Test
    void load_whenDefaultIsMissing_throws() {
        Properties properties = defaults();
        properties.remove("db.password");

        assertThrows(IllegalStateException.class, () -> AppConfig.load(properties, Map.of()));
    }

    @Test
    void toString_whenCalled_doesNotRevealThePassword() {
        AppConfig config = new AppConfig("jdbc:mysql://host/db?password=url-secret", "private-user", "hunter2");

        assertFalse(config.toString().contains("hunter2"));
        assertFalse(config.toString().contains("url-secret"));
        assertFalse(config.toString().contains("private-user"));
        assertEquals("AppConfig[dbUrl=***, dbUser=***, dbPassword=***]", config.toString());
    }
}
