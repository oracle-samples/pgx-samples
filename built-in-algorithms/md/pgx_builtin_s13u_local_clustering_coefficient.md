# Local Clustering Coefficient for ignoring edge directions (LCC)

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s13u_local_clustering_coefficient
- **Time Complexity:** O(V ^ 2) with V = number of vertices
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#localClusteringCoefficient(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#localClusteringCoefficient_oracle_pgx_api_PgxGraph_)
  - [Analyst#localClusteringCoefficient(PgxGraph graph, boolean ignoreEdgeDirection)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#localClusteringCoefficient_oracle_pgx_api_PgxGraph_boolean_)
  - [Analyst#localClusteringCoefficient(PgxGraph graph, VertexProperty<ID,​java.lang.Double> lcc)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#localClusteringCoefficient_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_)
  - [Analyst#localClusteringCoefficient(PgxGraph graph, VertexProperty<ID,​java.lang.Double> lcc, boolean ignoreEdgeDirection)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#localClusteringCoefficient_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_boolean_)

The LCC of a vertex V is the fraction of connections between each pair of neighbors of V, i.e. the fraction of existing triangles from all the possible triangles involving V and every other pair of neighbor vertices of V. This implementation is intended for directed graphs and interprets them as undirected. Nodes with a degree smaller than 2 will be assigned a LCC value of 0.

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
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class ClusteringCoefficientUndirected {
  public void localClusteringCoefficient(PgxGraph g, @Out VertexProperty<Double> lcc) {
    lcc.setAll(0d);

    VertexProperty<Long> degree = VertexProperty.create();

    g.getVertices().forEach(v -> {
      degree.set(v, v.getOutDegree() + v.getInDegree() - v.getOutNeighbors().filter(v1 -> (v.hasEdgeFrom(v1))).size());
    });

    g.getVertices().filter(v -> degree.get(v) >= 2).forEach(v -> {
      long count = 0;
      v.getOutNeighbors().forEach(m -> {
        count += v.getOutNeighbors()
            .filter(n -> m.lessThan(n) && (m.hasEdgeTo(n) || n.hasEdgeTo(m)))
            .size();
        count += v.getInNeighbors()
            .filter(n -> !v.hasEdgeTo(n) && m.lessThan(n) && (m.hasEdgeTo(n) || n.hasEdgeTo(m)))
            .size();
      });
      v.getInNeighbors().filter(m -> !v.hasEdgeTo(m)).forEach(m -> {
        count += v.getOutNeighbors()
            .filter(n -> m.lessThan(n) && (m.hasEdgeTo(n) || n.hasEdgeTo(m)))
            .size();
        count += v.getInNeighbors()
            .filter(n -> !v.hasEdgeTo(n) && m.lessThan(n) && (m.hasEdgeTo(n) || n.hasEdgeTo(m)))
            .size();
      });
      lcc.set(v, (2.0 * count) / ((double) degree.get(v) * (degree.get(v) - 1)));
    });
  }
}
```
