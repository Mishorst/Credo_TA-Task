package com.credo.task.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

public class TestResultDao {
    private final String url;

    public TestResultDao(String dbFilePath) {
        try {
            Path p = Path.of(dbFilePath).toAbsolutePath();
            Files.createDirectories(p.getParent());
            this.url = "jdbc:sqlite:" + p;
            init();
        } catch (Exception e) {
            throw new RuntimeException("Failed to init DB", e);
        }
    }

    private void init() {
        try (Connection c = DriverManager.getConnection(url);
             Statement st = c.createStatement()) {

            st.execute("""
        CREATE TABLE IF NOT EXISTS test_results (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          test_name TEXT NOT NULL,
          status TEXT NOT NULL,
          execution_time DATETIME NOT NULL
        )
      """);

            st.execute("""
        CREATE UNIQUE INDEX IF NOT EXISTS ux_test_results_test_name
        ON test_results(test_name)
      """);

        } catch (SQLException e) {
            throw new RuntimeException("DB initialization error", e);
        }
    }

    public void upsert(String testName, String status, LocalDateTime when) {
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement("""
           INSERT INTO test_results (test_name, status, execution_time)
           VALUES (?, ?, ?)
           ON CONFLICT(test_name) DO UPDATE SET
             status = excluded.status,
             execution_time = excluded.execution_time
         """)) {
            ps.setString(1, testName);
            ps.setString(2, status);
            ps.setString(3, when.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB upsert error", e);
        }
    }
}
