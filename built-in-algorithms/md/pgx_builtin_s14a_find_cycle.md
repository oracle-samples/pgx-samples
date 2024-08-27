# Find Cycle

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s14a_find_cycle
- **Time Complexity:** O(V * (V + E)) with V = number of vertices, E = number of edges
- **Space Requirement:** O(5 * V + E) with V = number of vertices, E = number of edges
- **Javadoc:** 
  - [Analyst#findCycle(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#findCycle-oracle.pgx.api.PgxGraph-)
  - [Analyst#findCycle(PgxGraph graph, VertexSequence<ID> nodeSeq, EdgeSequence edgeSeq)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#findCycle-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexSequence-oracle.pgx.api.EdgeSequence-)

This algorithm tries to find a cycle in a directed graph using DFS traversals and will return the first cycle found, if there is one. In such case, the vertices and edges involved in the cycle will be returned in the order of visit. The algorithm is expensive because it will perform DFS traversals using different vertices as starting points until it explores the whole graph (worst-case scenario), or until it finds a cycle.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

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
public class FindCycle {
  public boolean findCycle(PgxGraph g, @Out VertexSequence cycleNodes, @Out EdgeSequence cycleEdges) {
    VertexProperty<Boolean> visited = VertexProperty.create();
    VertexProperty<Boolean> inPath = VertexProperty.create();
    VertexProperty<PgxVertex> source = VertexProperty.create();
    VertexProperty<Long> deg = VertexProperty.create();

    visited.setAll(false);
    inPath.setAll(false);
    deg.setAll(PgxVertex::getOutDegree);

    Scalar<Boolean> foundCycle = Scalar.create(false);
    Scalar<PgxVertex> pivotNode = Scalar.create();

    g.getVertices().filter(s -> !visited.get(s) && !foundCycle.get()).forSequential(s ->
        inDFS(g, s)
            .navigator(v -> !foundCycle.get())
            .filter(v -> deg.get(v) > 0)
            .forward(v -> {
              //Adding potential vertices in the cycle path
              inPath.set(v, true);

              v.getOutNeighbors().forEach(w -> {
                source.set(w, v);
                if (inPath.get(w)) {
                  foundCycle.set(true);
                  pivotNode.set(w);
                }
              });
            })
            .backwardFilter(v -> !foundCycle.get())
            .backward(v -> {
              //Updating paths leading to dead-ends
              v.getInNeighbors().filter(inPath).forEach(w -> {
                if (deg.get(w) > 0) {
                  deg.set(w, deg.get(w) - 1);
                }
              });

              pivotNode.set(source.get(v));
              //Removing from the potential cycle path
              //a vertex that leads to a dead-end
              PgxVertex vertex = pivotNode.get();
              if (deg.get(vertex) == 0) {
                inPath.set(vertex, false);
                visited.set(vertex, true);
              }
            })
    );

    if (foundCycle.get()) {
      //Adding the vertices (in REVERSE order) in the cycle
      PgxVertex firstInCycle = pivotNode.get();
      cycleNodes.pushFront(firstInCycle);
      PgxVertex t = source.get(firstInCycle);
      pivotNode.set(t);

      Scalar<PgxEdge> e = Scalar.create();
      while (pivotNode.get() != firstInCycle) {
        cycleNodes.pushFront(pivotNode.get());

        PgxVertex pivotNodeGet = pivotNode.get();
        pivotNodeGet.getOutNeighbors().filter(inPath).forSequential(v ->
            e.set(v.edge())
        );

        cycleEdges.pushFront(e.get());

        PgxVertex vertex = pivotNode.get();
        PgxVertex sourceVertex = source.get(vertex);
        pivotNode.set(sourceVertex);
      }
      cycleNodes.pushFront(pivotNode.get());
      PgxVertex pivotNodeGet = pivotNode.get();
      pivotNodeGet.getOutNeighbors().filter(inPath).forSequential(v ->
          e.set(v.edge())
      );
      cycleEdges.pushFront(e.get());
    }

    return foundCycle.get();
  }
}
```
