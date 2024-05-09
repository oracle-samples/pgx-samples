package com.oracle.example;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.client.HttpClient;
import jakarta.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Micronaut client wrapper around Oracle Graph Visualization REST endpoints.
 * See the Oracle Graph documentation
 * for details about that API:
 *
 * https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgdg/rest-endpoints-graph-visualization-application.html
 */
@Singleton
public class GraphClient {

  private static Logger log = LoggerFactory.getLogger(GraphClient.class);

  HttpClient httpClient;

  @Value("${oracle.jdbc-url}")
  String jdbcUrl;

  @Value("${oracle.username}")
  String username;

  @Value("${oracle.password}")
  String password;

  private static final String anonymousFunction;

  static {
    try {
      anonymousFunction = IOUtils.toString(
          GraphClient.class.getResourceAsStream("/my_sqlgraph_json.sql"),
          StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("Cannot locate anonymous function file");
      throw new RuntimeException(e);     
    }
  }

  public String query(String query, String parameter) throws Exception {

    String wrapQuery = anonymousFunction + "SELECT MY_SQLGRAPH_JSON('" + query + "') as result from dual";
    String resultString = null;
    try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
      try (PreparedStatement ps = conn.prepareStatement(wrapQuery)) {
        ps.setString(1, parameter);
        log.info("running: {}", wrapQuery);
        log.info("replaced parameter: {}", parameter);
        try (ResultSet resultSet = ps.executeQuery()) {
          if (resultSet.next()) {
            resultString = resultSet.getString(1);
          }
        }
      }
    }
    return resultString;
  }
}
