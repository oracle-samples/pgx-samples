# Adamic-Adar index

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s2_adamic_adar_counting
- **Time Complexity:** O(E) with E = number of edges
- **Space Requirement:** O(E) with E = number of edges
- **Javadoc:**
  - [Analyst#adamicAdarCounting(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#adamicAdarCounting_oracle_pgx_api_PgxGraph_)
  - [Analyst#adamicAdarCounting(PgxGraph graph, EdgeProperty<java.lang.Double> aa)](https://docs.oracle.com/en/database/oracle/property-graph/24.4/spgjv/oracle/pgx/api/Analyst.html#adamicAdarCounting_oracle_pgx_api_PgxGraph_oracle_pgx_api_EdgeProperty_)

The Adamic-Adar index is meant for undirected graphs, since it is computed using the degree of the shared neighbors by two vertices in the graph. This implementation computes the index for every pair of vertices connected by an edge and associates it with that edge.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `adamic_adar` | edgeProp<double> | edge property holding the Adamic-Adar index of each edge in the graph. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2024 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.EdgeProperty;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxVertex;
import oracle.pgx.algorithm.annotations.Out;

import static java.lang.Math.log10;

@GraphAlgorithm
public class AdamicAdar {
  public void adamicAdar(PgxGraph g, @Out EdgeProperty<Double> aa) {
    g.getEdges().forEach(e -> {
      PgxVertex src = e.sourceVertex();
      PgxVertex dst = e.destinationVertex();

      double value = src.getOutNeighbors()
          .filter(n -> n.hasEdgeFrom(dst))
          .sum(n -> 1 / log10(n.getDegree()));

      aa.set(e, value);
    });
  }
}

```
