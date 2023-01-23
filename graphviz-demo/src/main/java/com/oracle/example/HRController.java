package com.oracle.example;

import javax.security.auth.login.LoginException;

import java.util.Arrays;
import java.util.stream.Collectors;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import io.micronaut.web.router.exceptions.UnsatisfiedQueryValueRouteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/hr")
public class HRController {

  private static Logger log = LoggerFactory.getLogger(HRController.class);

  GraphClient graphClient;

  public HRController(GraphClient graphClient) {
    this.graphClient = graphClient;
  }

  @Get("/directs")
  @Produces(MediaType.APPLICATION_JSON)
  public String getDirects(@QueryValue String email) throws LoginException {
    String pgql = "select e from match ()-[e]->(x:employees) on MYHR where x.email = '" + email + "'";
    log.info("running {}", pgql);
    return graphClient.query(pgql);
  }

  @Get("/neighbors")
  @Produces(MediaType.APPLICATION_JSON)
  public String getNeighbors(@QueryValue String ids) throws LoginException {
    ids = Arrays.stream(ids.split(",")).map(String::trim).collect(Collectors.joining("','"));
    String pgql = "select e from match (x) -[e]-> () on MYHR where id(x) in ('" + ids + "')";
    log.info("running {}", pgql);
    return graphClient.query(pgql);
  }

  @Error(global = true)
  public HttpResponse<JsonError> error(HttpRequest request, Throwable e) {
    log.error("processing exception {}", e.getMessage(), e);
    JsonError error = new JsonError(e.getMessage()).link(Link.SELF, Link.of(request.getUri()));
    if (e.getClass() == IllegalArgumentException.class || e.getClass() == UnsatisfiedQueryValueRouteException.class) {
      return HttpResponse.<JsonError>badRequest().body(error);
    }
    return HttpResponse.<JsonError>serverError().body(error);
  }
}
