# Prim's Algorithm

- **Category:** classic graph algorithms
- **Algorithm ID:** pgx_builtin_a1_prim
- **Time Complexity:** O(E + V log V) with V = number of vertices, E = number of edges
- **Space Requirement:** O(2 * E + V) with V = number of vertices, E = number of edges
- **Javadoc:** 
  - [Analyst#prim(PgxGraph graph, EdgeProperty<java.lang.Double> weight)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#prim-oracle.pgx.api.PgxGraph-oracle.pgx.api.EdgeProperty-)
  - [Analyst#prim(PgxGraph graph, EdgeProperty<java.lang.Double> weight, EdgeProperty<java.lang.Boolean> mst)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#prim-oracle.pgx.api.PgxGraph-oracle.pgx.api.EdgeProperty-oracle.pgx.api.EdgeProperty-)

This implementation of Prim's algorithm works on undirected graphs that are connected and have no multi-edges (i.e. more than one edge connecting the same pair of vertices). The algorithm computes the minimum spanning tree (MST) of the graph using the weights associated to each edge. A minimum spanning tree is a subset of the edges that connects all the vertices in the graph such that it minimizes the total weight associated to the edges.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `weight` | edgeProp<double> | edge property holding the weight of each edge in the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `in_mst` | edgeProp<bool> | edge property holding the edges belonging to the minimum spanning tree of the graph (i.e. all the edges with in_mst=true). |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | double | the total weight associated to the MST. |

## Code

```java
/*
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxMap;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class Prim {
  public double mst(PgxGraph g, EdgeProperty<Double> weight, @Out EdgeProperty<Boolean> inMst) {
    if (g.getNumVertices() == 0) {
      return 0.0;
    }

    PgxMap<PgxEdge, Double> q = PgxMap.create();
    PgxVertex root = g.getRandomVertex();

    root.getNeighbors().filter(n -> n != root).forSequential(n -> {
      PgxEdge e = n.edge();
      q.set(e, weight.get(e));
    });

    VertexProperty<Boolean> processed = VertexProperty.create();
    processed.setAll(false);
    processed.set(root, true);
    inMst.setAll(false);

    int numNodes = 1;
    double totalWeight = 0.0;

    while (numNodes < g.getNumVertices() && q.size() > 0) {
      PgxEdge newEdge = q.getKeyForMinValue();
      PgxVertex u = newEdge.sourceVertex();
      PgxVertex v = newEdge.destinationVertex();

      if (processed.get(v)) {
        PgxVertex tmp = v;
        v = u;
        u = tmp;
      }

      q.remove(newEdge);

      if (processed.get(v) != processed.get(u)) {
        inMst.set(newEdge, true);
        processed.set(v, true);
        totalWeight += weight.get(newEdge);

        v.getNeighbors().forEach(n -> {
          PgxEdge e = n.edge();
          if (processed.get(n)) {
            q.remove(e);
          } else {
            q.set(e, weight.get(e));
          }
        });
        numNodes++;
      }
    }

    return totalWeight;
  }
}
```
