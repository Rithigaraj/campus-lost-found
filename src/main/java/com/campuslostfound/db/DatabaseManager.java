package com.campuslostfound.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages JDBC connectivity to the campus_lost_found MySQL database.
 * Connection settings are read from db.properties on the classpath.
 */
public final class DatabaseManager {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DatabaseManager.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "db.properties not found on the classpath. "
                        + "Expected file: src/main/resources/db.properties");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load db.properties", e);
        }
    }

    private DatabaseManager() {
    }

    /**
     * Opens and returns a new connection to the MySQL database.
     *
     * Settings come from db.properties but can be overridden by the
     * DB_URL, DB_USER and DB_PASSWORD environment variables (used by
     * Docker deployments).
     */
    public static Connection getConnection() throws SQLException {
        String url = envOrProp("DB_URL", "db.url");
        String user = envOrProp("DB_USER", "db.user");
        String password = envOrProp("DB_PASSWORD", "db.password");
        return DriverManager.getConnection(url, user, password);
    }

    private static String envOrProp(String envName, String propName) {
        String value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return PROPS.getProperty(propName, "");
    }

    /**
     * Verifies connectivity; returns true when the database is reachable.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
}
