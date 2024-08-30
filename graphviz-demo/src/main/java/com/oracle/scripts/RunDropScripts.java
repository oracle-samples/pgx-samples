package com.oracle.scripts;

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

public class RunDropScripts {

  private static final Logger LOG = LoggerFactory.getLogger(RunDropScripts.class);

  private static final String JDBC_URL = getEnvValue("JDBC_URL");

  private static final String USERNAME = getEnvValue("USERNAME");

  private static final String PASSWORD = getEnvValue("PASSWORD");

  private static final int TABLE_OR_VIEW_DOES_NOT_EXISTS = 942;

  private static final int SEQUENCE_DOES_NOT_EXISTS = 2289;

  private static String getEnvValue(String envName) {
    String property = System.getenv(envName);
    if (property == null) {
      throw new IllegalArgumentException(envName + " not set");
    }
    return property;
  }

  public static void main(String[] args) throws Exception { 
    dropHrPg();
    dropHrTables();
  }

  private static void dropHrTables() throws Exception  {
    try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
      StringBuilder buffer = new StringBuilder();
      InputStream is = RunDropScripts.class.getResourceAsStream("/dataset_property_graph/drop_hr.sql");
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        String sql = line.trim();
        try (Statement stmt = conn.createStatement()) {
          LOG.info("Dropping test data || {}", sql);
          try {
            stmt.execute(sql);
          } catch (SQLException e) {
            if (e.getErrorCode() == TABLE_OR_VIEW_DOES_NOT_EXISTS || e.getErrorCode() == SEQUENCE_DOES_NOT_EXISTS) {
              LOG.info("Table, view or sequence doest not exists, just ignored.");
            } else {
              throw e;
            }
          }
          buffer.setLength(0);
        }
      }
    }
  }

  private static void dropHrPg() throws Exception {
    try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
      String dropPg = "DROP PROPERTY GRAPH MYHR";
      LOG.info("Dropping test data || {}", dropPg);
      try {
        conn.createStatement().execute(dropPg);
      } catch (SQLException e) {
        LOG.info(e.getMessage());
      }
    }
  }
}