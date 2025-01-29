# Dijkstra Multi-Destination Algorithm

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_p1c_single_source_all_destinations_dijkstra
- **Time Complexity:** O(E + V log V) with V = number of vertices, E = number of edges
- **Space Requirement:** O(4 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#singleSourceMultiDestination(PgxGraph graph, PgxVertex src, EdgeProperty cost)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#shortestPathMultiDestinationDijkstra_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_EdgeProperty_)
  - [Analyst#singleSourceMultiDestination(PgxGraph graph, PgxVertex src, EdgeProperty cost, VertexProperty distance, VertexProperty> parent, VertexProperty parentEdge)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#shortestPathMultiDestinationDijkstra_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_)

This variant of the Dijkstra's algorithm tries to find the shortest path ignoring edges directions for directed graphs while also taking into account a filter expression, which will add restrictions over the potential edges when looking for the shortest path between the source and destination vertices.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `weight` | edgeProp<double> | edge property holding the (positive) weight of each edge in the graph. |
| `root` | node | the source vertex from the graph for the path. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `dist` | vertexProp<node> | vertex property holding the distance to the source vertex for each vertex in the graph. |
| `parent` | vertexProp<node> | vertex property holding the parent vertex of the each vertex in the shortest path. |
| `parent_edge` | vertexProp<edge> | vertex property holding the edge ID linking the current vertex in the path with the previous vertex in the path. |

## Code

```java
/*
 * Copyright (C) 2013 - 2025 Oracle and/or its affiliates. All rights reserved.
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
import oracle.pgx.algorithm.ControlFlow;


@GraphAlgorithm
public class DijkstraMultiDestination {
  public void newDijkstra(PgxGraph g, EdgeProperty<Double> weight, PgxVertex root,
      @Out VertexProperty<Double> distance, @Out VertexProperty<PgxVertex> parent,
      @Out VertexProperty<PgxEdge> parentEdge) {

    VertexProperty<Boolean> reached = VertexProperty.create();

    // sequentially initialize, otherwise compiler flags this algorithm as
    //parallel in nature
    g.getVertices().forSequential(n -> {
      parent.set(n, PgxVertex.NONE);
      parentEdge.set(n, PgxEdge.NONE);
      reached.set(n, false);
      distance.set(n, Double.POSITIVE_INFINITY);
    });

    long searchLoop = g.getNumVertices();
    ControlFlow.setNumberOfStepsEstimatedForCompletion(searchLoop);

    distance.set(root, 0d);

    PgxMap<PgxVertex, Double> reachable = PgxMap.create();
    reachable.set(root, 0d);

    // Search loop
    while (reachable.size() > 0) {
      PgxVertex next = reachable.getKeyForMinValue();
      reached.set(next, true);
      double dist = reachable.get(next);
      reachable.remove(next);
      next.getOutNeighbors().filter(v -> !reached.get(v)).forSequential(v -> {
        PgxEdge e = v.edge();
        if (!reachable.containsKey(v) || reachable.get(v) > dist + weight.get(e)) {
          reachable.set(v, dist + weight.get(e));
          distance.set(v, dist + weight.get(e));
          parent.set(v, next);
          parentEdge.set(v, e);
        }
      });
    }
  }
}
```
