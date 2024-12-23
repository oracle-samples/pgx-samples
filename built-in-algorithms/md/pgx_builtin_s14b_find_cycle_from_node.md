# Find Cycle from Node

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s14b_find_cycle_from_node
- **Time Complexity:** O(V + E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(4 * V + E) with V = number of vertices, E = number of edges
- **Javadoc:**
  - [Analyst#findCycle(PgxGraph graph, PgxVertex<ID> src)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#findCycle_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_)
  - [Analyst#findCycle(PgxGraph graph, PgxVertex<ID> src, VertexSequence<ID> nodeSeq, EdgeSequence edgeSeq)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#findCycle_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex_oracle_pgx_api_VertexSequence_oracle_pgx_api_EdgeSequence_)

This implementation tries to find a cycle in a directed graph using the given vertex as starting point for the DFS traversal and will return the first cycle found, if there is one. In such case, the vertices and edges involved in the cycle will be returned in the order of visit. Restricting the DFS traversal to a single starting point means that some parts of the graph may not get explored.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `s` | node | source vertex for the search. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `cycle_nodes` | nodeSeq | vertex sequence holding the vertices in the cycle. |
| `cycle_edges` | edgeSeq | edge sequence holding the edges in the cycle. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | bool | true if there is a cycle in the graph, false otherwise. |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeSequence;
import oracle.pgx.algorithm.PgxEdge;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSequence;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.inDFS;

@GraphAlgorithm
public class FindCycleFromNode {
  public boolean findCycle(PgxGraph g, PgxVertex s, @Out VertexSequence cycleNodes, @Out EdgeSequence cycleEdges) {
    VertexProperty<PgxVertex> source = VertexProperty.create();
    VertexProperty<Long> deg = VertexProperty.create();
    VertexProperty<Boolean> inPath = VertexProperty.create();

    deg.setAll(PgxVertex::getOutDegree);
    inPath.setAll(false);
    Scalar<Boolean> foundCycle = Scalar.create(false);
    Scalar<PgxVertex> pivotNode = Scalar.create();

    inDFS(g, s).navigator(v -> !foundCycle.get()).filter(v -> v.getDegree() > 0).forward(v -> {
      inPath.set(v, true);

      v.getOutNeighbors().forEach(w -> {
        source.set(w, v);
        if (inPath.get(w)) {
          foundCycle.set(true);
          pivotNode.set(w);
        }
      });
    }).backwardFilter(v -> !foundCycle.get()).backward(v -> {
      v.getInNeighbors().forEach(w -> {
        if (deg.get(w) > 0) {
          deg.set(w, deg.get(w) - 1);
        }
      });
      pivotNode.set(source.get(v));
      if (deg.get(pivotNode.get()) == 0) {
        inPath.set(pivotNode.get(), false);
      }
    });

    if (foundCycle.get()) {
      PgxVertex firstInCycle = pivotNode.get();
      cycleNodes.pushFront(pivotNode.get());
      pivotNode.set(source.get(pivotNode.get()));

      Scalar<PgxEdge> e = Scalar.create();
      while (pivotNode.get() != firstInCycle) {
        cycleNodes.pushFront(pivotNode.get());

        pivotNode.get().getOutNeighbors().filter(inPath).forSequential(v -> e.set(v.edge()));
        cycleEdges.pushFront(e.get());

        pivotNode.set(source.get(pivotNode.get()));
      }
      cycleNodes.pushFront(pivotNode.get());
      pivotNode.get().getOutNeighbors().filter(inPath).forSequential(v -> e.set(v.edge()));

      cycleEdges.pushFront(e.get());
    }

    return foundCycle.get();
  }
}
```
