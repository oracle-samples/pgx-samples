package com.oracle.scripts;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class RunCreateScripts {

  private static final Logger LOG = LoggerFactory.getLogger(RunCreateScripts.class);

  private static final String JDBC_URL = getEnvValue("JDBC_URL");

  private static final String USERNAME = getEnvValue("USERNAME");

  private static final String PASSWORD = getEnvValue("PASSWORD");

  private static final int TABLE_ALREADY_EXISTS = 1950;

  private static final int ORA_NAME_IS_ALREADY_USED = 955; //ORA-00955: name is already used by an existing object\

  private static String getEnvValue(String envName) {
    String property = System.getenv(envName);
    if (property == null) {
      throw new IllegalArgumentException(envName + " not set");
    }
    return property;
  }

  public static void main(String[] args) throws Exception {
    createHrTables();
    createHrPg();
  }

  private static void createHrTables() throws Exception  {
    try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
      StringBuilder buffer = new StringBuilder();
      InputStream is = RunCreateScripts.class.getResourceAsStream("/dataset_property_graph/create_hr_dataset.sql");
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        String sql = line.trim();
        try (Statement stmt = conn.createStatement()) {
          LOG.info("Creating test data || {}", sql);
          try {
            stmt.execute(sql);
          } catch (SQLException e) {
            if (e.getErrorCode() == TABLE_ALREADY_EXISTS || e.getErrorCode() == ORA_NAME_IS_ALREADY_USED) {
              LOG.info("Table already exists, just ignored.");
            } else {
              throw e;
            }
          }
          buffer.setLength(0);
        }
      }
    }
  }

  private static void createHrPg() throws Exception {
    try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
      executeSqlScripts(conn, "/dataset_property_graph/create_hr_property_graph.sql");
    }
  }

  private static void executeSqlScripts(Connection conn, String sqlFile) throws Exception {
    String script = IOUtils.toString(RunCreateScripts.class.getResourceAsStream(sqlFile), StandardCharsets.UTF_8);
    LOG.info("Executing statements {}", script);
    try {
      conn.createStatement().execute(script);
    } catch (SQLException e) {
      LOG.info(e.getMessage());
    }
  }
}