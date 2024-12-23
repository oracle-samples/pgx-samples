# Bidirectional Dijkstra Algorithm (ignoring edge directions)

- **Category:** path finding
- **Algorithm ID:** pgx_builtin_p2_single_source_single_destination_bidirectional_dijkstra
- **Time Complexity:** O(E + V log V) with V = number of vertices, E = number of edges
- **Space Requirement:** O(10 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#shortestPathDijkstraBidirectional(PgxGraph graph, ID srcId, ID dstId, EdgeProperty<java.lang.Double> cost)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#shortestPathDijkstraBidirectional_oracle_pgx_api_PgxGraph_ID_ID_oracle_pgx_api_EdgeProperty_)
  - [Analyst#shortestPathDijkstraBidirectional(PgxGraph graph, ID srcId, ID dstId, EdgeProperty<java.lang.Double> cost, VertexProperty<ID,​PgxVertex<ID>> parent, VertexProperty<ID,​PgxEdge> parentEdge)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#shortestPathDijkstraBidirectional_oracle_pgx_api_PgxGraph_ID_ID_oracle_pgx_api_EdgeProperty_oracle_pgx_api_VertexProperty_oracle_pgx_api_VertexProperty_)

This variant of the Dijkstra's algorithm searches for shortest path in two ways, it does a forward search from the source vertex and a backwards one from the destination vertex. If the path between the vertices exists, both searches will meet each other at an intermediate point.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `weight` | edgeProp<double> | edge property holding the (positive) weight of each edge in the graph. |
| `src` | node | the source vertex from the graph for the path. |
| `dst` | node | the destination vertex from the graph for the path. |

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
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Reduction.updateMinValue;
import static java.lang.Double.POSITIVE_INFINITY;
import oracle.pgx.algorithm.ControlFlow;

@GraphAlgorithm
public class BidirectionalDijkstra {
  public boolean bidirectionalDijkstra(PgxGraph g, EdgeProperty<Double> weight, PgxVertex src, PgxVertex dst,
      @Out VertexProperty<PgxVertex> parent, @Out VertexProperty<PgxEdge> parentEdge) {
    if (g.getNumVertices() == 0) {
      return false;
    } else if (src == dst) {
      return true;
    }

    long bidirectionalSearchLoop = g.getNumVertices();
    long updatePathLoop = g.getNumVertices();
    long numberOfStepsEstimatedForCompletion = bidirectionalSearchLoop + UpdatePathLoop;
    ControlFlow.setNumberOfStepsEstimatedForCompletion(numberOfStepsEstimatedForCompletion);

    // Temporary data structures
    VertexProperty<PgxVertex> rParent = VertexProperty.create();
    VertexProperty<PgxEdge> rParentEdge = VertexProperty.create();
    VertexProperty<Boolean> fFinalized = VertexProperty.create();
    VertexProperty<Boolean> rFinalized = VertexProperty.create();
    VertexProperty<Double> fCost = VertexProperty.create();
    VertexProperty<Double> hCost = VertexProperty.create();
    PgxMap<PgxVertex, Double> fReachable = PgxMap.create();
    PgxMap<PgxVertex, Double> rReachable = PgxMap.create();

    // sequentially initialize, otherwise compiler flags this algorithm as parallel in nature
    g.getVertices().forSequential(n -> {
      parent.set(n, PgxVertex.NONE);
      parentEdge.set(n, PgxEdge.NONE);
      rParent.set(n, PgxVertex.NONE);
      fFinalized.set(n, false);
      rFinalized.set(n, false);
      fCost.set(n, POSITIVE_INFINITY);
      hCost.set(n, POSITIVE_INFINITY);
    });

    fReachable.set(src, 0.0);
    rReachable.set(dst, 0.0);
    fCost.set(src, 0.0);
    hCost.set(dst, 0.0);

    Scalar<Double> curminfCost = Scalar.create(0.0);
    Scalar<Double> curminhCost = Scalar.create(0.0);
    double minCost = POSITIVE_INFINITY;
    double minUnitCost = 0.0; // This value is 1 for int version
    PgxVertex mid = PgxVertex.NONE;
    boolean terminate = false;

    // Bidirectional search loop
    while (!terminate && (fReachable.size() != 0) && (rReachable.size() != 0)) {
      if (fReachable.size() <= rReachable.size()) {
        PgxVertex fnext = fReachable.getKeyForMinValue();
        fReachable.remove(fnext);
        fFinalized.set(fnext, true);
        curminfCost.set(fCost.get(fnext));
        if (curminfCost.get() + curminhCost.get() + minUnitCost >= minCost) {
          terminate = true;
        }

        double fdist = fCost.get(fnext);
        fnext.getOutNeighbors().filter(v -> !fFinalized.get(v)).forSequential(v -> {
          PgxEdge e = v.edge();
          if (fdist + weight.get(e) + curminhCost.get() <= minCost && fCost.get(v) > fdist + weight.get(e)) {
            fCost.set(v, fdist + weight.get(e));
            fReachable.set(v, fCost.get(v));
            parent.set(v, fnext);
            parentEdge.set(v, e);
            if (hCost.get(v) != POSITIVE_INFINITY) {
              double newCost = fCost.get(v) + hCost.get(v);
              updateMinValue(minCost, newCost).andUpdate(mid, v);
            }
          }
        });
      } else {
        PgxVertex rnext = rReachable.getKeyForMinValue();
        rReachable.remove(rnext);
        rFinalized.set(rnext, true);
        curminhCost.set(hCost.get(rnext));
        if (curminfCost.get() + curminhCost.get() + minUnitCost >= minCost) {
          terminate = true;
        }

        double rdist = hCost.get(rnext);
        rnext.getInNeighbors().filter(v -> !rFinalized.get(v)).forSequential(v -> {
          PgxEdge e = v.edge();
          if (rdist + weight.get(e) + curminfCost.get() <= minCost && hCost.get(v) > rdist + weight.get(e)) {
            hCost.set(v, rdist + weight.get(e));
            rReachable.set(v, hCost.get(v));
            rParent.set(v, rnext);
            rParentEdge.set(v, e);
            if (fCost.get(v) != POSITIVE_INFINITY) {
              double newCost = fCost.get(v) + hCost.get(v);
              updateMinValue(minCost, newCost).andUpdate(mid, v);
            }
          }
        });
      }
    }

    // if a path was found
    if (mid != PgxVertex.NONE) {
      // Update the 'parent' and 'parentEdge' property of all the vertices in the
      // path from mid to dst
      PgxVertex cur = mid;
      while (cur != dst) {
        PgxVertex prev = rParent.get(cur);
        parent.set(prev, cur);
        parentEdge.set(prev, rParentEdge.get(cur));
        cur = prev;
      }
      return true;
    }
    // No path was found
    return false;
  }
}
```
