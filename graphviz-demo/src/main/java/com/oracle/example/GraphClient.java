package com.oracle.example;

import javax.security.auth.login.LoginException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.uri.UriBuilder;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.micronaut.http.HttpRequest.GET;
import static io.micronaut.http.HttpRequest.POST;

/**
 * Micronaut client wrapper around Oracle Graph Visualization REST endpoints. See the Oracle Graph documentation
 * for details about that API:
 *
 * https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgdg/rest-endpoints-graph-visualization-application.html
 */
@Singleton
public class GraphClient {

  private static Logger log = LoggerFactory.getLogger(GraphClient.class);

  HttpClient httpClient;

  @Value("${oracle.graph-server.jdbc-url}")
  String jdbcUrl;

  @Value("${oracle.graph-server.username}")
  String username;

  @Value("${oracle.graph-server.password}")
  String password;

  Cookie cookie;

  public GraphClient(@Client("${oracle.graph-server.url}") HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public String query(String query) throws LoginException {
    return query(query, false);
  }

  public String query(String query, boolean isRetry) throws LoginException {
    if (cookie == null) {
      login();
    }
    URI uri = UriBuilder.of("/ui/v1/query")
        .queryParam("pgql", query)
        .queryParam("formatter", "gvt")
        .build();
    try {
      log.info("GET {}", uri);
      HttpResponse<String> response = httpClient.toBlocking().exchange(
          GET(uri)
              .cookie(cookie)
              .accept(MediaType.APPLICATION_JSON),
          String.class);
      log.info("received response - reading body");
      return response.getBody().orElseThrow(() -> new InternalError("response body could not be converted to string"));
    } catch (HttpClientResponseException e) {
      Object body = e.getResponse().body();
      if (e.getStatus() == HttpStatus.FORBIDDEN || e.getStatus() == HttpStatus.UNAUTHORIZED) {
        if (isRetry) {
          log.warn("authentication failed while trying to run query. Response = {}", body);
          throw new LoginException("authentication failed");
        }
        log.info("hit 401, invalidating cookie and retrying");
        cookie = null;
        return query(query, true);
      }
      if (e.getStatus() == HttpStatus.BAD_REQUEST) {
        throw new IllegalArgumentException(body != null ? body.toString() : "bad request");
      }
      log.warn("query failed. Response = {}", body);
      throw new InternalError("query failed");
    }
  }

  private void login() throws LoginException {
    log.info("login {}", username);
    Map<String, String> payload = new HashMap<>();
    payload.put("baseUrl", jdbcUrl);
    payload.put("username", username);
    payload.put("password", password);
    payload.put("pgqlDriver", "pgqlDriver");
    try {
      HttpResponse<String> response = httpClient.toBlocking().exchange(
          POST("/ui/v1/login", payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON),
          String.class);
      cookie = response.getCookie("JSESSIONID").orElseThrow(() -> new LoginException("no cookie in login response"));
      log.info("obtained cookie {}", cookie.getName());
    } catch (HttpClientResponseException e) {
      log.warn("login failed with status {}", e.getStatus(), e);
      throw new LoginException("login failed");
    }
  }
}
