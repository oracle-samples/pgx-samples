package com.oracle.example;

import javax.security.auth.login.LoginException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.example.response.Response;
import com.oracle.example.response.Result;
import com.oracle.example.response.TokenResponse;

import static io.micronaut.http.HttpRequest.POST;
import static io.micronaut.http.HttpRequest.PUT;

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

  @Value("${oracle.graph-server.jdbc-url}")
  String jdbcUrl;

  @Value("${oracle.graph-server.username}")
  String username;

  @Value("${oracle.graph-server.password}")
  String password;

  // JSON parser
  private final static ObjectMapper objectMapper = new ObjectMapper();

  // Authentication token;
  String token;

  public GraphClient(@Client("${oracle.graph-server.url}") HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public String query(String query) throws LoginException {
    return query(query, false);
  }

  public String query(String query, boolean isRetry) throws LoginException {
    if (token == null) {
      login();
    }

    log.info("POST /v2/runQuery");
    log.info("query {}", query);

    List<String> statements = new ArrayList<>();
    statements.add(query);

    Map<String, Object> payload = new HashMap<>();
    payload.put("driver", "PGQL_IN_DATABASE");
    payload.put("formatter", "GVT");
    payload.put("statements", statements);
    log.info("payload: {}", payload);

    CharSequence token = this.token;

    try {
      HttpResponse<String> response = httpClient.toBlocking().exchange(
          POST("/v2/runQuery", payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .bearerAuth(token),
          String.class);

      log.info("received response - reading body");

      String responseBody = response.getBody()
          .orElseThrow(() -> new InternalError("response body could not be converted to string"));
      log.info("response body: {}", responseBody);

      Response json = objectMapper.readValue(responseBody, Response.class);
      Result result = json.getResults().get(0);

      boolean responseIsSuccess = (boolean) result.getSuccess();

      if (!responseIsSuccess) {
        String errorMsg = new StringBuilder("query failed. Response = ").append(result.getError())
            .toString();
        log.warn(errorMsg);
        throw new InternalError("could not get response from server");
      }

      return (String) result.getResult();
    } catch (HttpClientResponseException e) {
      Object body = e.getResponse().body();
      if (e.getStatus() == HttpStatus.FORBIDDEN || e.getStatus() == HttpStatus.UNAUTHORIZED) {
        if (isRetry) {
          log.warn("authentication failed while trying to run query. Response = {}", body);
          throw new LoginException("authentication failed");
        }
        log.info("hit 401, refreshing token and retrying");
        refreshToken();
        return query(query, true);
      }
      if (e.getStatus() == HttpStatus.BAD_REQUEST) {
        throw new IllegalArgumentException(body != null ? body.toString() : "bad request");
      }
      log.warn("query failed. Response = {}", body);
      throw new InternalError("query failed");
    } catch (JsonProcessingException e) {
      log.warn("could not parse response", e);
      throw new InternalError("failed to parse response");
    }
  }

  private void login() throws LoginException {
    log.info("POST /auth/token");
    log.info("login {}", username);

    Map<String, Object> payload = new HashMap<>();
    payload.put("username", username);
    payload.put("password", password);
    payload.put("createSession", false);

    try {
      HttpResponse<String> response = httpClient.toBlocking().exchange(
          POST("/auth/token", payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON),
          String.class);

      TokenResponse json = objectMapper.readValue(
          response.getBody().orElseThrow(() -> new InternalError("response body could not be converted to string")),
          TokenResponse.class);
      token = json.getAccessToken();
      log.info("obtained token: {}", token);
    } catch (HttpClientResponseException e) {
      log.warn("login failed with status {}", e.getStatus(), e);
      throw new LoginException("login failed");
    } catch (JsonProcessingException e) {
      log.warn("could not parse server response", e);
      throw new LoginException("token refresh failed");
    }
  }

  private void refreshToken() throws LoginException {
    log.info("refreshToken {}", username);

    Map<String, Object> payload = new HashMap<>();
    payload.put("token", token);
    payload.put("createSession", false);

    try {
      HttpResponse<String> response = httpClient.toBlocking().exchange(
          PUT("/auth/token", payload)
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON),
          String.class);

      TokenResponse json = objectMapper.readValue(
          response.getBody().orElseThrow(() -> new InternalError("response body could not be converted to string")),
          TokenResponse.class);
      token = json.getAccessToken();
      log.info("obtained token: {}", token);
    } catch (HttpClientResponseException e) {
      log.warn("token refresh failed with status {}", e.getStatus(), e);
      throw new LoginException("token refresh failed");
    } catch (JsonProcessingException e) {
      log.warn("could not parse server response", e);
      throw new LoginException("token refresh failed");
    }
  }
}
