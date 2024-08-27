# Harmonic Centrality

- **Category:** ranking and walking
- **Algorithm ID:** pgx_builtin_k9_harmonic_centrality
- **Time Complexity:** O(V * (V + E)) with V = number of vertices, E = number of edgeswalk
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#harmonicCentrality(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#harmonicCentrality-oracle.pgx.api.PgxGraph-)  - [Analyst#harmonicCentrality(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#harmonicCentrality-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-)

Harmonic centrality computes the centrality of each vertex by taking the reciprocal of the sum of the shortest path distances from that vertex to all other vertices in the graph. This metric highlights the importance of vertices that efficiently connect disparate parts of the network

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `hc` | vertexProp | vertex property holding the harmonic centrality value for each vertex in the graph. |

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
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

import static oracle.pgx.algorithm.Traversal.currentLevel;
import static oracle.pgx.algorithm.Traversal.inBFS;

@GraphAlgorithm
public class HarmonicCentrality {
  public void harmonicCentrality(PgxGraph g, @Out VertexProperty<Double> harmonicCentrality) {
    g.getVertices().forEach(root -> {
      Scalar<Double> rootSum = Scalar.create(0.0);

      inBFS(g, root).filter(v -> v != root).forward(v ->
          rootSum.reduceAdd(1.0 / currentLevel())
      );

      harmonicCentrality.set(root, rootSum.get());
    });
  }
}
```
