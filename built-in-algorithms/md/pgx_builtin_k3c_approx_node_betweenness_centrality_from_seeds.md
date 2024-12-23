# Approximate Vertex Betweenness Centrality From seeds

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k3c_approx_node_betweenness_centrality_from_seeds
- **Time Complexity:** O(V * E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(3 * V) with V = number of vertices
- **Javadoc:**
  - [Analyst#approximateVertexBetweennessCentralityFromSeeds(PgxGraph graph, PgxVertex<ID>... seeds)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#approximateVertexBetweennessCentralityFromSeeds_oracle_pgx_api_PgxGraph_oracle_pgx_api_PgxVertex____)
  - [Analyst#approximateVertexBetweennessCentralityFromSeeds(PgxGraph graph, VertexProperty<ID,java.lang.Double> bc, PgxVertex<ID>... seeds)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#approximateVertexBetweennessCentralityFromSeeds_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_oracle_pgx_api_PgxVertex____)

This variant of betweenness centrality approximates the centrality of the vertices by just using the vertices from the given sequence as starting points for the BFS traversals of the graph, instead of computing the exact value by using all the vertices in the graph.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `seeds` | nodeSeq | the (unique) chosen vertices to be used to compute the approximated betweenness centrality coefficients. |

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
import oracle.pgx.algorithm.VertexSequence;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.inBFS;

@GraphAlgorithm
public class BetweennessCentralityApproximateSeeds {
  public void betweennessCentralityApproximate(PgxGraph g, VertexSequence seeds, @Out VertexProperty<Double> bc) {
    bc.setAll(0d); // Initialize

    VertexSet set = VertexSet.create();
    seeds.forSequential(set::add);

    set.forSequential(s -> {
      // temporary values per vertex
      VertexProperty<Double> sigma = VertexProperty.create();
      VertexProperty<Double> delta = VertexProperty.create();
      sigma.setAll(0d);
      sigma.set(s, 1d);

      // BFS order iteration from s
      inBFS(g, s) //
          .filter(v -> v != s) //
          .forward(v -> sigma.set(v, v.getUpNeighbors().sum(sigma))) //
          .backwardFilter(v -> v != s) //
          .backward(v -> {
            delta.set(v, v.getDownNeighbors().sum(w -> sigma.get(v) / sigma.get(w) * (1 + delta.get(w))));
            bc.reduceAdd(v, delta.get(v));
          });
    });
  }
}
```
