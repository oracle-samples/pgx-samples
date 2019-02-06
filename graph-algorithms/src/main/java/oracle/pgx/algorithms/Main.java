/*
 * Copyright (C) 2013 - 2019 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import oracle.pgx.api.CompiledProgram;
import oracle.pgx.api.GraphBuilder;
import oracle.pgx.api.Pgx;
import oracle.pgx.api.PgxGraph;
import oracle.pgx.api.PgxSession;
import oracle.pgx.api.PgxVertex;
import oracle.pgx.api.ServerInstance;
import oracle.pgx.api.VertexProperty;
import oracle.pgx.common.types.IdType;
import oracle.pgx.common.types.PropertyType;
import oracle.pgx.config.PgxConfig;

public class Main {
  public static final double TOLERANCE = 0.001;
  public static final double DAMPING = 0.85;
  public static final int MAX_ITER_COUNT = 1000;
  public static final boolean NORMALIZE = true;

  public static void main(String[] args) throws Exception {
    try (PgxSession session = Pgx.createSession("pgx-algorithm-session")) {
      // Compile PGX algorithm
      String code = getProgram("Pagerank.java");
      CompiledProgram pagerankProgram = session.compileProgram(code);

      // Run algorithm
      PgxGraph graph = createGraph(session);
      VertexProperty<Object, Object> rank = graph.createVertexProperty(PropertyType.DOUBLE);
      pagerankProgram.run(graph, TOLERANCE, DAMPING, MAX_ITER_COUNT, NORMALIZE, rank);

      // Print results
      for (PgxVertex<Object> vertex : graph.getVertices()) {
        System.out.println("Pagerank " + vertex + " = " + rank.get(vertex));
      }
    }
  }

  public static String getProgram(String name) {
    URL resource = Main.class.getClassLoader().getResource(name);

    if (resource == null) {
      throw new IllegalStateException("Program '" + name + "' not found.");
    }

    return resource.getFile();
  }

  public static PgxGraph createGraph(PgxSession session) throws ExecutionException, InterruptedException {
    GraphBuilder<Integer> graphBuilder = session.createGraphBuilder(IdType.INTEGER);

    graphBuilder.addVertex(1).addLabel("person").setProperty("name", "Michael");
    graphBuilder.addVertex(2).addLabel("person").setProperty("name", "Rory");
    graphBuilder.addVertex(3).addLabel("person").setProperty("name", "Gabriel");
    graphBuilder.addVertex(4).addLabel("person").setProperty("name", "Daniel");
    graphBuilder.addVertex(5).addLabel("person").setProperty("name", "Victor");
    graphBuilder.addVertex(6).addLabel("person").setProperty("name", "Danny");
    graphBuilder.addVertex(7).addLabel("project").setProperty("name", "Compiler");
    graphBuilder.addVertex(8).addLabel("project").setProperty("name", "ML");
    graphBuilder.addEdge(1, 7).setLabel("created");
    graphBuilder.addEdge(2, 8).setLabel("created");
    graphBuilder.addEdge(3, 7).setLabel("created");
    graphBuilder.addEdge(1, 3).setLabel("knows");
    graphBuilder.addEdge(2, 6).setLabel("knows");
    graphBuilder.addEdge(3, 4).setLabel("knows");

    return graphBuilder.build();
  }
}
