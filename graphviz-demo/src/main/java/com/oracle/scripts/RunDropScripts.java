package com.oracle.scripts;

import oracle.pg.rdbms.pgql.PgqlConnection;
import oracle.pg.rdbms.pgql.PgqlStatement;
import oracle.pg.rdbms.pgql.PgqlToSqlException;
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
    dropHrTables();
    dropHrPgView();
  }

  private static void dropHrTables() throws Exception  {
    try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
      StringBuilder buffer = new StringBuilder();
      InputStream is = RunDropScripts.class.getResourceAsStream("/pgview_dataset/drop_hr.sql");
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

  private static void dropHrPgView() throws Exception {
    try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
      conn.setAutoCommit(false);
      PgqlConnection pgqlConnection = PgqlConnection.getConnection(conn);
      try (PgqlStatement statement = pgqlConnection.createStatement()) {
        String dropPgql = "DROP PROPERTY GRAPH MYHR";
        statement.execute(dropPgql);
        LOG.info("Dropping test data || {}", dropPgql);
      } catch (PgqlToSqlException e) {
        if (e.getMessage().contains("does not exist")) {
          LOG.info("MYHR Graph does not exists.");
        } else {
          throw e;
        }
      }
    }
  }
}