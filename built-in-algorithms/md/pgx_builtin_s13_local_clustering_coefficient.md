# Local Clustering Coefficient (LCC)

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s13_local_clustering_coefficient
- **Time Complexity:** O(V ^ 2) with V = number of vertices
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#localClusteringCoefficient(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#localClusteringCoefficient_oracle_pgx_api_PgxGraph_)
  - [Analyst#localClusteringCoefficient(PgxGraph graph, VertexProperty<ID,java.lang.Double> lcc)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#localClusteringCoefficient_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_)

The LCC of a vertex V is the fraction of connections between each pair of neighbors of V, i.e. the fraction of existing triangles from all the possible triangles involving V and every other pair of neighbor vertices of V. This implementation is intended for undirected graphs. Nodes with a degree smaller than 2 will be assigned a LCC value of 0.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `lcc` | vertexProp<double> | vertex property holding the lcc value for each vertex. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.Scalar;
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class ClusteringCoefficient {
  public void localClusteringCoefficient(PgxGraph g, @Out VertexProperty<Double> lcc) {
    lcc.setAll(0d);

    g.getVertices().filter(v -> v.getOutDegree() >= 2).forEach(v -> {
      Scalar<Long> lcount = Scalar.create(0L);
      v.getOutNeighbors().forEach(m ->
          v.getOutNeighbors().filter(m::lessThan).forEach(n -> {
            if (m.hasEdgeTo(n)) {
              lcount.increment();
            }
          })
      );
      lcc.set(v, (2.0 * lcount.get()) / ((double) v.getOutDegree() * (v.getOutDegree() - 1)));
    });
  }
}
```
