# In-Degree Distribution

- **Category:** structure evaluation
- **Algorithm ID:** pgx_builtin_s7_in_degree_distribution
- **Time Complexity:** O(V) with V = number of vertices
- **Space Requirement:** O(V) with V = number of vertices
- **Javadoc:** 
  - [Analyst#inDegreeDistribution(PgxGraph graph)](https://docs.oracle.com/en/database/oracle/property-graph/24.3/spgjv/oracle/pgx/api/Analyst.html#inDegreeDistribution-oracle.pgx.api.PgxGraph-)
  - [Analyst#inDegreeDistribution(PgxGraph graph, PgxMap<java.lang.Integer,java.lang.Long> distribution)](https://docs.oracle.com/en/database/oracle/property-graph/22/.3spgjv/oracle/pgx/api/Analyst.html#inDegreeDistribution-oracle.pgx.api.PgxGraph-oracle.pgx.api.PgxMap-)

This version of the degree distribution will return a map with the distribution of the in-degree (i.e. just incoming edges) of the graph. For undirected graphs the algorithm will consider all the edges (incoming and outgoing) for the distribution.


## Signature

| Input Argument | Type | Comment |
| :--- | :--- | :--- |
| `G` | graph | the graph. |

| Output Argument | Type | Comment |
| :--- | :--- | :--- |
| `distribution` | map<int, long> | map holding a histogram of the vertex degrees in the graph. |

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
import oracle.pgx.algorithm.PgxMap;
import oracle.pgx.algorithm.annotations.GraphAlgorithm;
import oracle.pgx.algorithm.annotations.Out;

@GraphAlgorithm
public class IndegreeDistribution {
  public void indegreeDistribution(PgxGraph g, @Out PgxMap<Long, Long> distribution) {
    g.getVertices().forSequential(n -> {
      long degree = n.getInDegree();

      distribution.increment(degree);
    });
  }
}
```
