# Hop Distance

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_p4_single_source_all_destinations_hop_distance
- **Time Complexity:** O(V + E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(3 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#shortestPathHopDist(PgxGraph graph, PgxVertex<ID> src)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#shortestPathHopDist-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-)
  - [Analyst#shortestPathHopDist(PgxGraph graph, PgxVertex<ID> src, VertexProperty<ID,java.lang.Double> distance, VertexProperty<ID,PgxVertex<ID>> parent, VertexProperty<ID,PgxEdge> parentEdge)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#shortestPathHopDist-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxVertex-oracle.pgx.api.VertexProperty-oracle.pgx.api.VertexProperty-oracle.pgx.api.VertexProperty-)

The Hop distance of two vertices S and V in a graph is the number of edges that are in a shortest path connecting them. This algorithm will return the distance of each vertex with respect to the given source vertex in the input and will also return the parent vertex and linking edge for each vertex. The returned information allows to trace back shortest paths from any reachable vertex to the source vertex.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `root` | node | the source vertex from the graph for the path. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `dist` | vertexProp<double> | vertex property holding the hop distance to the source vertex for each vertex in the graph. |
| `prev` | vertexProp<node> | vertex property holding the parent vertex of the each vertex in the shortest path. |
| `prev_edge` | vertexProp<edge> | vertex property holding the edge ID linking the current vertex in the path with the previous vertex in the path. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.currentLevel;
import static oracle.pgx.algorithm.Traversal.inBFS;

@GraphAlgorithm
public class HopDistance {
  public void hopDist(PgxGraph g, PgxVertex root, @Out VertexProperty<Double> dist, @Out VertexProperty<PgxVertex> prev,
      @Out VertexProperty<PgxEdge> prevEdge) {
    if (g.getNumVertices() == 0) {
      return;
    }

    dist.setAll(Double.POSITIVE_INFINITY);
    prev.setAll(PgxVertex.NONE);
    prevEdge.setAll(PgxEdge.NONE);
    dist.set(root, 0d);

    inBFS(g, root).forward(n -> {
      dist.set(n, (double) currentLevel());
      prev.set(n, n.parentVertex());
      prevEdge.set(n, n.parentEdge());
    });
  }
}
```
