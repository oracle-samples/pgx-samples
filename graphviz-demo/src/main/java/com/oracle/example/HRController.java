package com.oracle.example;

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
  public String getDirects(@QueryValue String email) throws Exception {
    String query = "SELECT * FROM GRAPH_TABLE(\n"
        + "MYHR\n"
        + "MATCH (m)-[e]->(n IS employees WHERE n.email =''' || ? ||''')\n"
        + "COLUMNS (vertex_id(m) as m_id, edge_id(e) as e_id, vertex_id(n) as n_id)\n"
        + ")";
    return graphClient.query(query, email);
  }

  @Get("/neighbors")
  @Produces(MediaType.APPLICATION_JSON)
  public String getNeighbors(@QueryValue String ids) throws Exception {
    ids = Arrays.stream(ids.split(",")).map(String::trim).collect(Collectors.joining("','"));
    String query = "SELECT * FROM GRAPH_TABLE(\n"
        + "MYHR\n"
        + "MATCH (m)-[e]->(n)\n"
        + "WHERE JSON_value(vertex_id(m), ''$.ELEM_TABLE'') || json_query(vertex_id(m), ''$.KEY_VALUE'' returning varchar2) in (''' || ? || ''') \n"
        + "COLUMNS (vertex_id(m) as m_id, edge_id(e) as e_id, vertex_id(n) as n_id)\n"
        + ")";
    return graphClient.query(query, ids);
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
