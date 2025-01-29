# Bellman-Ford (Ignoring edge directions)

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_p3u_single_source_all_destinations_bellman_ford
- **Time Complexity:** O(V + E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(6 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#shortestPathBellmanFordReverse(PgxGraph graph, PgxVertex<ID> src, EdgeProperty<java.lang.Double> cost)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#shortestPathBellmanFordReverse_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_EdgeProperty_)
  - [Analyst#shortestPathBellmanFord(PgxGraph graph, PgxVertex<ID> src, EdgeProperty<java.lang.Double> cost, VertexProperty<ID,​java.lang.Double> distance, VertexProperty<ID,​PgxVertex<ID>> parent, VertexProperty<ID,​PgxEdge> parentEdge, boolean ignoreEdgeDirection)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#shortestPathBellmanFord_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_boolean_)

Bellman-Ford algorithm tries to find the shortest path (if there is one) between the given source and destination vertices, while minimizing the distance or cost associated to each edge in the graph.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `len` | edgeProp<double> | edge property holding the weight of each edge in the graph. |
| `root` | node | the source vertex from the graph for the path. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `dist` | vertexProp<double> | vertex property holding the distance to the source vertex for each vertex in the graph. |
| `prev` | vertexProp<node> | vertex property holding the parent vertex of the each vertex in the shortest path. |
| `prev_edge` | vertexProp<edge> | vertex property holding the edge ID linking the current vertex in the path with the previous vertex in the path. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2025 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;
import oracle.pgx.algorithm.ControlFlow;

import static oracle.pgx.algorithm.PgxVertex.NONE;
import static oracle.pgx.algorithm.Reduction.updateMinValue;
import static java.lang.Double.POSITIVE_INFINITY;

@GraphAlgorithm
public class BellmanFordUndirected {
  public void bellmanFordUndirected(PgxGraph g, EdgeProperty<Double> len, PgxVertex root,
      @Out VertexProperty<Double> dist, @Out VertexProperty<PgxVertex> prev, @Out VertexProperty<PgxEdge> prevEdge) {
    VertexProperty<Boolean> updated = VertexProperty.create();
    VertexProperty<Boolean> updatedNxt = VertexProperty.create();
    VertexProperty<Double> distNxt = VertexProperty.create();
    boolean done = false;

    // initializations
    dist.setAll(v -> v == root ? 0.0 : POSITIVE_INFINITY);
    updated.setAll(v -> v == root);
    distNxt.setAll(dist::get);
    updatedNxt.setAll(updated::get);
    prev.setAll(NONE);
    prevEdge.setAll(PgxEdge.NONE);

    long counter = 0;
    long numVertices = g.getNumVertices();

    long initializations = 6 * numVertices;
    long searchLoop = numVertices - 1;
    long updateStep = 5 * numVertices;
    long numberOfStepsEstimatedForCompletion = initializations + (searchLoop * updateStep);
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);

    // Search loop
    while (!done && counter < numVertices - 1) {
      g.getVertices().filter(updated).forEach(n -> {
        n.getOutNeighbors().forEach(s -> {
          PgxEdge e = s.edge(); // the edge to s
          // updatedNxt becomes true only if distNxt is actually updated
          updateMinValue(s, distNxt, dist.get(n) + len.get(e)).andUpdate(s, updatedNxt, true).andUpdate(s, prev, n)
              .andUpdate(s, prevEdge, e);
        });
        n.getInNeighbors().forEach(s -> {
          if (!s.hasEdgeFrom(n)) {
            PgxEdge e = s.edge(); // the edge to s
            // updatedNxt becomes true only if distNxt is actually updated
            updateMinValue(s, distNxt, dist.get(n) + len.get(e)).andUpdate(s, updatedNxt, true).andUpdate(s, prev, n)
                .andUpdate(s, prevEdge, e);
          }
        });
      });

      // Update step
      dist.setAll(distNxt::get);
      updated.setAll(updatedNxt::get);
      updatedNxt.setAll(false);

      done = !g.getVertices().anyMatch(updated::get);
      counter++;
    }
  }
}
```
