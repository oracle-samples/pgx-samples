# Topological Sort

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s16a_topological_sort
- **Time Complexity:** O(V + E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(2 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#topologicalSort(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#topologicalSort-oracle.pgx.api.PgxGraph-)
  - [Analyst#topologicalSort(PgxGraph graph, VertexProperty<ID,java.lang.Integer> topoSort)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#topologicalSort-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-)

Topological sort tries to set an order over the vertices in a graph using the direction of the edges. A directed graph has a topological order if and only if it has no cycles, i.e. it is a directed acyclic graph. The algorithm visits the vertices in a DFS-like fashion to set up their order. The order of the vertices is returned as a vertex property, and the values will be set to -1 if there is a cycle in the graph.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `top_order` | vertexProp<int> | vertex property holding the topological order of each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | bool | true if the graph has a topological order, false otherwise. |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSequence;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class TopologicalSort {
  public boolean topologicalSort(PgxGraph g, @Out VertexProperty<Integer> topologicalOrder) {
    boolean sorted = true;
    VertexProperty<Long> deg = VertexProperty.create();
    VertexSequence s = VertexSequence.create();
    Scalar<Long> e = Scalar.create(g.getNumEdges());

    g.getVertices().filter(n -> n.getInDegree() == 0).forSequential(s::pushBack);

    topologicalOrder.setAll(-1);
    deg.setAll(PgxVertex::getInDegree);
    int visited = 0;

    while (s.size() > 0) {
      PgxVertex n = s.front();
      topologicalOrder.set(n, visited);
      visited++;
      n.getOutNeighbors().forSequential(nbr -> {
        deg.set(nbr, deg.get(nbr) - 1);
        e.set(e.get() - 1);
        if (deg.get(nbr) == 0) {
          s.pushBack(nbr);
        }
      });
      s.popFront();
    }

    if (e.get() > 0) {
      topologicalOrder.setAll(-1);
      sorted = false;
    }
    return sorted;
  }
}
```
