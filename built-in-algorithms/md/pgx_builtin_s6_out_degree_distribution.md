# Out-Degree Distribution

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s6_out_degree_distribution
- **Time Complexity:** O(V) with V = number of vertices
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:**
  - [Analyst#outDegreeCentrality(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#outDegreeCentrality_oracle_pgx_api_PgxGraph_)
  - [Analyst#outDegreeCentrality(PgxGraph graph, VertexProperty<ID,java.lang.Integer> dc)](https://docs.oracle.com/en/database/oracle/property-graph/25.1/spgjv/oracle/pgx/api/Analyst.html#outDegreeCentrality_oracle_pgx_api_PgxGraph_oracle_pgx_api_VertexProperty_)

This version of the degree distribution will return a map with the distribution of the out-degree (i.e. just outgoing edges) of the graph. For undirected graphs the algorithm will consider all the edges (incoming and outgoing) for the distribution.

## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `distribution` | map<int, long> | map holding a histogram of the node degrees in the graph. |

| Return Value | Type | Comment |
| :--- | :--- | :--- |
| | void | None |

## Code

```java
/*
 * Copyright (C) 2013 - 2025 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgx.algorithms;

import oracle.pgx.algorithm.PgxGraph;
import oracle.pgx.algorithm.PgxMap;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class OutdegreeDistribution {
  public void outdegreeDistribution(PgxGraph g, @Out PgxMap<Long, Long> distribution) {
    g.getVertices().forSequential(n -> {
      long degree = n.getOutDegree();

      distribution.increment(degree);
    });
  }
}
```
