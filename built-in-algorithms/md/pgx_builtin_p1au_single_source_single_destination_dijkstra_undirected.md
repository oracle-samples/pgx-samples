# Classic Dijkstra Algorithm (ignoring edge directions)

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_p1au_single_source_single_destination_dijkstra_undirected
- **Time Complexity:** O(E + V log V) with V = number of vertices, E = number of edges
- **Space Requirement:** O(4 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#shortestPathDijkstra(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, EdgeProperty<java.lang.Double> cost, boolean ignoreEdgeDirection)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#shortestPathDijkstra_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_PgxVertex_oracle_pgx_api_EdgeProperty_boolean_)
  - [Analyst#shortestPathDijkstra(PgxGraph graph, PgxVertex<ID> src, PgxVertex<ID> dst, EdgeProperty<java.lang.Double> cost, VertexProperty<ID,​PgxVertex<ID>> parent, VertexProperty<ID,​PgxEdge> parentEdge, boolean ignoreEdgeDirection)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#shortestPathDijkstra_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_PgxVertex_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_boolean_)

Dijkstra's algorithm tries to find the shortest path (if there is one) between the given source and destination vertices, while minimizing the distance or cost associated to each edge in the graph.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `weight` | edgeProp<double> | edge property holding the (positive) weight of each edge in the graph. |
| `root` | node | the source vertex from the graph for the path. |
| `dest` | node | the destination vertex from the graph for the path. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `parent` | vertexProp<node> | vertex property holding the parent vertex of the each vertex in the shortest path. |
| `parent_edge` | vertexProp<edge> | vertex property holding the edge ID linking the current vertex in the path with the previous vertex in the path. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | bool | true if there is a path connecting source and destination vertices, false otherwise |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxMap;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class DijkstraUndirected {
  public boolean dijkstraUndirected(PgxGraph g, EdgeProperty<Double> weight, PgxVertex root, PgxVertex dest,
      @Out VertexProperty<PgxVertex> parent, @Out VertexProperty<PgxEdge> parentEdge) {
    if (g.getNumVertices() == 0) {
      return false;
    }

    VertexProperty<Boolean> reached = VertexProperty.create();

    // sequentially initialize, otherwise compiler flags this algorithm as
    //parallel in nature
    g.getVertices().forSequential(n -> {
      parent.set(n, PgxVertex.NONE);
      parentEdge.set(n, PgxEdge.NONE);
      reached.set(n, false);
    });

    PgxMap<PgxVertex, Double> reachable = PgxMap.create();
    reachable.set(root, 0d);

    //-------------------------------
    // look up the vertex
    //-------------------------------
    boolean found = false;

    while (!found && reachable.size() > 0) {
      PgxVertex next = reachable.getKeyForMinValue();
      if (next == dest) {
        found = true;
      } else {
        reached.set(next, true);
        double dist = reachable.get(next);
        reachable.remove(next);
        next.getNeighbors().filter(v -> !reached.get(v)).forSequential(v -> {
          PgxEdge e = v.edge();
          if (!reachable.containsKey(v) || reachable.get(v) > dist + weight.get(e)) {
            reachable.set(v, dist + weight.get(e));
            parent.set(v, next);
            parentEdge.set(v, e);
          }
        });
      }
    }

    // return false if not reachable
    return found;
  }
}
```
