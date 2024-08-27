# Local Clustering Coefficient for Directed Graphs (LCC)

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s13d_local_clustering_coefficient
- **Time Complexity:** O(V ^ 2) with V = number of vertices
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#localClusteringCoefficient(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#localClusteringCoefficient-oracle.pgx.api.PgxGraph-)
  - [Analyst#localClusteringCoefficient(PgxGraph graph, boolean ignoreEdgeDirection)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#localClusteringCoefficient-oracle.pgx.api.PgxGraph-boolean-)
  - [Analyst#localClusteringCoefficient(PgxGraph graph, VertexProperty lcc)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#localClusteringCoefficient-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-)
  - [Analyst#localClusteringCoefficient(PgxGraph graph, VertexProperty lcc, boolean ignoreEdgeDirection)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#localClusteringCoefficient-oracle.pgx.api.PgxGraph-oracle.pgx.api.VertexProperty-boolean-)

The LCC of a vertex V is the fraction of connections between each pair of neighbors of V, i.e. the fraction of existing triangles from all the possible triangles involving V and every other pair of neighbor vertices of V. This implementation is intended for directed graphs. Nodes with a degree smaller than 2 will be assigned a LCC value of 0.

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
import oracle.pgx.algorithm.VertexProperty;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class ClusteringCoefficientDirected {
  public void localClusteringCoefficient(PgxGraph g, @Out VertexProperty<Double> lcc) {
    lcc.setAll(0d);

    g.getVertices().filter(v -> v.getOutDegree() >= 2).forEach(v -> {
      double count = 0;
      v.getOutNeighbors().forEach(m -> {
        v.getOutNeighbors().filter(n -> (m != n)).forEach(n -> {
          if (m.hasEdgeTo(n)) {
            count++;
          }
        });
      });
      lcc.set(v, count / ((double) v.getOutDegree() * (v.getOutDegree() - 1)));

    });
  }
}
```
