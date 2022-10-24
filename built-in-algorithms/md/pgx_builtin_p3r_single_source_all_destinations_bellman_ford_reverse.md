# (Backwards) Bellman-Ford Algorithm

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_p3r_single_source_all_destinations_bellman_ford_reverse
- **Time Complexity:** O(V + E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(6 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#shortestPathBellmanFordReverse(PgxGraph graph, PgxVertex<ID> src, EdgeProperty<java.lang.Double> cost)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#shortestPathBellmanFordReverse-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.EdgeProperty-)
  - [Analyst#shortestPathBellmanFordReverse(PgxGraph graph, PgxVertex<ID> src, EdgeProperty<java.lang.Double> cost, VertexProperty<ID,java.lang.Double> distance, VertexProperty<ID,PgxVertex<ID>> parent, VertexProperty<ID,PgxEdge> parentEdge)](https://docs.oracle.com/en/database/oracle/property-graph/22.3/spgjv/oracle/pgx/api/Analyst.html#shortestPathBellmanFordReverse-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.EdgeProperty-oracle.pgx.api.VertexProperty-oracle.pgx.api.VertexProperty-oracle.pgx.api.VertexProperty-)

This variant of the Bellman-Ford algorithm tries to find the shortest path (if there is one) between the given source and destination vertices in a reversed fashion using the incoming edges instead of the outgoing, while minimizing the distance or cost associated to each edge in the graph.


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
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Reduction.updateMinValue;
import static java.lang.Double.POSITIVE_INFINITY;

@GraphAlgorithm
public class BellmanFordBackward {
  public void bellmanFordBackward(PgxGraph g, EdgeProperty<Double> len, PgxVertex root,
      @Out VertexProperty<Double> dist, @Out VertexProperty<PgxVertex> prev, @Out VertexProperty<PgxEdge> prevEdge) {
    VertexProperty<Boolean> updated = VertexProperty.create();
    VertexProperty<Boolean> updatedNxt = VertexProperty.create();
    VertexProperty<Double> distNxt = VertexProperty.create();
    boolean done = false;

    dist.setAll(v -> v == root ? 0.0 : POSITIVE_INFINITY);
    updated.setAll(v -> v == root);
    distNxt.setAll(dist::get);
    updatedNxt.setAll(updated::get);
    prev.setAll(PgxVertex.NONE);
    prevEdge.setAll(PgxEdge.NONE);

    while (!done) {
      g.getVertices().filter(updated).forEach(n -> n.getInNeighbors().forEach(s -> {
        PgxEdge e = s.edge(); // the edge to s
        // updatedNxt becomes true only if distNxt is actually updated
        updateMinValue(s, distNxt, dist.get(n) + len.get(e)).andUpdate(s, updatedNxt, true).andUpdate(s, prev, n)
            .andUpdate(s, prevEdge, e);
      }));

      dist.setAll(distNxt::get);
      updated.setAll(updatedNxt::get);
      updatedNxt.setAll(false);

      done = !g.getVertices().anyMatch(updated::get);
    }
  }
}
```
