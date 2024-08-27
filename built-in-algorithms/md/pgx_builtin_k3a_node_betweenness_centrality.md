# Vertex Betweenness Centrality

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k3a_node_betweenness_centrality
- **Time Complexity:** O(V * E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(3 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#vertexBetweennessCentrality(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#vertexBetweennessCentrality-oracle.pgx.api.PgxGraph-)
  - [Analyst#vertexBetweennessCentrality(PgxGraph graph, VertexProperty<ID,java.lang.Double> bc)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#vertexBetweennessCentrality-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-)

The Betweenness Centrality of a vertex V in a graph is the sum of the fraction of shortest paths that pass through V from all the possible shortest paths connecting every possible pair of vertices S, T in the graph, such that V is different from S and T. Because of its definition, the algorithm is meant for connected graphs.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `bc` | vertexProp<double> | vertex property holding the betweenness centrality value for each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.inBFS;

@GraphAlgorithm
public class BetweennessCentrality {
  public void bcFull(PgxGraph g, @Out VertexProperty<Double> bc) {
    bc.setAll(0d); // Initialize

    g.getVertices().forEach(s -> {
      // temporary values per vertex
      VertexProperty<Double> sigma = VertexProperty.create();
      VertexProperty<Double> delta = VertexProperty.create();
      sigma.setAll(0d);
      sigma.set(s, 1d);

      // BFS order iteration from s
      inBFS(g, s)
          .filter(v -> v != s)
          .forward(v -> sigma.set(v, v.getUpNeighbors().sum(sigma)))
          .backwardFilter(v -> v != s)
          .backward(v -> {
            delta.set(v, v.getDownNeighbors().sum(w -> (1 + delta.get(w)) / sigma.get(w)) * sigma.get(v));
            bc.reduceAdd(v, delta.get(v));
          });
    });
  }
}
```
