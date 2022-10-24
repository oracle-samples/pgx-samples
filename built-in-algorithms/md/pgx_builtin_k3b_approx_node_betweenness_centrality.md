# Approximate Vertex Betweenness Centrality with Random Seeds

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k3b_approx_node_betweenness_centrality
- **Time Complexity:** O(V * E) with V = number of vertices, E = number of edges
- **Space Requirement:** O(3 * V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#approximateVertexBetweennessCentrality(PgxGraph graph, int k)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#approximateVertexBetweennessCentrality-oracle.pgx.api.PgxGraph-int-)
  - [Analyst#approximateVertexBetweennessCentrality(PgxGraph graph, int k, VertexProperty<ID,java.lang.Double> bc)](https://docs.oracle.com/en/database/oracle/property-graph/22.4/spgjv/oracle/pgx/api/Analyst.html#approximateVertexBetweennessCentrality-oracle.pgx.api.PgxGraph-int-oracle.pgx.api.VertexProperty-)

This variant of betweenness centrality approximates the centrality of the vertices by just using k random vertices as starting points for the BFS traversals of the graph, instead of computing the exact value by using all the vertices in the graph.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |
| `num_seeds` | int | number of random vertices to be used to compute the approximated betweenness centrality coefficients. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `bc` | vertexProp<double> | vertex property holding the betweenness centrality value for each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2022 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.VertexSet;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.inBFS;

@GraphAlgorithm
public class BetweennessCentralityApproximate {
  public void betweennessCentralityApproximate(PgxGraph g, int numSeeds, @Out VertexProperty<Double> bc) {
    bc.setAll(0d); // Initialize

    long maxSeeds = (g.getNumVertices() > numSeeds) ? numSeeds : g.getNumVertices();

    VertexSet seeds = VertexSet.create();
    while (seeds.size() < maxSeeds) {
      seeds.add(g.getRandomVertex());
    }

    seeds.forEach(s -> {
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
            delta.set(v, v.getDownNeighbors().sum(w -> sigma.get(v) / sigma.get(w) * (1 + delta.get(w))));
            bc.reduceAdd(v, delta.get(v));
          });
    });
  }
}
```
