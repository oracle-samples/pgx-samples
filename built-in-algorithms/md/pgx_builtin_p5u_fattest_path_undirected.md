# Fattest Path (ignoring edge directions)

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_p5u_fattest_path_undirected
- **Time Complexity:** O(E + V log V) with V = number of vertices, E = number of edges
- **Space Requirement:** O(4 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#fattestPath(PgxGraph graph, ID rootId, EdgeProperty<java.lang.Double> capacity)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#fattestPath_oracle_pgx_api_PgxGraph_ID_oracle_pgx_api_EdgeProperty_)
  - [Analyst#fattestPath(PgxGraph graph, ID rootId, EdgeProperty<java.lang.Double> capacity, VertexProperty<ID,​java.lang.Double> distance, VertexProperty<ID,​PgxVertex<ID>> parent, VertexProperty<ID,​PgxEdge> parentEdge)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#fattestPath_oracle_pgx_api_PgxGraph_ID_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_)
  - [Analyst#fattestPath(PgxGraph graph, PgxVertex<ID> root, EdgeProperty<java.lang.Double> capacity, boolean ignoreEdgeDirection)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#fattestPath_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_EdgeProperty_boolean_)
  - [Analyst#fattestPath(PgxGraph graph, PgxVertex<ID> root, EdgeProperty<java.lang.Double> capacity, VertexProperty<ID,​java.lang.Double> distance, VertexProperty<ID,​PgxVertex<ID>> parent, VertexProperty<ID,​PgxEdge> parentEdge, boolean ignoreEdgeDirection)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#fattestPath_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_boolean_)

The Fattest path algorithm can be regarded as a variant of Dijkstra's algorithm, it tries to find the fattest path between the given source and all the reachable vertices in the graph. The fatness of a path is equal to the minimum value of the capacity from the edges that take part in the path, thus a fattest path is conformed by the edges with the largest possible capacity.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `capacity` | edgeProp<double> | edge property holding the capacity of each edge in the graph. |
| `root` | node | the source vertex from the graph for the path. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `parent_node` | vertexProp<node> | vertex property holding the parent vertex of the each vertex in the fattest path. |
| `parent_edge` | vertexProp<edge> | vertex property holding the edge ID linking the current vertex in the path with the previous vertex in the path. |
| `fat` | vertexProp<double> | vertex property holding the capacity value of the fattest path up to the current vertex. The fatness value for the source vertex will be INF, while it will be 0 for all the vertices that are not reachable from the source. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

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

import static java.lang.Double.POSITIVE_INFINITY;

@GraphAlgorithm
public class FattestPathUndirected {
  public void fattestPathUndirected(PgxGraph g, EdgeProperty<Double> capacity, PgxVertex root,
      @Out VertexProperty<PgxVertex> parentNode, @Out VertexProperty<PgxEdge> parentEdge,
      @Out VertexProperty<Double> fat) {
    if (g.getNumVertices() == 0) {
      return;
    }

    // sequentially initialize, otherwise compiler flags this algorithm as parallel in nature
    g.getVertices().forSequential(n -> {
      parentNode.set(n, PgxVertex.NONE);
      parentEdge.set(n, PgxEdge.NONE);
      fat.set(n, (double) 0);
    });

    fat.set(root, POSITIVE_INFINITY);

    // create 'queue'
    PgxMap<PgxVertex, Double> q = PgxMap.create();
    q.set(root, POSITIVE_INFINITY);

    while (q.size() > 0) {
      PgxVertex u = q.getKeyForMaxValue();
      q.remove(u);

      u.getOutEdges().forEach(e -> {
        PgxVertex v = e.destinationVertex();
        double minCap = (fat.get(u) < capacity.get(e)) ? fat.get(u) : capacity.get(e);
        if (fat.get(v) < minCap) {
          fat.set(v, minCap);
          q.set(v, fat.get(v));
          parentNode.set(v, u);
          parentEdge.set(v, e);
        }
      });

      u.getInEdges().forEach(e -> {
        PgxVertex v = e.sourceVertex();
        double minCap = (fat.get(u) < capacity.get(e)) ? fat.get(u) : capacity.get(e);
        if (fat.get(v) < minCap) {
          fat.set(v, minCap);
          q.set(v, fat.get(v));
          parentNode.set(v, u);
          parentEdge.set(v, e);
        }
      });
    }
  }
}
```
