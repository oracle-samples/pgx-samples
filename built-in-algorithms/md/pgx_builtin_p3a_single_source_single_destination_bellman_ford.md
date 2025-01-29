# Bellman-Ford Single Destination Algorithm

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_p3a_single_source_single_destination_bellman_ford
- **Time Complexity:** O(V + E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(6 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#shortestPathBellmanFordSingleDestination(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, EdgeProperty<java.lang.Double> cost)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#shortestPathBellmanFordReverse_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_EdgeProperty_)
  - [Analyst#shortestPathBellmanFordSingleDestination(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, EdgeProperty<java.lang.Double> cost, VertexProperty> parent, VertexProperty<ID,​PgxEdge> parentEdge)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#shortestPathBellmanFord_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_boolean_)

Bellman-Ford algorithm tries to find the shortest path (if there is one) between the given source and destination vertices, while minimizing the distance or cost associated to each edge in the graph.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `len` | edgeProp<double> | edge property holding the weight of each edge in the graph. |
| `root` | node | the source vertex from the graph for the path. |
| `dest` | node | the destination vertex from the graph for the path. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
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
public class BellmanFordSingleDestination {
  public boolean bellmanFordSingleDestination(PgxGraph g, EdgeProperty<Double> len, PgxVertex root, PgxVertex dest,
      @Out VertexProperty<PgxVertex> prev, @Out VertexProperty<PgxEdge> prevEdge) {

    // Temporary properties to track updates and next iteration values
    VertexProperty<Boolean> updated = VertexProperty.create();
    VertexProperty<Boolean> updatedNxt = VertexProperty.create();
    VertexProperty<Double> dist = VertexProperty.create();
    VertexProperty<Double> distNxt = VertexProperty.create();
    boolean done = false;
    boolean foundDest = false;

    long counter = 0;

    long numVertices = g.getNumVertices();
    long initializations = 6 * numVertices;
    long searchingStep = numVertices - 1;
    long updatingStep = 5 * numVertices;
    long numberOfStepsEstimatedForCompletion = initializations + (searchingStep * updatingStep);

    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);

    // Initializations
    dist.setAll(v -> v == root ? 0.0 : POSITIVE_INFINITY);
    updated.setAll(v -> v == root);
    distNxt.setAll(dist::get);
    updatedNxt.setAll(updated::get);
    prev.setAll(NONE);
    prevEdge.setAll(PgxEdge.NONE);

    // Search Loop
    while (!done && counter < numVertices - 1) {
      g.getVertices().filter(updated).forEach(n -> {
        n.getOutNeighbors().forEach(s -> {
          PgxEdge e = s.edge(); // the edge to s
          updateMinValue(s, distNxt, dist.get(n) + len.get(e))
            .andUpdate(s, updatedNxt, true)
            .andUpdate(s, prev, n)
            .andUpdate(s, prevEdge, e);
        });
      });
      dist.setAll(distNxt::get);
      updated.setAll(updatedNxt::get);
      updatedNxt.setAll(false);

      // Check if any vertices were updated in this iteration
      done = !g.getVertices().anyMatch(updated::get);
      counter++;
    }

    foundDest = dist.get(dest) != POSITIVE_INFINITY;

    return foundDest;
  }
}
```
